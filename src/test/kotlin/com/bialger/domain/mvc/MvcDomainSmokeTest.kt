package com.bialger.domain.mvc

import io.kotest.matchers.string.shouldContain
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.core.spec.style.StringSpec

@MicronautTest
class MvcDomainSmokeTest(
    @param:Client("/") private val client: HttpClient
) : StringSpec({

    "mvc index lists domain links" {
        val html = client.toBlocking().retrieve("/mvc")
        html shouldContain "MVC"
        html shouldContain "/mvc/organizations"
        html shouldContain "/mvc/branches"
        html shouldContain "/mvc/rooms"
        html shouldContain "/mvc/roles"
        html shouldContain "/mvc/permissions"
        html shouldContain "/mvc/employees"
        html shouldContain "/mvc/specialties"
        html shouldContain "/mvc/patient-tag-types"
        html shouldContain "/mvc/lab-tests"
        html shouldContain "/mvc/inventory-items"
        html shouldContain "/mvc/time-slots"
        html shouldContain "/mvc/patients"
        html shouldContain "/mvc/appointments"
        html shouldContain "/mvc/payments"
    }

    "mvc specialties list returns html" {
        val html = client.toBlocking().retrieve("/mvc/specialties")
        html shouldContain "Специализации"
    }

    "mvc organizations list returns html" {
        val html = client.toBlocking().retrieve("/mvc/organizations")
        html shouldContain "Организации"
    }

    "mvc branches list returns html" {
        val html = client.toBlocking().retrieve("/mvc/branches")
        html shouldContain "Филиалы"
    }

    "mvc rooms list returns html" {
        val html = client.toBlocking().retrieve("/mvc/rooms")
        html shouldContain "Кабинеты"
    }

    "mvc roles list returns html" {
        val html = client.toBlocking().retrieve("/mvc/roles")
        html shouldContain "Роли"
    }

    "mvc permissions list returns html" {
        val html = client.toBlocking().retrieve("/mvc/permissions")
        html shouldContain "Права доступа"
    }

    "mvc employees list returns html" {
        val html = client.toBlocking().retrieve("/mvc/employees")
        html shouldContain "Сотрудники"
    }

    "mvc lab tests list returns html" {
        val html = client.toBlocking().retrieve("/mvc/lab-tests")
        html shouldContain "Лабораторные тесты"
    }

    "mvc inventory items list returns html" {
        val html = client.toBlocking().retrieve("/mvc/inventory-items")
        html shouldContain "Позиции склада"
    }

    "mvc time slots list returns html" {
        val html = client.toBlocking().retrieve("/mvc/time-slots")
        html shouldContain "Слоты расписания"
    }

    "mvc patients list returns html" {
        val html = client.toBlocking().retrieve("/mvc/patients")
        html shouldContain "Пациенты"
    }

    "mvc appointments list returns html" {
        val html = client.toBlocking().retrieve("/mvc/appointments")
        html shouldContain "Приёмы"
    }

    "mvc payments list returns html" {
        val html = client.toBlocking().retrieve("/mvc/payments")
        html shouldContain "Платежи"
    }
})
