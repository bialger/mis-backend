package com.bialger.api.error

import io.micronaut.context.annotation.Requires
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Requires(classes = [ExceptionHandler::class])
class DataAccessExceptionHandler :
    ExceptionHandler<DataAccessException, HttpResponse<ApiErrorResponse>> {

    override fun handle(
        request: HttpRequest<*>,
        exception: DataAccessException
    ): HttpResponse<ApiErrorResponse> {
        if (!request.isApiRequest()) {
            throw exception
        }
        return if (isFkConflict(exception)) {
            HttpResponse.status<ApiErrorResponse>(HttpStatus.CONFLICT).body(
                ApiErrorResponse(
                    error = "conflict",
                    message = "Нельзя удалить запись: есть связанные данные"
                )
            )
        } else {
            HttpResponse.serverError(
                ApiErrorResponse(
                    error = "server_error",
                    message = "Database operation failed"
                )
            )
        }
    }

    private fun isFkConflict(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            val msg = current.message?.lowercase().orEmpty()
            if (
                msg.contains("foreign key constraint") ||
                msg.contains("violates foreign key constraint") ||
                msg.contains("is still referenced from table")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

