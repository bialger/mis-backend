package com.bialger.db

import com.bialger.db.entity.NotificationEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.PatientEntity
import com.bialger.db.enums.NotificationChannel
import com.bialger.db.enums.NotificationStatus
import com.bialger.db.enums.NotificationType
import com.bialger.db.repository.NotificationRepository
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.PatientRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class NotificationRepositoryTest(
    private val notificationRepository: NotificationRepository,
    private val patientRepository: PatientRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    "save and findById" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг", codeOkpo = "123", address = "Addr"))
        val patientId = UUID.randomUUID()
        patientRepository.save(
            PatientEntity(
                id = patientId,
                cardNumber = "N001",
                organizationId = orgId,
                fullName = "Пациент"
            )
        )

        val entity = NotificationEntity(
            id = UUID.randomUUID(),
            patientId = patientId,
            channel = NotificationChannel.SMS,
            type = NotificationType.APPOINTMENT_CONFIRMATION,
            status = NotificationStatus.PENDING,
            content = "Напоминание о визите"
        )
        notificationRepository.save(entity)

        val found = notificationRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.channel shouldBe NotificationChannel.SMS
        found.status shouldBe NotificationStatus.PENDING
    }

    "findByPatientId" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг2", codeOkpo = "456", address = "A"))
        val patientId = UUID.randomUUID()
        patientRepository.save(
            PatientEntity(id = patientId, cardNumber = "N002", organizationId = orgId, fullName = "Пациент2")
        )

        notificationRepository.save(
            NotificationEntity(
                id = UUID.randomUUID(),
                patientId = patientId,
                channel = NotificationChannel.EMAIL,
                type = NotificationType.VISIT_REMINDER,
                status = NotificationStatus.SENT
            )
        )
        notificationRepository.save(
            NotificationEntity(
                id = UUID.randomUUID(),
                patientId = patientId,
                channel = NotificationChannel.SMS,
                type = NotificationType.MARKETING,
                status = NotificationStatus.PENDING
            )
        )

        val notifications = notificationRepository.findByPatientId(patientId)
        notifications shouldHaveSize 2
    }

    "findByStatus" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг3", codeOkpo = "789", address = "B"))
        val patientId = UUID.randomUUID()
        patientRepository.save(
            PatientEntity(id = patientId, cardNumber = "N003", organizationId = orgId, fullName = "Пациент3")
        )

        notificationRepository.save(
            NotificationEntity(
                id = UUID.randomUUID(),
                patientId = patientId,
                channel = NotificationChannel.SMS,
                type = NotificationType.APPOINTMENT_CONFIRMATION,
                status = NotificationStatus.FAILED
            )
        )

        val failed = notificationRepository.findByStatus(NotificationStatus.FAILED)
        failed shouldHaveSize 1
        failed.first().status shouldBe NotificationStatus.FAILED
    }
})
