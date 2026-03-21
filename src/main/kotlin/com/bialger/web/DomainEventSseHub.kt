package com.bialger.web

import io.micronaut.http.sse.Event
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-topic SSE streams for MVC domain collections (one topic per bounded context resource).
 */
@Singleton
class DomainEventSseHub {

    private val log = LoggerFactory.getLogger(DomainEventSseHub::class.java)

    private val sinks = ConcurrentHashMap<String, Sinks.Many<String>>()

    /** Один общий Flux на топик: иначе каждый GET /events создавал новый asFlux(), а повторная подписка на Publisher давала дубли событий (особенно при PATCH). */
    private val sharedEventStreams = ConcurrentHashMap<String, Flux<Event<String>>>()

    private fun sinkFor(topic: String): Sinks.Many<String> =
        sinks.computeIfAbsent(topic) {
            Sinks.many().multicast().onBackpressureBuffer()
        }

    fun stream(topic: String): Publisher<Event<String>> =
        sharedEventStreams.computeIfAbsent(topic) {
            sinkFor(topic).asFlux()
                .distinctUntilChanged()
                .map { payload -> Event.of(payload).name("$topic-change") }
                .share()
        }

    fun emitJson(topic: String, json: String) {
        val r = sinkFor(topic).tryEmitNext(json)
        if (r.isFailure) {
            log.warn("SSE emit failed for topic {}: {} (no active /events listeners yet?)", topic, r)
        }
    }
}
