package com.example.myapplication.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import com.example.myapplication.utils.PaycheckCalculator
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

    private val _salaryTaxSettings = MutableStateFlow<TaxSettings>(TaxSettings())
    val salaryTaxSettings: StateFlow<TaxSettings> = _salaryTaxSettings.asStateFlow()

    private val _hourlyTaxSettings = MutableStateFlow<TaxSettings>(TaxSettings())
    val hourlyTaxSettings: StateFlow<TaxSettings> = _hourlyTaxSettings.asStateFlow()

    // Add new state flows for deductions
    private val _salaryDeductions = MutableStateFlow<List<Deduction>>(emptyList())
    val salaryDeductions: StateFlow<List<Deduction>> = _salaryDeductions.asStateFlow()

    private val _hourlyDeductions = MutableStateFlow<List<Deduction>>(emptyList())
    val hourlyDeductions: StateFlow<List<Deduction>> = _hourlyDeductions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(true) // Start with loading state
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _payments = MutableStateFlow<List<PaymentInfo>>(emptyList())
    val payments: StateFlow<List<PaymentInfo>> = _payments.asStateFlow()

    private val _navigateToWalletScreen = MutableLiveData<Unit>()
    val navigateToWalletScreen: LiveData<Unit> get() = _navigateToWalletScreen

    fun navigateToWalletScreen() { _navigateToWalletScreen.value = Unit }

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

        val frequency = when {
            _paySettings.value.salarySettings.enabled -> _paySettings.value.salarySettings.payFrequency
            _paySettings.value.hourlySettings.enabled -> _paySettings.value.hourlySettings.payFrequency
            else -> PayFrequency.BI_WEEKLY // Default frequency
        }

        val generatedPayPeriods = repository.generatePayPeriodDates(
            startDate = LocalDate.now(),
            count = 3,
            frequency = frequency
        )
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

        // Load tax settings and deductions
        viewModelScope.launch {
            try {
                repository.getSalaryDeductions().collect { deductions ->
                    _salaryDeductions.value = deductions
                }
                repository.getHourlyDeductions().collect { deductions ->
                    _hourlyDeductions.value = deductions
                }
            } catch (e: Exception) {
                _error.value = "Error loading deductions: ${e.message}"
            }
        }
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

    fun updateSalarySettings(
        enabled: Boolean,
        annualSalary: Double,
        frequency: PayFrequency
    ) {
        viewModelScope.launch {
            try {
                val currentSettings = _paySettings.value
                val updatedSettings = currentSettings.copy(
                    salarySettings = currentSettings.salarySettings.copy(
                        enabled = enabled,
                        annualSalary = annualSalary,
                        payFrequency = frequency
                    )
                )
                repository.savePaySettings(updatedSettings)
                _paySettings.value = updatedSettings
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Error updating salary settings: ${e.message}"
            }
        }
    }

    fun updateHourlySettings(
        enabled: Boolean,
        baseRate: Double,
        weekendRate: Double,
        nightDifferential: Double,
        overtimeMultiplier: Double,
        nightShiftStart: Int,
        nightShiftEnd: Int,
        frequency: PayFrequency
    ) {
        viewModelScope.launch {
            try {
                val currentSettings = _paySettings.value
                val updatedSettings = currentSettings.copy(
                    hourlySettings = currentSettings.hourlySettings.copy(
                        enabled = enabled,
                        baseRate = baseRate,
                        weekendRate = weekendRate,
                        nightDifferential = nightDifferential,
                        overtimeMultiplier = overtimeMultiplier,
                        nightShiftStart = nightShiftStart,
                        nightShiftEnd = nightShiftEnd,
                        payFrequency = frequency
                    )
                )
                repository.savePaySettings(updatedSettings)
                _paySettings.value = updatedSettings
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Error updating hourly settings: ${e.message}"
            }
        }
    }

    fun deleteShift(shift: WorkShift) {
        viewModelScope.launch {
            try {
                repository.deleteShift(shift)
                loadShifts() // Refresh shifts after deletion
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Error deleting shift: ${e.message}"
            }
        }
    }

    fun updateTaxSettings(salarySettings: TaxSettings, hourlySettings: TaxSettings) {
        viewModelScope.launch {
            try {
                repository.updateSalaryTaxSettings(salarySettings)
                repository.updateHourlyTaxSettings(hourlySettings)
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Failed to update tax settings: ${e.message}"
            }
        }
    }

    fun addDeduction(deduction: Deduction, isForSalary: Boolean) {
        viewModelScope.launch {
            try {
                if (isForSalary) {
                    repository.addSalaryDeduction(deduction)
                } else {
                    repository.addHourlyDeduction(deduction)
                }
                recalculatePaychecks()
            } catch (e: Exception) {
                _error.value = "Failed to add deduction: ${e.message}"
            }
        }
    }

    fun removeDeduction(deduction: Deduction, isForSalary: Boolean) {
        viewModelScope.launch {
            try {
                if (isForSalary) {
                    repository.removeSalaryDeduction(deduction)
                } else {
                    repository.removeHourlyDeduction(deduction)
                }
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
                _payments.value = calculateUpcomingPayments()
            } catch (e: Exception) {
                _error.value = "Error calculating paychecks: ${e.message}"
            }
        }
    }


    private fun calculateUpcomingPayments(): List<PaymentInfo> {
        val settings = _paySettings.value
        val payPeriods = _payPeriods.value
        val shifts = _shifts.value

        return payPeriods.map { period ->
            // Get shifts for this period
            val periodShifts = shifts.filter { shift ->
                shift.date >= period.startDate && shift.date <= period.endDate
            }

            // Use existing PaycheckCalculator to calculate the paycheck
            val paycheck = PaycheckCalculator.calculatePaycheck(periodShifts, settings)

            PaymentInfo(
                employeeName = "Current Employee",
                amount = paycheck.grossPay,
                date = period.payDate,
                isEstimate = period.payDate > LocalDate.now()
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}