package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


@Dao
interface BudgetComparisonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetComparison(comparison: BudgetComparison)

    @Query("SELECT * FROM budget_comparison")
    fun getAllBudgetComparisons(): Flow<List<BudgetComparison>>

    @Query("""
        DELETE FROM budget_comparison 
        WHERE year = :year 
        AND month = :month 
        AND categoryName = :categoryName
    """)
    suspend fun deleteComparisonForPeriod(year: Int, month: Int, categoryName: String)



    @Query("""
        SELECT 
            0 as id,
            mb.year,
            mb.month,
            mb.categoryName,
            mb.budgetedAmount,
            COALESCE(SUM(e.amount), 0.0) as actualAmount,
            mb.budgetedAmount - COALESCE(SUM(e.amount), 0.0) as difference,
            CASE 
                WHEN mb.budgetedAmount != 0 
                THEN (ABS(COALESCE(SUM(e.amount), 0.0)) / ABS(mb.budgetedAmount)) * 100
                ELSE 0.0
            END as percentageUsed
        FROM monthly_budgets mb
        LEFT JOIN expenses e ON 
            mb.year = e.year AND
            mb.month = e.month AND
            mb.categoryName = e.category
        WHERE (:years IS NULL OR mb.year IN (:years))
            AND (:months IS NULL OR mb.month IN (:months))
            AND (:categories IS NULL OR mb.categoryName IN (:categories))
        GROUP BY mb.year, mb.month, mb.categoryName
    """)
    fun getBudgetComparisonsByFilters(
        years: List<Int>?,
        months: List<Int>?,
        categories: List<String>?
    ): Flow<List<BudgetComparison>>


    @Query("""
        DELETE FROM budget_comparison 
        WHERE year = :year 
        AND month = :month 
        AND categoryName = :categoryName
    """)
    suspend fun deleteBudgetComparisonForExpense(
        year: Int,
        month: Int,
        categoryName: String
    )

    @Query("DELETE FROM budget_comparison WHERE categoryName = :categoryName")
    suspend fun deleteBudgetByCategory(categoryName: String)

    @Query("DELETE FROM budget_comparison")
    suspend fun deleteAllComparisons()

    @Query("""
        INSERT INTO budget_comparison (
            year, 
            month, 
            categoryName, 
            budgetedAmount, 
            actualAmount,
            difference,
            percentageUsed
        )
        SELECT 
            :year AS year,
            :month AS month,
            :categoryName AS categoryName,
            ABS(bc.budgetedAmount) as budgetedAmount,
            ABS(COALESCE((
                SELECT SUM(amount) 
                FROM expenses 
                WHERE year = :year 
                AND month = :month 
                AND category = :categoryName
            ), 0)) as actualAmount,
            ABS(bc.budgetedAmount) - ABS(COALESCE((
                SELECT SUM(amount) 
                FROM expenses 
                WHERE year = :year 
                AND month = :month 
                AND category = :categoryName
            ), 0)) as difference,
            CASE 
                WHEN ABS(bc.budgetedAmount) = 0 THEN 0
                ELSE (ABS(COALESCE((
                    SELECT SUM(amount) 
                    FROM expenses 
                    WHERE year = :year 
                    AND month = :month 
                    AND category = :categoryName
                ), 0)) / ABS(bc.budgetedAmount)) * 100
            END as percentageUsed
        FROM budget_category bc
        WHERE bc.categoryName = :categoryName
    """)
    suspend fun insertBudgetComparison(
        year: Int,
        month: Int,
        categoryName: String
    )

    @Transaction
    suspend fun calculateAndStoreBudgetComparisons(
        expenseDao: ExpenseDao,
        budgetCategoryDao: BudgetCategoryDao,
        year: Int,
        month: Int
    ) {
        // First, delete all existing comparisons for this period
        cleanupOldComparisons(year, month)

        val expenses = expenseDao.getExpensesByYearAndMonth(year, month)
        val categories = budgetCategoryDao.getAllCategories()

        categories.forEach { category ->
            val totalExpenses = expenses
                .filter { it.category == category.categoryName }
                .sumOf { it.amount }

            val comparison = BudgetComparison(
                year = year,
                month = month,
                categoryName = category.categoryName,
                budgetedAmount = category.budgetedAmount,
                actualAmount = totalExpenses,
                difference = category.budgetedAmount - totalExpenses,
                percentageUsed = if (category.budgetedAmount != 0.0)
                    (Math.abs(totalExpenses) / Math.abs(category.budgetedAmount)) * 100
                else 0.0
            )
            insertBudgetComparison(comparison)
        }
    }

    @Transaction
    suspend fun updateBudgetComparisons(year: Int, month: Int, categories: List<String>) {
        categories.forEach { category ->
            deleteBudgetComparisonForExpense(year, month, category)
            insertBudgetComparison(year, month, category)
        }
    }
    @Query("DELETE FROM budget_comparison WHERE year = :year AND month = :month")
    suspend fun cleanupOldComparisons(year: Int, month: Int)
}