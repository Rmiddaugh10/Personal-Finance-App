package com.example.myapplication.data

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Combined data models file - PayrollModels.kt
data class PayRate(
    val baseRate: Double,
    val weekendRate: Double,
    val nightDifferential: Double,
    val overtimeMultiplier: Double = 1.5
)

data class PayPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val payDate: LocalDate
)

data class WorkShift(
    val id: Long = 0,
    val employeeId: String = "current_employee", // Default value
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val breakDuration: Duration,
    val isWeekend: Boolean = date.dayOfWeek.value >= 6, // Auto-calculate from date
    val isNightShift: Boolean = startTime.hour >= 18 || endTime.hour <= 6, // Auto-calculate from time
    val actualHoursWorked: Double? = null,
    val status: String = "SCHEDULED" // Default value
)

data class TaxInfo(
    val federalWithholding: Double,
    val stateWithholding: Double,
    val socialSecurity: Double = 0.062, // 6.2% default
    val medicare: Double = 0.0145, // 1.45% default
    val additionalDeductions: Map<String, Double>
)

data class PaycheckCalculation(
    val grossPay: Double,
    val netPay: Double,
    val regularHours: Double,
    val overtimeHours: Double,
    val weekendHours: Double,
    val nightHours: Double,
    val deductions: Map<String, Double>
)

data class WalletBalance(
    val cashAmount: Double,
    val lastUpdated: LocalDateTime
)

data class PaymentInfo(
    val employeeName: String,
    val amount: Double,
    val date: LocalDate,
    val isEstimate: Boolean
)