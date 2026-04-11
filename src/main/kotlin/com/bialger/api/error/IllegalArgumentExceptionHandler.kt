package com.bialger.api.error

import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Requires(classes = [ExceptionHandler::class])
class IllegalArgumentExceptionHandler :
    ExceptionHandler<IllegalArgumentException, HttpResponse<ApiErrorResponse>> {

    override fun handle(
        request: HttpRequest<*>,
        exception: IllegalArgumentException
    ): HttpResponse<ApiErrorResponse> {
        if (!request.isApiRequest()) {
            throw exception
        }
        return HttpResponse.badRequest(
            ApiErrorResponse(error = "bad_request", message = exception.message)
        )
    }
}
