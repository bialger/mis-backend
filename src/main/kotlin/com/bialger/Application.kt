package com.bialger

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(
        title = "MIS CRM API",
        version = "0.1",
        description = "REST API for an outpatient MIS (multi-branch clinics, ambulatory patients). " +
            "Tags follow domain modules. Paginated collections use query parameters page and size; " +
            "responses may include a Link header (RFC 5988). " +
            "/api errors return JSON (validation_failed, bad_request)."
    )
)
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Micronaut.run(Application::class.java, *args)
        }
    }
}
