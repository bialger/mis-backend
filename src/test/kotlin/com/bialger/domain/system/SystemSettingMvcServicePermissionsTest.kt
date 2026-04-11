package com.bialger.domain.system

import com.bialger.domain.system.mvc.SystemSettingMvcService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest(transactional = true)
class SystemSettingMvcServicePermissionsTest(
    private val service: SystemSettingMvcService
) : StringSpec({

    "SYSADMIN gets no finance or inventory access" {
        val perms = service.resolvePermissions("SYSADMIN")
        perms["canViewFinance"] shouldBe false
        perms["canEditFinance"] shouldBe false
        perms["canViewInventory"] shouldBe false
        perms["canWriteInventory"] shouldBe false
        perms["canManualEgiszSend"] shouldBe false
        perms["canEditBackdateDays"] shouldBe 0
    }

    "DOCTOR gets no finance access, no inventory write" {
        val perms = service.resolvePermissions("DOCTOR")
        perms["canViewFinance"] shouldBe false
        perms["canEditFinance"] shouldBe false
        perms["canViewInventory"] shouldBe false
        perms["canWriteInventory"] shouldBe false
        perms["canManualEgiszSend"] shouldBe false
        perms["canEditBackdateDays"] shouldBe 60
    }

    "NURSE gets limited access (no finance, no inventory write)" {
        val perms = service.resolvePermissions("NURSE")
        perms["canViewFinance"] shouldBe false
        perms["canEditFinance"] shouldBe false
        perms["canViewInventory"] shouldBe true
        perms["canWriteInventory"] shouldBe false
        perms["canManualEgiszSend"] shouldBe false
    }

    "HEAD gets full access including EGISZ send" {
        val perms = service.resolvePermissions("HEAD")
        perms["canViewFinance"] shouldBe true
        perms["canEditFinance"] shouldBe true
        perms["canViewInventory"] shouldBe true
        perms["canWriteInventory"] shouldBe true
        perms["canManualEgiszSend"] shouldBe true
        (perms["canEditBackdateDays"] as Int) shouldBe 3650
    }

    "ADMIN gets finance and inventory access, no EGISZ" {
        val perms = service.resolvePermissions("ADMIN")
        perms["canViewFinance"] shouldBe true
        perms["canEditFinance"] shouldBe true
        perms["canViewInventory"] shouldBe true
        perms["canWriteInventory"] shouldBe true
        perms["canManualEgiszSend"] shouldBe false
    }

    "default (no roleCode) behaves like ADMIN" {
        val permsDefault = service.resolvePermissions()
        val permsAdmin = service.resolvePermissions("ADMIN")
        permsDefault shouldBe permsAdmin
    }
})
