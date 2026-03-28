package com.bialger.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

/**
 * Micronaut Serde does not register a [ObjectMapper] bean; MVC/SSE code needs one for JSON payloads.
 */
@Factory
class ObjectMapperFactory {

    @Singleton
    fun objectMapper(): ObjectMapper = ObjectMapper().findAndRegisterModules()
}
