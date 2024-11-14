package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WalletViewModel(private val repository: ExpenseRepository) : ViewModel() {
    private val _walletBalance = MutableStateFlow<WalletBalance?>(null)
    val walletBalance: StateFlow<WalletBalance?> = _walletBalance.asStateFlow()

    private val _shifts = MutableStateFlow<List<WorkShift>>(emptyList())
    val shifts: StateFlow<List<WorkShift>> = _shifts.asStateFlow()

    private val _payPeriods = MutableStateFlow<List<PayPeriod>>(emptyList())
    val payPeriods: StateFlow<List<PayPeriod>> = _payPeriods.asStateFlow()

    private val _paychecks = MutableStateFlow<List<PaycheckCalculation>>(emptyList())
    val paychecks: StateFlow<List<PaycheckCalculation>> = _paychecks.asStateFlow()

    private val _showUpdateBalanceDialog = MutableStateFlow(false)
    val showUpdateBalanceDialog: StateFlow<Boolean> = _showUpdateBalanceDialog.asStateFlow()

    private val _paySettings = MutableStateFlow(PaySettings())
    val paySettings: StateFlow<PaySettings> = _paySettings.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(true) // Start with loading state
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _payments = MutableStateFlow<List<PaymentInfo>>(emptyList())
    val payments: StateFlow<List<PaymentInfo>> = _payments.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                loadInitialData()
            } catch (e: Exception) {
                _error.value = "Error loading data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadInitialData() {
        // Load wallet balance
        _walletBalance.value = repository.getWalletBalance()

        // Load pay settings
        _paySettings.value = repository.getPaySettings()

        // Load upcoming pay periods
        val generatedPayPeriods = repository.generatePayPeriodDates()
        _payPeriods.value = generatedPayPeriods

        // Load shifts
        viewModelScope.launch {
            repository.getShiftsForPayPeriod(
                employeeId = "current_user",
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusMonths(3)
            ).collect { shifts ->
                _shifts.value = shifts
            }
        }

        // Calculate initial paychecks
        _paychecks.value = repository.calculatePaychecks()
    }

    fun showUpdateBalanceDialog() {
        _showUpdateBalanceDialog.value = true
    }

    fun hideUpdateBalanceDialog() {
        _showUpdateBalanceDialog.value = false
    }

    fun updateWalletBalance(newAmount: Double) {
        viewModelScope.launch {
            try {
                val newBalance = WalletBalance(
                    cashAmount = newAmount,
                    lastUpdated = LocalDateTime.now()
                )
                repository.updateWalletBalance(newBalance)
                _walletBalance.value = newBalance
            } catch (e: Exception) {
                _error.value = "Error updating balance: ${e.message}"
            }
        }
    }

    fun addShift(
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        breakDuration: Long
    ) {
        viewModelScope.launch {
            try {
                val shift = WorkShift(
                    employeeId = "current_user",
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    breakDuration = Duration.ofMinutes(breakDuration),
                    isWeekend = date.dayOfWeek.value >= 6,
                    isNightShift = startTime.hour >= 18 || endTime.hour <= 6,
                    actualHoursWorked = null,
                    status = "SCHEDULED"
                )
                repository.insertShift(
                    employeeId = shift.employeeId,
                    date = shift.date,
                    startTime = shift.startTime,
                    endTime = shift.endTime,
                    breakDurationMinutes = shift.breakDuration.toMinutes(),
                    isWeekend = shift.isWeekend,
                    isNightShift = shift.isNightShift,
                    actualHoursWorked = shift.actualHoursWorked,
                    status = shift.status
                )
                // Refresh shifts after adding
                loadShifts()
            } catch (e: Exception) {
                _error.value = "Error adding shift: ${e.message}"
            }
        }
    }
    private fun loadShifts() {
        viewModelScope.launch {
            repository.getShiftsForPayPeriod(
                employeeId = "current_user",
                startDate = LocalDate.now().minusMonths(1),  // Include past month
                endDate = LocalDate.now().plusMonths(3)
            ).collect { shifts ->
                _shifts.value = shifts
            }
        }
    }

    fun updateShift(shift: WorkShift) {
        viewModelScope.launch {
            try {
                repository.updateWorkShift(shift)
                loadShifts() // Refresh shifts after update
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Error updating shift: ${e.message}"
            }
        }
    }

    fun updateEmploymentType(type: EmploymentType) {
        viewModelScope.launch {
            try {
                val updatedSettings = _paySettings.value.copy(employmentType = type)
                repository.savePaySettings(updatedSettings)
                _paySettings.value = updatedSettings
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Error updating employment type: ${e.message}"
            }
        }
    }

    fun updatePayRates(rates: PayRates) {
        viewModelScope.launch {
            try {
                val updatedSettings = _paySettings.value.copy(payRates = rates)
                repository.savePaySettings(updatedSettings)
                _paySettings.value = updatedSettings
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Error updating pay rates: ${e.message}"
            }
        }
    }

    fun updateTaxSettings(taxSettings: TaxSettings) {
        viewModelScope.launch {
            try {
                val updatedSettings = _paySettings.value.copy(taxSettings = taxSettings)
                _paySettings.value = updatedSettings
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Failed to update tax settings: ${e.message}"
            }
        }
    }

    fun addDeduction(deduction: Deduction) {
        viewModelScope.launch {
            try {
                val currentDeductions = _paySettings.value.deductions
                val updatedSettings = _paySettings.value.copy(
                    deductions = currentDeductions + deduction
                )
                _paySettings.value = updatedSettings
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Failed to add deduction: ${e.message}"
            }
        }
    }

    fun removeDeduction(deduction: Deduction) {
        viewModelScope.launch {
            try {
                val currentDeductions = _paySettings.value.deductions
                val updatedSettings = _paySettings.value.copy(
                    deductions = currentDeductions - deduction
                )
                _paySettings.value = updatedSettings
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Failed to remove deduction: ${e.message}"
            }
        }
    }

    private fun recalculatePaychecks() {
        viewModelScope.launch {
            try {
                _paychecks.value = repository.calculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Error calculating paychecks: ${e.message}"
            }
        }
    }

    private fun calculateDeductions(grossPay: Double, settings: PaySettings): Map<String, Double> {
        val deductions = mutableMapOf<String, Double>()

        // Tax calculations
        if (settings.taxSettings.federalWithholding) {
            deductions["Federal Tax"] = grossPay * 0.15 // Example rate
        }
        if (settings.taxSettings.stateTaxEnabled) {
            deductions["State Tax"] = grossPay * (settings.taxSettings.stateWithholdingPercentage / 100)
        }
        if (settings.taxSettings.cityTaxEnabled) {
            deductions["City Tax"] = grossPay * (settings.taxSettings.cityWithholdingPercentage / 100)
        }

        // Add custom deductions
        settings.deductions.forEach { deduction ->
            deductions[deduction.name] = when (deduction.frequency) {
                DeductionFrequency.PER_PAYCHECK -> deduction.amount
                DeductionFrequency.MONTHLY -> deduction.amount / 2 // Assuming semi-monthly pay
                DeductionFrequency.ANNUAL -> deduction.amount / 24 // Assuming semi-monthly pay
            }
        }

        return deductions
    }

    private fun calculateShiftHours(shift: WorkShift): Double {
        val duration = Duration.between(shift.startTime, shift.endTime)
        return (duration.toMinutes() - shift.breakDuration.toMinutes()) / 60.0
    }

    private fun updatePayments() {
        viewModelScope.launch {
            val newPayments = calculateUpcomingPayments()
            _payments.value = newPayments
        }
    }

    private fun calculateUpcomingPayments(): List<PaymentInfo> {
        // This will be implemented based on your payment calculation logic
        return emptyList() // Placeholder
    }


    fun clearError() {
        _error.value = null
    }
}