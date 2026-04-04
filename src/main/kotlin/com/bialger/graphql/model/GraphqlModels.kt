package com.bialger.graphql.model

import com.bialger.domain.finance.enums.PaymentMethodType
import com.bialger.domain.finance.enums.PaymentStatusType
import com.bialger.domain.patient.enums.GenderType
import com.bialger.domain.patient.enums.LocalityType
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus

data class PageInfoGql(
    val page: Int,
    val size: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class PatientPageGql(
    val items: List<PatientGql>,
    val pageInfo: PageInfoGql
)

data class AppointmentPageGql(
    val items: List<AppointmentGql>,
    val pageInfo: PageInfoGql
)

data class PatientGql(
    val id: String,
    val organizationId: String,
    val cardNumber: String,
    val fullName: String,
    val gender: GenderType?,
    val birthDate: String?,
    val phone: String?,
    val email: String?,
    val registrationAddress: String?,
    val residenceAddress: String?,
    val localityType: LocalityType?,
    val citizenship: String?,
    val identityDocument: String?,
    val omsPolicy: String?,
    val snils: String?,
    val insuranceOrganization: String?,
    val contactPerson: String?,
    val guardian: String?,
    val profession: String?,
    val workplace: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class AppointmentGql(
    val id: String,
    val patientId: String,
    val employeeId: String,
    val timeSlotId: String?,
    val branchId: String,
    val roomId: String,
    val status: AppointmentStatus,
    val source: AppointmentSource,
    val notes: String?,
    val createdBy: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class PaymentGql(
    val id: String,
    val appointmentId: String,
    val amount: String,
    val paymentMethod: PaymentMethodType,
    val paymentStatus: PaymentStatusType,
    val notes: String?,
    val createdBy: String,
    val createdAt: String?
)

data class EmployeeGql(
    val id: String,
    val fullName: String,
    val email: String?,
    val phone: String?,
    val isActive: Boolean
)

data class BranchGql(
    val id: String,
    val organizationId: String,
    val name: String,
    val address: String?,
    val phone: String?,
    val isActive: Boolean
)

data class RoomGql(
    val id: String,
    val branchId: String,
    val name: String,
    val description: String?,
    val isActive: Boolean
)

data class PatientUpsertInput(
    val organizationId: String,
    val cardNumber: String,
    val fullName: String,
    val gender: GenderType?,
    val birthDate: String?,
    val phone: String?,
    val email: String?,
    val registrationAddress: String?,
    val residenceAddress: String?,
    val localityType: LocalityType?,
    val citizenship: String?,
    val identityDocument: String?,
    val omsPolicy: String?,
    val snils: String?,
    val insuranceOrganization: String?,
    val contactPerson: String?,
    val guardian: String?,
    val profession: String?,
    val workplace: String?
)
