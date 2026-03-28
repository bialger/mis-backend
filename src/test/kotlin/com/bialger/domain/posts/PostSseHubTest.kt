package com.bialger.domain.posts

import com.bialger.web.DomainEventSseHub
import io.kotest.core.spec.style.StringSpec
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class PostSseHubTest : StringSpec({

    "emit sends one SSE event to subscribers" {
        val hub = DomainEventSseHub()
        StepVerifier.create(Flux.from(hub.stream("posts")))
            .then { hub.emitJson("posts", """{"type":"CREATED","id":1}""") }
            .expectNextCount(1)
            .thenCancel()
            .verify()
    }
})
