package com.bialger.domain.patient.enums

enum class ConsentType {
    /** Voluntary informed consent for medical intervention (FZ-323). */
    MEDICAL_INTERVENTION,
    /** Consent to personal data processing (FZ-152). */
    PERSONAL_DATA_PROCESSING,
    /** Transfer of data to government systems (EGISZ, etc.). */
    GOV_DATA_TRANSFER,
    /** Consent to marketing communications. */
    MARKETING
}
