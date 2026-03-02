package com.bialger

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/")
class HelloController {
    @Get
    fun index(): String = "MIS web server is running"
}
