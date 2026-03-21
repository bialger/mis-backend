package com.bialger.web

import io.micronaut.http.sse.Event
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-topic SSE streams for MVC domain collections (one topic per bounded context resource).
 */
@Singleton
class DomainEventSseHub {

    private val sinks = ConcurrentHashMap<String, Sinks.Many<String>>()

    private fun sinkFor(topic: String): Sinks.Many<String> =
        sinks.computeIfAbsent(topic) {
            Sinks.many().multicast().onBackpressureBuffer()
        }

    fun stream(topic: String): Publisher<Event<String>> =
        sinkFor(topic).asFlux().map { payload -> Event.of(payload).name("$topic-change") }

    fun emitJson(topic: String, json: String) {
        sinkFor(topic).tryEmitNext(json)
    }
}
