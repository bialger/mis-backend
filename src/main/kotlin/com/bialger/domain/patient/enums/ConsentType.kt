package com.bialger.domain.patient.enums

enum class ConsentType {
    /** Добровольное информированное согласие на медицинское вмешательство (ФЗ-323) */
    MEDICAL_INTERVENTION,
    /** Согласие на обработку персональных данных (ФЗ-152) */
    PERSONAL_DATA_PROCESSING,
    /** Передача данных в государственные органы (ЕГИСЗ и пр.) */
    GOV_DATA_TRANSFER,
    /** Согласие на маркетинговые коммуникации */
    MARKETING
}
