package com.bialger.api

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Hidden
import java.net.URI

@Hidden
@Controller
class SwaggerUiRedirectController {

    @Get("/swagger-ui")
    fun redirect(): HttpResponse<Any> = HttpResponse.redirect(URI.create("/swagger-ui/index.html"))
}
