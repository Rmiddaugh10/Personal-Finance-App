package com.example.myapplication.utils

import com.example.myapplication.data.*
import java.time.Duration

object PaycheckCalculator {
    private const val MEDICARE_RATE = 0.0133  // 1.45%
    private const val SOCIAL_SECURITY_RATE = 0.057  // 6.2%
    private const val SOCIAL_SECURITY_WAGE_CAP = 147000.0  // 2024 wage base

    fun calculatePaycheck(
        shifts: List<WorkShift>,
        settings: PaySettings
    ): PaycheckCalculation {
        // If both are enabled, prioritize hourly when shifts exist
        if (settings.hourlySettings.enabled && shifts.isNotEmpty()) {
            val hourlyPay = calculateHourlyPay(
                shifts = shifts,
                hourlySettings = settings.hourlySettings,
                hourlyHours = HourlyHours(
                    regularHours = 0.0,
                    overtimeHours = 0.0,
                    weekendHours = 0.0,
                    nightHours = 0.0
                )
            )

            val deductions = calculateDeductions(
                grossPay = hourlyPay.pay,
                taxSettings = settings.hourlyTaxSettings,
                deductions = settings.hourlyDeductions
            )

            return PaycheckCalculation(
                grossPay = hourlyPay.pay,
                netPay = hourlyPay.pay - deductions.values.sum(),
                regularHours = hourlyPay.hours.regularHours,
                overtimeHours = hourlyPay.hours.overtimeHours,
                weekendHours = hourlyPay.hours.weekendHours,
                nightHours = hourlyPay.hours.nightHours,
                deductions = deductions
            )
        }

        // Return salary calculation when no shifts or only salary enabled
        if (settings.salarySettings.enabled) {
            val salaryPay = calculateSalaryPay(settings.salarySettings)
            val deductions = calculateDeductions(
                grossPay = salaryPay,
                taxSettings = settings.salaryTaxSettings,
                deductions = settings.salaryDeductions
            )

            return PaycheckCalculation(
                grossPay = salaryPay,
                netPay = salaryPay - deductions.values.sum(),
                regularHours = 0.0,
                overtimeHours = 0.0,
                weekendHours = 0.0,
                nightHours = 0.0,
                deductions = deductions
            )
        }

        // Return zero-value calculation if neither enabled
        return PaycheckCalculation(
            grossPay = 0.0,
            netPay = 0.0,
            regularHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = 0.0,
            nightHours = 0.0,
            deductions = emptyMap()
        )
    }

    private data class HourlyHours(
        val regularHours: Double,
        val overtimeHours: Double,
        val weekendHours: Double,
        val nightHours: Double
    )

    private data class HourlyPayResult(
        val pay: Double,
        val hours: HourlyHours
    )

    private fun calculateHourlyPay(
        shifts: List<WorkShift>,
        hourlySettings: HourlySettings,
        hourlyHours: HourlyHours
    ): HourlyPayResult {
        var totalPay = 0.0
        var regularHours = hourlyHours.regularHours
        var overtimeHours = hourlyHours.overtimeHours
        var weekendHours = hourlyHours.weekendHours
        var nightHours = hourlyHours.nightHours

        // Get overtime threshold based on pay frequency
        val overtimeThreshold = when (hourlySettings.payFrequency) {
            PayFrequency.WEEKLY -> 40.0
            PayFrequency.BI_WEEKLY -> 80.0
            PayFrequency.SEMI_MONTHLY -> 86.67
            PayFrequency.MONTHLY -> 173.33
        }

        // Calculate total regular hours for the period
        val totalRegularHours = shifts.sumOf { shift ->
            val (regularShiftHours, nightShiftHours) = calculateShiftHours(
                shift,
                hourlySettings.nightShiftStart,
                hourlySettings.nightShiftEnd
            )
            if (!shift.isWeekend) regularShiftHours + nightShiftHours else 0.0
        }

        // Calculate overtime if total hours exceed threshold
        if (totalRegularHours > overtimeThreshold) {
            regularHours = overtimeThreshold
            overtimeHours = totalRegularHours - overtimeThreshold
        } else {
            regularHours = totalRegularHours
            overtimeHours = 0.0
        }

        // Calculate pay for regular and overtime hours
        totalPay += regularHours * hourlySettings.baseRate
        totalPay += overtimeHours * hourlySettings.baseRate * hourlySettings.overtimeMultiplier

        // Add weekend and night differential pay
        shifts.forEach { shift ->
            val (regularShiftHours, nightShiftHours) = calculateShiftHours(
                shift,
                hourlySettings.nightShiftStart,
                hourlySettings.nightShiftEnd
            )

            if (shift.isWeekend) {
                weekendHours += (regularShiftHours + nightShiftHours)
                totalPay += weekendHours * hourlySettings.weekendRate
            }

            if (!shift.isWeekend) {
                nightHours += nightShiftHours
                totalPay += nightShiftHours * hourlySettings.nightDifferential
            }
        }

        return HourlyPayResult(
            pay = totalPay,
            hours = HourlyHours(
                regularHours = regularHours,
                overtimeHours = overtimeHours,
                weekendHours = weekendHours,
                nightHours = nightHours
            )
        )
    }

    private fun calculateSalaryPay(settings: SalarySettings): Double {
        return when (settings.payFrequency) {
            PayFrequency.WEEKLY -> settings.annualSalary / 52
            PayFrequency.BI_WEEKLY -> settings.annualSalary / 26
            PayFrequency.SEMI_MONTHLY -> settings.annualSalary / 24
            PayFrequency.MONTHLY -> settings.annualSalary / 12
        }
    }

    private data class ShiftHours(
        val regular: Double,
        val night: Double
    )

    private fun calculateShiftHours(
        shift: WorkShift,
        nightShiftStart: Int,
        nightShiftEnd: Int
    ): ShiftHours {
        val shiftDuration = Duration.between(shift.startTime, shift.endTime)
        val breakDuration = shift.breakDuration
        val totalMinutes = shiftDuration.toMinutes() - breakDuration.toMinutes()

        var nightMinutes = 0L
        var currentTime = shift.startTime
        while (currentTime.isBefore(shift.endTime)) {
            if (isNightShiftHour(currentTime.hour, nightShiftStart, nightShiftEnd)) {
                nightMinutes++
            }
            currentTime = currentTime.plusMinutes(1)
        }

        val nightHours = nightMinutes / 60.0
        val regularHours = (totalMinutes / 60.0) - nightHours

        return ShiftHours(regular = regularHours, night = nightHours)
    }

    private fun isNightShiftHour(hour: Int, nightShiftStart: Int, nightShiftEnd: Int): Boolean {
        return if (nightShiftStart > nightShiftEnd) {
            // Overnight shift (e.g., 18-6)
            hour >= nightShiftStart || hour < nightShiftEnd
        } else {
            // Same day shift
            hour >= nightShiftStart && hour < nightShiftEnd
        }
    }

    private fun calculateDeductions(
        grossPay: Double,
        taxSettings: TaxSettings,
        deductions: List<Deduction>
    ): Map<String, Double> {
        val result = mutableMapOf<String, Double>()

        // Federal Tax
        if (taxSettings.federalWithholding) {
            result["Federal Tax"] = grossPay * (taxSettings.federalTaxRate / 100)
        }

        // State Tax
        if (taxSettings.stateTaxEnabled) {
            result["State Tax"] = grossPay * (taxSettings.stateWithholdingPercentage / 100)
        }

        // City Tax
        if (taxSettings.cityTaxEnabled) {
            result["City Tax"] = grossPay * (taxSettings.cityWithholdingPercentage / 100)
        }

        // Medicare
        if (taxSettings.medicareTaxEnabled) {
            result["Medicare"] = grossPay * MEDICARE_RATE
        }

        // Social Security
        if (taxSettings.socialSecurityTaxEnabled) {
            val socialSecurityWages = grossPay.coerceAtMost(SOCIAL_SECURITY_WAGE_CAP)
            result["Social Security"] = socialSecurityWages * SOCIAL_SECURITY_RATE
        }

        // Custom deductions
        deductions.forEach { deduction ->
            val amount = when (deduction.frequency) {
                DeductionFrequency.PER_PAYCHECK -> deduction.amount
                DeductionFrequency.MONTHLY -> deduction.amount / 2  // Assuming semi-monthly pay
                DeductionFrequency.ANNUAL -> deduction.amount / 24  // Assuming semi-monthly pay
            }
            result[deduction.name] = amount
        }

        return result
    }
}