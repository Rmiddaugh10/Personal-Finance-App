package com.example.myapplication.utils

import com.example.myapplication.data.*
import java.time.Duration

object PaycheckCalculator {
    fun calculatePaycheck(
        shifts: List<WorkShift>,
        settings: PaySettings
    ): PaycheckCalculation {
        val hourlySettings = settings.hourlySettings
        val salarySettings = settings.salarySettings

        // Track hours and pay separately for each type
        var totalRegularHours = 0.0
        var totalOvertimeHours = 0.0
        var totalWeekendHours = 0.0
        var totalNightHours = 0.0
        var totalPay = 0.0

        // Calculate hourly pay if enabled
        if (hourlySettings.enabled) {
            shifts.forEach { shift ->
                val (regularHours, nightHours) = calculateShiftHours(
                    shift,
                    hourlySettings.nightShiftStart,
                    hourlySettings.nightShiftEnd
                )

                if (shift.isWeekend) {
                    totalWeekendHours += (regularHours + nightHours)
                    totalPay += totalWeekendHours * hourlySettings.weekendRate
                } else {
                    // Add regular hours up to 40
                    val remainingRegularHours = 40.0 - totalRegularHours
                    if (regularHours <= remainingRegularHours) {
                        totalRegularHours += regularHours
                        totalPay += regularHours * hourlySettings.baseRate
                    } else {
                        totalRegularHours += remainingRegularHours
                        totalOvertimeHours += regularHours - remainingRegularHours
                        totalPay += remainingRegularHours * hourlySettings.baseRate +
                                (regularHours - remainingRegularHours) * hourlySettings.baseRate * hourlySettings.overtimeMultiplier
                    }

                    // Add night differential hours
                    totalNightHours += nightHours
                    totalPay += nightHours * (hourlySettings.baseRate + hourlySettings.nightDifferential)
                }
            }
        }

        // Add salary pay if enabled
        if (salarySettings.enabled) {
            val annualSalary = salarySettings.annualSalary
            val salaryPerPeriod = when (salarySettings.payFrequency) {
                PayFrequency.WEEKLY -> annualSalary / 52
                PayFrequency.BI_WEEKLY -> annualSalary / 26
                PayFrequency.SEMI_MONTHLY -> annualSalary / 24
                PayFrequency.MONTHLY -> annualSalary / 12
            }
            totalPay += salaryPerPeriod
        }

        // Calculate deductions
        val deductions = calculateDeductions(totalPay, settings)
        val totalDeductions = deductions.values.sum()

        return PaycheckCalculation(
            grossPay = totalPay,
            netPay = totalPay - totalDeductions,
            regularHours = totalRegularHours,
            overtimeHours = totalOvertimeHours,
            weekendHours = totalWeekendHours,
            nightHours = totalNightHours,
            deductions = deductions
        )
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

        return ShiftHours(
            regular = regularHours,
            night = nightHours
        )
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

    private fun calculateDeductions(grossPay: Double, settings: PaySettings): Map<String, Double> {
        val deductions = mutableMapOf<String, Double>()

        // Apply tax settings
        with(settings.taxSettings) {
            if (federalWithholding) {
                deductions["Federal Tax"] = grossPay * 0.22 // Example rate
            }
            if (stateTaxEnabled) {
                deductions["State Tax"] = grossPay * (stateWithholdingPercentage / 100)
            }
            if (cityTaxEnabled) {
                deductions["City Tax"] = grossPay * (cityWithholdingPercentage / 100)
            }
            if (medicareTaxEnabled) {
                deductions["Medicare"] = grossPay * 0.0145
            }
            if (socialSecurityTaxEnabled) {
                deductions["Social Security"] = (grossPay * 0.062).coerceAtMost(147000 * 0.062)
            }
        }

        // Apply custom deductions
        settings.deductions.forEach { deduction ->
            val amount = when (deduction.frequency) {
                DeductionFrequency.PER_PAYCHECK -> deduction.amount
                DeductionFrequency.MONTHLY -> deduction.amount / 2 // Assuming semi-monthly
                DeductionFrequency.ANNUAL -> deduction.amount / 24 // Assuming semi-monthly
            }
            deductions[deduction.name] = amount
        }

        return deductions
    }
}