package com.bialger.domain.mvc

import com.bialger.domain.core.repository.OrganizationRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.core.spec.style.StringSpec
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
class MvcOrganizationCrudFlowTest(
    @param:Client("/") private val client: HttpClient,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    "create organization via form post then delete" {
        val name = "MVC-Org-${UUID.randomUUID().toString().take(8)}"
        val request = HttpRequest.POST(
            "/mvc/organizations",
            "name=$name&codeOkpo=&codeOkud=&address="
        ).contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)

        val response = client.toBlocking().exchange(request, String::class.java)
        response.status shouldBe HttpStatus.SEE_OTHER
        val location = response.header("Location").shouldNotBeNull()
        val id = UUID.fromString(location.substringAfterLast('/'))

        organizationRepository.findById(id).orElse(null).shouldNotBeNull().name shouldBe name

        client.toBlocking().exchange(io.micronaut.http.HttpRequest.DELETE<Any>("/mvc/organizations/$id"), String::class.java)
        organizationRepository.findById(id).isPresent shouldBe false
    }

    "organization add form contains fields" {
        val html = client.toBlocking().retrieve("/mvc/organizations/add")
        html shouldContain "Организация"
        html shouldContain "name=\"name\""
    }
})
