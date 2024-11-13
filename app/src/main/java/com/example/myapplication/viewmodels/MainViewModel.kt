package com.example.myapplication.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.BudgetCategory
import com.example.myapplication.data.BudgetComparison
import com.example.myapplication.data.CategoryBreakdown
import com.example.myapplication.data.ChartData
import com.example.myapplication.data.ExpenseData
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.Filter
import com.example.myapplication.data.StandardizedComparison
import com.example.myapplication.data.UiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.absoluteValue

class MainViewModel(private val repository: ExpenseRepository) : ViewModel() {
    // region State Declarations
    // region State Declarations
    private val _hasPermission = MutableStateFlow(false)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _expenses = MutableStateFlow<List<ExpenseData>>(emptyList())
    val expenses: StateFlow<List<ExpenseData>> = _expenses.asStateFlow()

    private val _filteredExpenses = MutableStateFlow<List<ExpenseData>>(emptyList())
    val filteredExpenses: StateFlow<List<ExpenseData>> = _filteredExpenses.asStateFlow()

    private val _categories = MutableStateFlow<List<BudgetCategory>>(emptyList())
    val categories: StateFlow<List<BudgetCategory>> = _categories.asStateFlow()

    private val _filteredComparisons = MutableStateFlow<List<BudgetComparison>>(emptyList())
    val filteredComparisons: StateFlow<List<BudgetComparison>> = _filteredComparisons.asStateFlow()

    // Update these to use the new Filter types
    private val _expenseFilters = MutableStateFlow(Filter.ExpenseFilters())
    val expenseFilters: StateFlow<Filter.ExpenseFilters> = _expenseFilters.asStateFlow()

    private val _budgetFilters = MutableStateFlow(Filter.BudgetComparisonFilters())
    val budgetFilters: StateFlow<Filter.BudgetComparisonFilters> = _budgetFilters.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _availableSources = MutableStateFlow<List<String>>(emptyList())
    val availableSources: StateFlow<List<String>> = _availableSources.asStateFlow()

    // Change List to Set
    private val _pinnedCategories = MutableStateFlow<Set<String>>(emptySet())
    val pinnedCategories: StateFlow<Set<String>> = _pinnedCategories.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<ExpenseData>>(emptyList())
    val recentTransactions: StateFlow<List<ExpenseData>> = _recentTransactions.asStateFlow()

    private val _chartData = MutableStateFlow(
        ChartData(
            totalBudget = 0.0,
            totalActual = 0.0,
            percentageUsed = 0.0,
            categoryBreakdown = emptyList()
        )
    )

    // Initialization

    init {
        // Launch each flow collection in its own coroutine
        viewModelScope.launch {
            repository.getAllSources().collect { sources ->
                Log.d("ViewModel", "Received sources: $sources")
                _availableSources.value = sources
            }
        }

        viewModelScope.launch {
            repository.getPinnedCategories().collect { pinnedCategories ->
                Log.d("ViewModel", "Received pinned categories: ${pinnedCategories.map { it.categoryName }}")
                _pinnedCategories.value = pinnedCategories.map { it.categoryName }.toSet()
            }
        }

        // Launch other initializations
        viewModelScope.launch {
            loadExpenses()
            loadCategories()
            initializeBudgetComparisons()
            loadRecentTransactions()
        }
    }

    private fun initializeBudgetComparisons() {
        viewModelScope.launch {
            combine(
                _budgetFilters,
                repository.budgetComparisons
            ) { budgetFilters, _ ->
                repository.getAllBudgetComparisons(
                    categories = budgetFilters.categories,
                    years = budgetFilters.years,
                    months = budgetFilters.months
                ).first()
            }.collect { filteredList ->
                _filteredComparisons.value = filteredList
            }
        }
    }

    // Data Loading Functions
    private fun loadExpenses() {
        viewModelScope.launch {
            repository.getAllExpensesFlow().collect { expensesList ->
                _expenses.value = expensesList
                applyFilters()
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategoriesFlow().collect { categoriesList ->
                _categories.value = categoriesList
            }
        }
    }

    private fun loadRecentTransactions() {
        viewModelScope.launch {
            repository.getAllExpensesFlow().collect { allExpenses ->
                _recentTransactions.value = allExpenses
                    .sortedByDescending { expense ->
                        // Sort by date (year, month, day)
                        "${expense.year}${expense.month.toString().padStart(2, '0')}${expense.day.toString().padStart(2, '0')}"
                    }
                    .take(50) // Get only the most recent 50 transactions
            }
        }
    }

    // Budget Category Management
    fun insertCategory(category: BudgetCategory) {
        viewModelScope.launch {
            try {
                repository.insertCategory(category)
                applyFilters()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error inserting category: ${e.message}", e)
            }
        }
    }

    fun deleteCategory(category: BudgetCategory) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                applyFilters()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting category: ${e.message}", e)
            }
        }
    }

    // Updated top three calculation


    // Expense Management
    fun addCashTransaction(amount: Double, description: String, category: String, date: LocalDate) {
        val newExpense = ExpenseData(
            id = generateUniqueId().toInt(),
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            amount = amount,
            description = description,
            category = category
        )
        viewModelScope.launch {
            repository.insertExpense(newExpense)
        }
    }

    // Add state for deleted items
    private data class DeletedItem<T>(
        val item: T,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val deletedExpenses = mutableListOf<DeletedItem<ExpenseData>>()
    private val deletedCategories = mutableListOf<DeletedItem<BudgetCategory>>()

    fun deleteExpense(expense: ExpenseData) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expense)
                deletedExpenses.add(DeletedItem(expense))
                // Notify UI about possible undo
                _uiEvent.emit(UiEvent.ShowUndoDelete("Expense deleted", "EXPENSE"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to delete expense"))
            }
        }
    }

    fun undoLastDelete(type: String) {
        viewModelScope.launch {
            when (type) {
                "EXPENSE" -> {
                    deletedExpenses.removeLastOrNull()?.let { deleted ->
                        repository.insertExpense(deleted.item)
                    }
                }
                "CATEGORY" -> {
                    deletedCategories.removeLastOrNull()?.let { deleted ->
                        repository.insertCategory(deleted.item)
                    }
                }
            }
        }
    }

    fun updateExpenseCategory(expenseId: Int, newCategory: String) {
        viewModelScope.launch {
            try {
                repository.updateExpenseCategory(expenseId, newCategory)
                // Refresh the data
                applyFilters()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating expense category: ${e.message}", e)
            }
        }
    }


    fun clearAllExpenses() {
        viewModelScope.launch {
            repository.clearAllExpenses()
        }
    }

    // Budget comparison filter updates
    fun updateBudgetYearFilters(selectedYears: List<Int>) {
        _budgetFilters.update { current ->
            current.copy(years = selectedYears)
        }
        applyBudgetFilters()
    }

    fun updateBudgetMonthFilters(selectedMonths: List<Int>) {
        _budgetFilters.update { current ->
            current.copy(months = selectedMonths)
        }
        applyBudgetFilters()
    }

    fun updateBudgetCategoryFilters(selectedCategories: List<String>) {
        _budgetFilters.update { current ->
            current.copy(categories = selectedCategories)
        }
        applyBudgetFilters()
    }

    // Update filter methods
    fun updateYearFilters(years: List<Int>) {
        _expenseFilters.value = _expenseFilters.value.copy(years = years)
        applyFilters()
    }

    fun updateMonthFilters(months: List<Int>) {
        _expenseFilters.value = _expenseFilters.value.copy(months = months)
        applyFilters()
    }

    fun updateCategoryFilters(categories: List<String>) {
        _expenseFilters.value = _expenseFilters.value.copy(categories = categories)
        applyFilters()
    }


    // Updated function for budget category updates
    fun updateBudgetCategory(categoryName: String, newBudgetValue: Double) {
        viewModelScope.launch {
            try {
                val currentEntry = repository.getCategoryByName(categoryName)
                if (currentEntry != null) {
                    val updatedEntry = currentEntry.copy(budgetedAmount = newBudgetValue)
                    repository.updateCategory(updatedEntry)

                    // Update monthly budget for current year/month
                    val currentDate = LocalDate.now()
                    repository.updateMonthlyBudget(
                        year = currentDate.year,
                        month = currentDate.monthValue,
                        category = categoryName,
                        amount = newBudgetValue
                    )

                    applyFilters() // Refresh the data
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating budget category: ${e.message}", e)
            }
        }
    }

    fun getBudgetCategories(): Flow<List<BudgetCategory>> = repository.getAllCategoriesFlow()


    // Separate apply methods for each screen
    private fun applyExpenseFilters() {
        viewModelScope.launch {
            val filters = _expenseFilters.value
            val source = _selectedSource.value

            _filteredExpenses.value = _expenses.value.filter { expense ->
                (filters.years.isEmpty() || filters.years.contains(expense.year)) &&
                        (filters.months.isEmpty() || filters.months.contains(expense.month)) &&
                        (filters.categories.isEmpty() || filters.categories.contains(expense.category)) &&
                        (source == null || expense.source == source) // String comparison, not CharCategory
            }
        }
    }

    private fun applyBudgetFilters() {
        viewModelScope.launch {
            _filteredComparisons.value = repository.getAllBudgetComparisons(
                categories = _budgetFilters.value.categories,
                years = _budgetFilters.value.years,
                months = _budgetFilters.value.months
            ).first()
        }
    }

    fun applyFilters() {
        viewModelScope.launch {
            try {
                // Apply existing filters
                applyExpenseFilters()
                applyBudgetFilters()

                // Calculate totals and update chart data
                val totalBudget = _filteredComparisons.value.sumOf { it.budgetedAmount }
                val totalActual = _filteredComparisons.value.sumOf { it.actualAmount }
                updateChartData(totalBudget, totalActual)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error applying filters: ${e.message}"))
            }
        }
    }


fun setSelectedSource(source: String?) {
        _selectedSource.value = source
        applyFilters()
    }

    fun clearSourceFilter() {
        _selectedSource.value = null
        applyFilters()
    }

    private fun updateChartData(totalBudget: Double, totalActual: Double) {
        viewModelScope.launch {
            try {
                val percentageUsed = if (totalBudget > 0) {
                    (totalActual.absoluteValue / totalBudget) * 100
                } else 0.0

                // Calculate category breakdown
                val categoryBreakdown = _filteredComparisons.value.map { comparison ->
                    CategoryBreakdown(
                        category = comparison.categoryName,
                        budgeted = comparison.budgetedAmount.absoluteValue,
                        actual = comparison.actualAmount.absoluteValue,
                        percentage = if (comparison.budgetedAmount != 0.0) {
                            (comparison.actualAmount.absoluteValue / comparison.budgetedAmount.absoluteValue) * 100
                        } else 0.0
                    )
                }

                _chartData.value = ChartData(
                    totalBudget = totalBudget,
                    totalActual = totalActual.absoluteValue,
                    percentageUsed = percentageUsed,
                    categoryBreakdown = categoryBreakdown
                )
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error updating chart data: ${e.message}"))
            }
        }
    }

    // Updated standardized comparisons calculation
    fun getStandardizedComparisons(): List<StandardizedComparison> {
        val filters = _budgetFilters.value
        val isMonthlyView = filters.months.isNotEmpty()

        return _filteredComparisons.value
            .filter { comparison ->
                (filters.years.isEmpty() || comparison.year in filters.years) &&
                        (filters.months.isEmpty() || comparison.month in filters.months) &&
                        (filters.categories.isEmpty() || comparison.categoryName in filters.categories)
            }
            .groupBy { it.categoryName }
            .map { (category, comparisons) ->
                // Determine if this is an income category
                val isIncome = category.contains("Income", ignoreCase = true)

                // Calculate total spent based on actual amounts
                val totalSpent = if (isMonthlyView) {
                    // For monthly view, sum only selected months
                    comparisons
                        .filter { it.month in filters.months }
                        .sumOf { it.actualAmount }
                } else {
                    // For yearly view, sum all months
                    comparisons.sumOf { it.actualAmount }
                }

                // Get monthly budget amount
                val monthlyBudget = comparisons.firstOrNull()?.budgetedAmount ?: 0.0

                // Calculate total budget based on view type
                val budgetedAmount = if (isMonthlyView) {
                    monthlyBudget // Use monthly budget as is for monthly view
                } else {
                    monthlyBudget * 12 // Multiply by 12 for yearly view
                }

                // Calculate percentage using absolute values
                val percentageUsed = if (budgetedAmount != 0.0) {
                    (abs(totalSpent) / abs(budgetedAmount)) * 100
                } else {
                    if (totalSpent != 0.0) 100.0 else 0.0
                }

                StandardizedComparison(
                    categoryName = category,
                    totalSpent = totalSpent,
                    budgetedAmount = budgetedAmount,
                    difference = budgetedAmount - totalSpent,
                    percentageUsed = percentageUsed,
                    isIncome = isIncome
                )
            }
    }
    fun calculateTotalIncome(): Double {
        return getStandardizedComparisons()
            .filter { it.isIncome }
            .sumOf { abs(it.totalSpent) }
    }

    fun calculateTotalBudget(): Double {
        return getStandardizedComparisons()
            .filter { !it.isIncome } // Exclude income categories
            .sumOf { abs(it.budgetedAmount) }
    }

    fun calculateTotalSpent(): Double {
        return getStandardizedComparisons()
            .filter { !it.isIncome }
            .sumOf { abs(it.totalSpent) }
    }


    // Add this function
    fun togglePinnedCategory(categoryName: String) {
        viewModelScope.launch {
            Log.d("MainViewModel", "Before toggle - Current pinned: ${_pinnedCategories.value}")
            repository.togglePinnedCategory(categoryName)

            // Update local state
            _pinnedCategories.value = if (_pinnedCategories.value.contains(categoryName)) {
                Log.d("MainViewModel", "Removing $categoryName from pinned set")
                _pinnedCategories.value - categoryName
            } else {
                Log.d("MainViewModel", "Adding $categoryName to pinned set")
                _pinnedCategories.value + categoryName
            }
            Log.d("MainViewModel", "After toggle - Updated pinned: ${_pinnedCategories.value}")
        }
    }


    // Add this to MainViewModel
    fun getCurrentMonthComparisons(): List<StandardizedComparison> {
        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year

        return _filteredComparisons.value
            .filter { comparison ->
                comparison.year == currentYear && comparison.month == currentMonth
            }
            .groupBy { it.categoryName }
            .map { (category, comparisons) ->
                val isIncome = category.contains("Income", ignoreCase = true)
                val comparison = comparisons.first()
                val actualAmount = abs(comparison.actualAmount)
                val budgetedAmount = abs(comparison.budgetedAmount)

                StandardizedComparison(
                    categoryName = category,
                    totalSpent = actualAmount,
                    budgetedAmount = budgetedAmount,
                    // Fix the remaining calculation
                    difference = budgetedAmount - actualAmount, // This is the fix
                    percentageUsed = if (budgetedAmount != 0.0) {
                        (actualAmount / budgetedAmount) * 100
                    } else {
                        if (actualAmount != 0.0) 100.0 else 0.0
                    },
                    isIncome = isIncome
                )
            }
    }



        // endregion

    // region Utility Functions
    private fun generateUniqueId(): Long = System.currentTimeMillis()

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }


    // endregion
}

