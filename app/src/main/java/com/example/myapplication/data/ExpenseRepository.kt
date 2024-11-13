package com.example.myapplication.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import com.example.myapplication.data.datastore.PreferencesKeys
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs


class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val budgetComparisonDao: BudgetComparisonDao,
    private val pinnedCategoryDao: PinnedCategoryDao,
    private val walletDao: WalletDao,
    private val shiftDao: ShiftDao,
    private val payPeriodDao: PayPeriodDao,
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>,
    private val scope: CoroutineScope,
) {

    private val gson = Gson()

    suspend fun savePaySettings(settings: PaySettings) {
        try {
            val settingsJson = gson.toJson(settings)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PAY_SETTINGS] = settingsJson
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error saving pay settings", e)
            throw e
        }
    }

    suspend fun getPaySettings(): PaySettings {
        return try {
            val preferences = dataStore.data.first()
            preferences[PreferencesKeys.PAY_SETTINGS]?.let { json ->
                gson.fromJson(json, PaySettings::class.java)
            } ?: PaySettings.DEFAULT
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error loading pay settings", e)
            PaySettings.DEFAULT
        }
    }


    private var categoryPatterns: List<CategoryPattern>? = null

    suspend fun suggestCategory(description: String): String {
        // Lazy load patterns if not already loaded
        if (categoryPatterns == null) {
            categoryPatterns = expenseDao.getCategoryPatterns()
        }

        // Try exact match first
        categoryPatterns?.find { it.description == description }?.let {
            return it.category
        }

        // Try partial matches using keywords
        val words = description.split(" ")
            .filter { it.length > 3 } // Skip small words
            .sortedByDescending { it.length } // Try longer words first

        for (word in words) {
            val pattern = "%$word%"
            expenseDao.getMostFrequentCategoryForPattern(pattern)?.let {
                return it
            }
        }

        return "Uncategorized"
    }

    suspend fun updateExpenseWithCategory(expense: ExpenseData, suggestedCategory: String) {
        val finalExpense = expense.copy(category = suggestedCategory)
        insertExpense(finalExpense)
    }


    // Flow of budget comparisons
    val budgetComparisons = budgetComparisonDao.getAllBudgetComparisons()
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    suspend fun insertCategory(category: BudgetCategory) {
        budgetCategoryDao.insertCategory(category)

        // Create monthly budgets for the current year
        val currentYear = LocalDate.now().year
        for (month in 1..12) {
            val monthlyBudget = MonthlyBudget(
                year = currentYear,
                month = month,
                categoryName = category.categoryName,
                budgetedAmount = category.budgetedAmount
            )
            monthlyBudgetDao.insertMonthlyBudget(monthlyBudget)
        }

        // Calculate initial comparisons
        for (month in 1..12) {
            budgetComparisonDao.calculateAndStoreBudgetComparisons(
                expenseDao = expenseDao,
                budgetCategoryDao = budgetCategoryDao,
                year = currentYear,
                month = month
            )
        }
    }

    // Add these methods
    fun getPinnedCategories(): Flow<List<PinnedCategory>> =
        pinnedCategoryDao.getPinnedCategories()
            .catch { e ->
                Log.e("Repository", "Error getting pinned categories", e)
                emit(emptyList())
            }

    suspend fun togglePinnedCategory(categoryName: String) {
        if (pinnedCategoryDao.isCategoryPinned(categoryName)) {
            // If category is already pinned, delete it
            pinnedCategoryDao.deletePinnedCategory(categoryName)
            Log.d("Repository", "Unpinning category: $categoryName")
        } else {
            // If category is not pinned, add it
            pinnedCategoryDao.insertPinnedCategory(PinnedCategory(categoryName))
            Log.d("Repository", "Pinning category: $categoryName")
        }
    }


    suspend fun getCategoryByName(name: String): BudgetCategory? =
        budgetCategoryDao.getCategoryByName(name)

    fun getAllCategoriesFlow(): Flow<List<BudgetCategory>> =
        budgetCategoryDao.getAllCategoriesFlow()

    suspend fun updateCategory(category: BudgetCategory) {
        // First cleanup old data
        monthlyBudgetDao.deleteBudgetsByCategory(category.categoryName)
        budgetComparisonDao.deleteBudgetByCategory(category.categoryName)

        // Update the category
        budgetCategoryDao.updateCategory(category)

        // Create new monthly budgets for the current year
        val currentYear = LocalDate.now().year
        for (month in 1..12) {
            val monthlyBudget = MonthlyBudget(
                year = currentYear,
                month = month,
                categoryName = category.categoryName,
                budgetedAmount = category.budgetedAmount
            )
            monthlyBudgetDao.insertMonthlyBudget(monthlyBudget)
        }

        // Recalculate comparisons for each month
        for (month in 1..12) {
            budgetComparisonDao.calculateAndStoreBudgetComparisons(
                expenseDao = expenseDao,
                budgetCategoryDao = budgetCategoryDao,
                year = currentYear,
                month = month
            )
        }
    }

    suspend fun deleteCategory(category: BudgetCategory) {
        budgetCategoryDao.deleteCategory(category)
        budgetComparisonDao.deleteBudgetByCategory(category.categoryName)
        monthlyBudgetDao.deleteBudgetsByCategory(category.categoryName)
    }

    // Expenses
    fun getAllSources(): Flow<List<String>> = expenseDao.getAllSources()

    fun getAllExpensesFlow(): Flow<List<ExpenseData>> =
        expenseDao.getAllExpensesFlow()

    suspend fun insertExpense(expense: ExpenseData) {
        expenseDao.insertExpense(expense)
        // Cleanup old comparisons for this month before recalculating
        budgetComparisonDao.cleanupOldComparisons(expense.year, expense.month)
        calculateBudgetComparisons(expense.year, expense.month)
    }

    suspend fun deleteExpense(expense: ExpenseData) {
        expenseDao.deleteExpense(expense.id)
        calculateBudgetComparisons(expense.year, expense.month)
    }

    suspend fun updateExpenseCategory(expenseId: Int, newCategory: String) {
        // Get the current expense
        val expense = expenseDao.getExpenseById(expenseId) ?: return

        // Get the old category
        val oldCategory = expense.category

        // Update the expense category
        expenseDao.updateCategory(expenseId, newCategory)

        // Delete existing budget comparisons for both old and new categories
        budgetComparisonDao.deleteComparisonForPeriod(
            year = expense.year,
            month = expense.month,
            categoryName = oldCategory
        )
        budgetComparisonDao.deleteComparisonForPeriod(
            year = expense.year,
            month = expense.month,
            categoryName = newCategory
        )

        // Get all expenses for the month after the update
        val monthlyExpenses = expenseDao.getExpensesByYearAndMonth(expense.year, expense.month)

        // Get all budget categories
        val categories = budgetCategoryDao.getAllCategories()

        // Recalculate budget comparisons
        categories.forEach { category ->
            val totalExpenses = monthlyExpenses
                .filter { it.category == category.categoryName }
                .sumOf { it.amount }

            val budgetComparison = BudgetComparison(
                year = expense.year,
                month = expense.month,
                categoryName = category.categoryName,
                budgetedAmount = category.budgetedAmount,
                actualAmount = totalExpenses,
                difference = category.budgetedAmount - totalExpenses,
                percentageUsed = if (category.budgetedAmount != 0.0)
                    (abs(totalExpenses) / category.budgetedAmount) * 100
                else 0.0
            )

            budgetComparisonDao.insertBudgetComparison(budgetComparison)
        }
    }


    suspend fun clearAllExpenses() {
        expenseDao.clearAllExpenses()
        budgetComparisonDao.deleteAllComparisons()
    }

    // Budget Comparisons
    suspend fun calculateBudgetComparisons(year: Int, month: Int) {
        budgetComparisonDao.calculateAndStoreBudgetComparisons(
            expenseDao = expenseDao,
            budgetCategoryDao = budgetCategoryDao,
            year = year,
            month = month
        )
    }

    fun getAllBudgetComparisons(
        categories: List<String>,
        years: List<Int>,
        months: List<Int>
    ): Flow<List<BudgetComparison>> {
        return budgetComparisonDao.getBudgetComparisonsByFilters(
            years = if (years.isEmpty()) null else years,
            months = if (months.isEmpty()) null else months,
            categories = if (categories.isEmpty()) null else categories
        )
    }

    suspend fun updateMonthlyBudget(year: Int, month: Int, category: String, amount: Double) {
        if (monthlyBudgetDao.budgetExists(year, month, category)) {
            monthlyBudgetDao.updateBudgetAmount(year, month, category, amount)
        } else {
            monthlyBudgetDao.insertMonthlyBudget(
                MonthlyBudget(
                    year = year,
                    month = month,
                    categoryName = category,
                    budgetedAmount = amount
                )
            )
        }
    }

    // Add this new method
    suspend fun transactionExists(
        year: Int,
        month: Int,
        day: Int,
        amount: Double,
        description: String
    ): Boolean {
        return expenseDao.transactionExists(year, month, day, amount, description)
    }


    // Pay period related methods
    fun getUpcomingPayPeriods(): Flow<List<PayPeriod>> =
        payPeriodDao.getUpcomingPayPeriods(LocalDate.now())
            .map { entities ->
                entities.map { entity ->
                    PayPeriod(
                        startDate = entity.startDate,
                        endDate = entity.endDate,
                        payDate = entity.payDate
                    )
                }
            }


    // Helper method to get current pay period
    private suspend fun getCurrentPayPeriod(): PayPeriodEntity? {
        val today = LocalDate.now()
        return payPeriodDao.getUpcomingPayPeriods(today)
            .first()
            .firstOrNull { period ->
                today in period.startDate..period.endDate
            }
    }

    fun getShiftsForPayPeriod(
        employeeId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<WorkShift>> {
        return shiftDao.getShiftsForPayPeriod(
            employeeId = employeeId,
            startDate = startDate.toEpochDay(),
            endDate = endDate.toEpochDay()
        ).map { shifts -> shifts.map { it.toDomainModel() } }
    }
    suspend fun insertShift(
        employeeId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        breakDurationMinutes: Long,
        isWeekend: Boolean,
        isNightShift: Boolean,
        actualHoursWorked: Double?,
        status: String
    ) {
        val shift = WorkShift(
            employeeId = employeeId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            breakDuration = Duration.ofMinutes(breakDurationMinutes),
            isWeekend = isWeekend,
            isNightShift = isNightShift,
            actualHoursWorked = actualHoursWorked,
            status = status
        )
        shiftDao.insertShift(shift.toEntity())
    }

    suspend fun updateWalletBalance(balance: WalletBalance) {
        walletDao.updateBalanceWithReplace(
            WalletBalanceEntity(
                cashAmount = balance.cashAmount,
                lastUpdated = balance.lastUpdated
            )
        )
    }

    // Shift Management
    fun getUpcomingShifts(): Flow<List<WorkShift>> {
        return shiftDao.getUpcomingShifts(LocalDate.now().toEpochDay())
            .map { shifts ->
                shifts.map { it.toDomainModel() }
            }
    }

    suspend fun updateWorkShift(shift: WorkShift) {
        shiftDao.updateShift(shift.toEntity())
    }

    suspend fun saveWorkShift(shift: WorkShift) {
        try {
            shiftDao.insertShift(shift.toEntity())
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error saving work shift", e)
            throw e
        }
    }

    // Paycheck Calculation
    suspend fun calculatePaychecks(): List<PaycheckCalculation> {
        val settings = getPaySettings()

        return when (settings.employmentType) {
            EmploymentType.SALARY -> calculateSalaryPaychecks(settings)
            EmploymentType.HOURLY -> calculateHourlyPaychecks(settings)
        }
    }

    private suspend fun calculateSalaryPaychecks(settings: PaySettings): List<PaycheckCalculation> {
        val payPeriods = payPeriodDao.getUpcomingPayPeriods(LocalDate.now()).first()
        val annualSalary = settings.payRates.basePay

        return payPeriods.map { period ->
            val payPerPeriod = when (settings.payRates.payFrequency) {
                PayFrequency.WEEKLY -> annualSalary / 52
                PayFrequency.BI_WEEKLY -> annualSalary / 26
                PayFrequency.SEMI_MONTHLY -> annualSalary / 24
                PayFrequency.MONTHLY -> annualSalary / 12
            }

            calculatePaycheckWithDeductions(payPerPeriod, settings)
        }
    }

    private suspend fun calculateHourlyPaychecks(settings: PaySettings): List<PaycheckCalculation> {
        val shifts = getUpcomingShifts().first()
        val payPeriods = payPeriodDao.getUpcomingPayPeriods(LocalDate.now()).first()

        return payPeriods.map { period ->
            val periodShifts = shifts.filter { shift ->
                shift.date >= period.startDate && shift.date <= period.endDate
            }

            val regularHours = periodShifts.sumOf { calculateRegularHours(it) }
            val overtimeHours = periodShifts.sumOf { calculateOvertimeHours(it) }
            val weekendHours = periodShifts.filter { it.isWeekend }.sumOf { calculateShiftHours(it) }
            val nightHours = periodShifts.filter { it.isNightShift }.sumOf { calculateShiftHours(it) }

            val rates = settings.payRates
            val grossPay = (regularHours * rates.basePay) +
                    (overtimeHours * rates.basePay * rates.overtimeMultiplier) +
                    (weekendHours * rates.weekendRate) +
                    (nightHours * rates.nightDifferential)

            calculatePaycheckWithDeductions(grossPay, settings)
        }
    }

    private fun calculatePaycheckWithDeductions(
        grossPay: Double,
        settings: PaySettings
    ): PaycheckCalculation {
        val deductions = mutableMapOf<String, Double>()
        val taxSettings = settings.taxSettings

        // Calculate tax deductions
        if (taxSettings.federalWithholding) {
            deductions["Federal Tax"] = grossPay * 0.15 // Example rate
        }
        if (taxSettings.stateTaxEnabled) {
            deductions["State Tax"] = grossPay * (taxSettings.stateWithholdingPercentage / 100)
        }
        if (taxSettings.cityTaxEnabled) {
            deductions["City Tax"] = grossPay * (taxSettings.cityWithholdingPercentage / 100)
        }
        if (taxSettings.medicareTaxEnabled) {
            deductions["Medicare"] = grossPay * 0.0145 // 1.45%
        }
        if (taxSettings.socialSecurityTaxEnabled) {
            deductions["Social Security"] = (grossPay * 0.062).coerceAtMost(147000 * 0.062) // 6.2% up to cap
        }

        // Add custom deductions
        settings.deductions.forEach { deduction ->
            val deductionAmount = when (deduction.frequency) {
                DeductionFrequency.PER_PAYCHECK -> deduction.amount
                DeductionFrequency.MONTHLY -> deduction.amount / 2 // Assuming semi-monthly pay
                DeductionFrequency.ANNUAL -> deduction.amount / 24 // Assuming semi-monthly pay
            }
            deductions[deduction.name] = deductionAmount
        }

        val totalDeductions = deductions.values.sum()

        return PaycheckCalculation(
            grossPay = grossPay,
            netPay = grossPay - totalDeductions,
            regularHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = 0.0,
            nightHours = 0.0,
            deductions = deductions
        )
    }

    private fun calculateShiftHours(shift: WorkShift): Double {
        val duration = java.time.Duration.between(shift.startTime, shift.endTime)
        return (duration.toMinutes() - shift.breakDuration.toMinutes()) / 60.0
    }

    private fun calculateRegularHours(shift: WorkShift): Double {
        val hours = calculateShiftHours(shift)
        return if (hours > 8) 8.0 else hours
    }

    private fun calculateOvertimeHours(shift: WorkShift): Double {
        val hours = calculateShiftHours(shift)
        return if (hours > 8) hours - 8 else 0.0
    }

    suspend fun getWalletBalance(): WalletBalance? {
        return try {
            walletDao.getLatestBalance().first()?.let { entity ->
                WalletBalance(
                    cashAmount = entity.cashAmount,
                    lastUpdated = entity.lastUpdated
                )
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting wallet balance", e)
            null
        }
    }
    fun generatePayPeriodDates(
        startDate: LocalDate = LocalDate.now(),
        count: Int = 3
    ): List<PayPeriod> {
        return (0 until count).map { i ->
            val periodStart = startDate.plusMonths(i.toLong()).withDayOfMonth(1)
            val periodEnd = periodStart.plusMonths(1).minusDays(1)
            val payDate = periodEnd.plusDays(5) // Pay 5 days after period end

            PayPeriod(
                startDate = periodStart,
                endDate = periodEnd,
                payDate = payDate
            )
        }
    }

    suspend fun updatePayPeriodCalculations(employeeId: String, paySettings: PaySettings) {
        payPeriodDao.updatePayPeriodCalculations(employeeId, paySettings, shiftDao)
    }
}
