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

@Singleton
class TemplateTypeConverter : AttributeConverter<TemplateType, Any> {
    override fun convertToPersistedValue(entityValue: TemplateType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "template_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): TemplateType? =
        fromPgObject(persistedValue)?.let { TemplateType.valueOf(it) }
}

@Singleton
class ConsentTypeConverter : AttributeConverter<ConsentType, Any> {
    override fun convertToPersistedValue(entityValue: ConsentType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "consent_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): ConsentType? =
        fromPgObject(persistedValue)?.let { ConsentType.valueOf(it) }
}

@Singleton
class PaymentMethodTypeConverter : AttributeConverter<PaymentMethodType, Any> {
    override fun convertToPersistedValue(entityValue: PaymentMethodType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "payment_method_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): PaymentMethodType? =
        fromPgObject(persistedValue)?.let { PaymentMethodType.valueOf(it) }
}

@Singleton
class PaymentStatusTypeConverter : AttributeConverter<PaymentStatusType, Any> {
    override fun convertToPersistedValue(entityValue: PaymentStatusType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "payment_status_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): PaymentStatusType? =
        fromPgObject(persistedValue)?.let { PaymentStatusType.valueOf(it) }
}

@Singleton
class LabOrderStatusConverter : AttributeConverter<LabOrderStatus, Any> {
    override fun convertToPersistedValue(entityValue: LabOrderStatus?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "lab_order_status")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): LabOrderStatus? =
        fromPgObject(persistedValue)?.let { LabOrderStatus.valueOf(it) }
}

@Singleton
class AppointmentStatusConverter : AttributeConverter<AppointmentStatus, Any> {
    override fun convertToPersistedValue(entityValue: AppointmentStatus?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "appointment_status")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): AppointmentStatus? =
        fromPgObject(persistedValue)?.let { AppointmentStatus.valueOf(it) }
}

@Singleton
class AppointmentSourceConverter : AttributeConverter<AppointmentSource, Any> {
    override fun convertToPersistedValue(entityValue: AppointmentSource?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "appointment_source")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): AppointmentSource? =
        fromPgObject(persistedValue)?.let { AppointmentSource.valueOf(it) }
}

@Singleton
class FileTypeConverter : AttributeConverter<FileType, Any> {
    override fun convertToPersistedValue(entityValue: FileType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "file_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): FileType? =
        fromPgObject(persistedValue)?.let { FileType.valueOf(it) }
}

@Singleton
class PrescriptionTypeConverter : AttributeConverter<PrescriptionType, Any> {
    override fun convertToPersistedValue(entityValue: PrescriptionType?, context: ConversionContext): Any? =
        toPgObject(entityValue?.name, "prescription_type")

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): PrescriptionType? =
        fromPgObject(persistedValue)?.let { PrescriptionType.valueOf(it) }
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
