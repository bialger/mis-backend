package com.bialger

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(title = "MIS CRM API", version = "0.1", description = "REST API для медицинской CRM")
)
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Micronaut.run(Application::class.java, *args)
        }
    }
}
