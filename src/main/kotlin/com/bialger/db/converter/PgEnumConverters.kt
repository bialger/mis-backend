package com.bialger.db.converter

import com.bialger.db.enums.*
import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.model.runtime.convert.AttributeConverter
import jakarta.inject.Singleton
import org.postgresql.util.PGobject

@Singleton
class IntegrationTypeConverter : AttributeConverter<IntegrationType, Any> {
    override fun convertToPersistedValue(entityValue: IntegrationType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "integration_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): IntegrationType? =
        fromPgObject(persistedValue)?.let { IntegrationType.valueOf(it) }
}

@Singleton
class NotificationChannelConverter : AttributeConverter<NotificationChannel, Any> {
    override fun convertToPersistedValue(entityValue: NotificationChannel?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "notification_channel")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): NotificationChannel? =
        fromPgObject(persistedValue)?.let { NotificationChannel.valueOf(it) }
}

@Singleton
class NotificationTypeConverter : AttributeConverter<NotificationType, Any> {
    override fun convertToPersistedValue(entityValue: NotificationType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "notification_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): NotificationType? =
        fromPgObject(persistedValue)?.let { NotificationType.valueOf(it) }
}

@Singleton
class NotificationStatusConverter : AttributeConverter<NotificationStatus, Any> {
    override fun convertToPersistedValue(entityValue: NotificationStatus?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "notification_status")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): NotificationStatus? =
        fromPgObject(persistedValue)?.let { NotificationStatus.valueOf(it) }
}

@Singleton
class GenderTypeConverter : AttributeConverter<GenderType?, Any> {
    override fun convertToPersistedValue(entityValue: GenderType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "gender_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): GenderType? =
        fromPgObject(persistedValue)?.let { GenderType.valueOf(it) }
}

@Singleton
class LocalityTypeConverter : AttributeConverter<LocalityType?, Any> {
    override fun convertToPersistedValue(entityValue: LocalityType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "locality_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): LocalityType? =
        fromPgObject(persistedValue)?.let { LocalityType.valueOf(it) }
}

private fun toPgObject(value: String?, pgType: String): Any? {
    if (value == null) return null
    return PGobject().apply {
        setType(pgType)
        setValue(value)
    }
}

private fun fromPgObject(persistedValue: Any?): String? = when (persistedValue) {
    is PGobject -> persistedValue.value
    is String -> persistedValue
    null -> null
    else -> persistedValue.toString()
}
