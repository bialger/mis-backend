package com.bialger.domain.attachment.entity

import com.bialger.db.converter.NotificationChannelConverter
import com.bialger.db.converter.NotificationStatusConverter
import com.bialger.db.converter.NotificationTypeConverter
import com.bialger.domain.attachment.enums.NotificationChannel
import com.bialger.domain.attachment.enums.NotificationStatus
import com.bialger.domain.attachment.enums.NotificationType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("notification")
data class NotificationEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val appointmentId: UUID? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = NotificationChannelConverter::class)
    val channel: NotificationChannel,
    @field:TypeDef(type = DataType.OBJECT, converter = NotificationTypeConverter::class)
    val type: NotificationType,
    val content: String? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = NotificationStatusConverter::class)
    val status: NotificationStatus,
    val sentAt: Instant? = null,
    val createdAt: Instant? = null
)
