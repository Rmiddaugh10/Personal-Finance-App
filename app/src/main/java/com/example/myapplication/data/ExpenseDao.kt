package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY year DESC, month DESC, day DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseData>>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<ExpenseData>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Int): ExpenseData?

    @Query("SELECT * FROM expenses WHERE year = :year AND month = :month")
    suspend fun getExpensesByYearAndMonth(year: Int, month: Int): List<ExpenseData>

    // Keep the transaction existence check
    // Add to ExpenseDao interface
    @Query("""
    SELECT EXISTS (
        SELECT 1 FROM expenses 
        WHERE year = :year 
        AND month = :month 
        AND day = :day 
        AND ABS(amount - :amount) < 0.01
        AND description = :description
    )
""")
    suspend fun transactionExists(
        year: Int,
        month: Int,
        day: Int,
        amount: Double,
        description: String
    ): Boolean

    @Transaction
    suspend fun safeInsertExpense(expense: ExpenseData): Long {
        val exists = transactionExists(
            expense.year,
            expense.month,
            expense.day,
            expense.amount,
            expense.description
        )
        return if (!exists) {
            insertExpense(expense)
        } else {
            -1 // Return -1 to indicate duplicate
        }
    }

    @Insert
    suspend fun insertExpense(expense: ExpenseData): Long

    @Query("UPDATE expenses SET category = :newCategory WHERE id = :expenseId")
    suspend fun updateCategory(expenseId: Int, newCategory: String)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("SELECT DISTINCT year, month FROM expenses")
    suspend fun getAllExpensePeriods(): List<YearMonth>

    data class YearMonth(val year: Int, val month: Int)

    @Query("SELECT DISTINCT source FROM expenses WHERE source IS NOT NULL")
    fun getAllSources(): Flow<List<String>>

    @Query("""
        SELECT * FROM expenses 
        WHERE (:source IS NULL OR source = :source)
        ORDER BY year DESC, month DESC, day DESC
    """)
    fun getExpensesBySource(source: String?): Flow<List<ExpenseData>>

    @Query("""
        SELECT * FROM expenses 
        WHERE (:years IS NULL OR year IN (:years))
        AND (:months IS NULL OR month IN (:months))
        AND (:categories IS NULL OR category IN (:categories))
        AND (:source IS NULL OR source = :source)
        ORDER BY year DESC, month DESC, day DESC
    """)
    fun getFilteredExpenses(
        years: List<Int>?,
        months: List<Int>?,
        categories: List<String>?,
        source: String?
    ): Flow<List<ExpenseData>>

    @Query("""
        SELECT * FROM expenses 
        ORDER BY year DESC, month DESC, day DESC 
        LIMIT 50
    """)
    fun getRecentExpenses(): Flow<List<ExpenseData>>

    @Query("""
        SELECT DISTINCT category, description, COUNT(*) as count
        FROM expenses
        WHERE category != 'Uncategorized'
        GROUP BY description, category
        HAVING count > 0
        ORDER BY count DESC
    """)
    suspend fun getCategoryPatterns(): List<CategoryPattern>

    @Query("""
        SELECT category
        FROM expenses
        WHERE description LIKE :pattern
        AND category != 'Uncategorized'
        GROUP BY category
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getMostFrequentCategoryForPattern(pattern: String): String?

}

