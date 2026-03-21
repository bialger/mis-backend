package com.bialger.web

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class DomainMvcEventEmitter(
    private val domainEventSseHub: DomainEventSseHub,
    private val objectMapper: ObjectMapper
) {
    fun notify(topic: String, mutation: String, id: String, title: String) {
        val json = objectMapper.writeValueAsString(
            mapOf(
                "type" to mutation,
                "id" to id,
                "title" to title
            )
        )
        domainEventSseHub.emitJson(topic, json)
    }
}
