package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Introspected
@Schema(description = "Update medical record (clinical visit documentation)")
data class MedicalRecordUpdateDto(
    val complaints: String? = null,
    val anamnesis: String? = null,
    val examinationResults: String? = null,
    val diseaseCourse: String? = null,
    val procedures: String? = null,
    val epicrisis: String? = null
)
