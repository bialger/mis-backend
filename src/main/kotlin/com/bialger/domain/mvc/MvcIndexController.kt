package com.bialger.domain.mvc

import com.bialger.web.AppPageModelFactory
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.ModelAndView
import io.swagger.v3.oas.annotations.Hidden

@Hidden
@Controller("/mvc")
class MvcIndexController(
    private val appPageModelFactory: AppPageModelFactory
) {

    @Get
    fun index(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "MVC — домены",
            activePage = "mvc",
            contentView = "pages/content/mvc/index"
        )
}
