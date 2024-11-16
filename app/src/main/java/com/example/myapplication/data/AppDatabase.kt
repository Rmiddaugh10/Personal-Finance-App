package com.example.myapplication.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.converter.DateConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        return value?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }
    }

    @TypeConverter
    fun toIntList(list: List<Int>?): String? {
        return list?.joinToString(",")
    }
}

@Database(
    entities = [
        ExpenseData::class,
        BudgetCategory::class,
        MonthlyBudget::class,
        BudgetComparison::class,
        PinnedCategory::class,
        WalletBalanceEntity::class,
        WorkShiftEntity::class,
        PayPeriodEntity::class,
        PayRateEntity::class,
        PaymentCalculationEntity::class,
        PaymentDeductionEntity::class,
        SalaryTaxSettingsEntity::class,
        HourlyTaxSettingsEntity::class,
        SalaryDeductionEntity::class,
        HourlyDeductionEntity::class
    ],
    version = 14,  // Increment version number
    exportSchema = false
)
@TypeConverters(DateConverter::class, Converters::class, WalletTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun budgetComparisonDao(): BudgetComparisonDao
    abstract fun pinnedCategoryDao(): PinnedCategoryDao
    abstract fun walletDao(): WalletDao
    abstract fun shiftDao(): ShiftDao
    abstract fun payPeriodDao(): PayPeriodDao
    abstract fun paymentCalculationDao(): PaymentCalculationDao
    abstract fun salaryTaxSettingsDao(): SalaryTaxSettingsDao
    abstract fun hourlyTaxSettingsDao(): HourlyTaxSettingsDao
    abstract fun salaryDeductionsDao(): SalaryDeductionsDao
    abstract fun hourlyDeductionsDao(): HourlyDeductionsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Existing migration logic
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Existing migration logic
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `source` TEXT
                    )
                """
                )
                database.execSQL(
                    """
                    INSERT INTO expenses_temp (
                        id, amount, category, description, 
                        year, month, day, source
                    )
                    SELECT 
                        id, amount, category, description,
                        year, month, day, 
                        CASE 
                            WHEN source IS NULL THEN NULL 
                            ELSE source 
                        END as source
                    FROM expenses
                """
                )
                database.execSQL("DROP TABLE expenses")
                database.execSQL("ALTER TABLE expenses_temp RENAME TO expenses")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        day INTEGER NOT NULL,
                        source TEXT
                    )
                """
                )
                database.execSQL(
                    """
                    INSERT INTO expenses_temp 
                    SELECT id, amount, category, description, year, month, day, source 
                    FROM expenses
                """
                )
                database.execSQL("DROP TABLE expenses")
                database.execSQL("ALTER TABLE expenses_temp RENAME TO expenses")
                database.execSQL(
                    """
                    UPDATE budget_comparison 
                    SET actualAmount = ABS(actualAmount),
                        budgetedAmount = ABS(budgetedAmount),
                        difference = budgetedAmount - actualAmount,
                        percentageUsed = CASE 
                            WHEN budgetedAmount != 0 
                            THEN (ABS(actualAmount) / ABS(budgetedAmount)) * 100
                            ELSE CASE 
                                WHEN actualAmount != 0 THEN 100
                                ELSE 0
                            END
                        END
                    WHERE 1=1
                """
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pinned_categories` (
                        `categoryName` TEXT NOT NULL PRIMARY KEY
                    )
                """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wallet_balance` (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cashAmount REAL NOT NULL,
                        lastUpdated TEXT NOT NULL
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `work_shifts` (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        employeeId TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        breakDurationMinutes INTEGER NOT NULL,
                        isWeekend INTEGER NOT NULL,
                        isNightShift INTEGER NOT NULL,
                        actualHoursWorked REAL,
                        status TEXT NOT NULL
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pay_periods` (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        employeeId TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        payDate INTEGER NOT NULL,
                        isSalary INTEGER NOT NULL,
                        baseRate REAL
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pay_rates` (
                        employeeId TEXT PRIMARY KEY NOT NULL,
                        baseRate REAL NOT NULL,
                        weekendRate REAL NOT NULL,
                        nightDifferential REAL NOT NULL,
                        overtimeMultiplier REAL NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS payment_calculations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        payPeriodId INTEGER NOT NULL,
                        regularHours REAL NOT NULL DEFAULT 0.0,
                        overtimeHours REAL NOT NULL DEFAULT 0.0,
                        weekendHours REAL NOT NULL DEFAULT 0.0,
                        nightHours REAL NOT NULL DEFAULT 0.0,
                        grossAmount REAL NOT NULL,
                        netAmount REAL NOT NULL,
                        calculatedAt TEXT NOT NULL,
                        FOREIGN KEY(payPeriodId) REFERENCES pay_periods(id) 
                        ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS payment_deductions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        paymentCalculationId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        FOREIGN KEY(paymentCalculationId) 
                        REFERENCES payment_calculations(id) 
                        ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_payment_calculations_pay_period_id ON payment_calculations(payPeriodId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_payment_deductions_calculation_id ON payment_deductions(paymentCalculationId)"
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // First, handle the existing tables from your current migration
                database.execSQL("ALTER TABLE pay_periods ADD COLUMN salaryEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pay_periods ADD COLUMN hourlyEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pay_periods ADD COLUMN annualSalary REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE pay_rates ADD COLUMN nightShiftStart INTEGER NOT NULL DEFAULT 18")
                database.execSQL("ALTER TABLE pay_rates ADD COLUMN nightShiftEnd INTEGER NOT NULL DEFAULT 6")

                // Create tables for salary tax settings
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS salary_tax_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                federalWithholding INTEGER NOT NULL DEFAULT 0,
                stateTaxEnabled INTEGER NOT NULL DEFAULT 0,
                stateWithholdingPercentage REAL NOT NULL DEFAULT 0.0,
                cityTaxEnabled INTEGER NOT NULL DEFAULT 0,
                cityWithholdingPercentage REAL NOT NULL DEFAULT 0.0,
                medicareTaxEnabled INTEGER NOT NULL DEFAULT 0,
                socialSecurityTaxEnabled INTEGER NOT NULL DEFAULT 0
            )
        """
                )

                // Create tables for hourly tax settings
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS hourly_tax_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                federalWithholding INTEGER NOT NULL DEFAULT 0,
                stateTaxEnabled INTEGER NOT NULL DEFAULT 0,
                stateWithholdingPercentage REAL NOT NULL DEFAULT 0.0,
                cityTaxEnabled INTEGER NOT NULL DEFAULT 0,
                cityWithholdingPercentage REAL NOT NULL DEFAULT 0.0,
                medicareTaxEnabled INTEGER NOT NULL DEFAULT 0,
                socialSecurityTaxEnabled INTEGER NOT NULL DEFAULT 0
            )
        """
                )

                // Create tables for salary deductions
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS salary_deductions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                frequency TEXT NOT NULL,
                type TEXT NOT NULL,
                taxable INTEGER NOT NULL DEFAULT 0
            )
        """
                )

                // Create tables for hourly deductions
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS hourly_deductions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                frequency TEXT NOT NULL,
                type TEXT NOT NULL,
                taxable INTEGER NOT NULL DEFAULT 0
            )
        """
                )

                // Recreate payment calculations table with the correct schema
                database.execSQL("DROP TABLE IF EXISTS payment_calculations")
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS payment_calculations (
                id INTEGER PRIMARY KEY NOT NULL,
                payPeriodId INTEGER NOT NULL,
                regularHours REAL NOT NULL,
                overtimeHours REAL NOT NULL,
                weekendHours REAL NOT NULL,
                nightHours REAL NOT NULL,
                grossAmount REAL NOT NULL,
                netAmount REAL NOT NULL,
                calculatedAt TEXT NOT NULL,
                FOREIGN KEY (payPeriodId) REFERENCES pay_periods(id) ON DELETE CASCADE
            )
        """
                )

                // Recreate payment deductions table with the correct schema
                database.execSQL("DROP TABLE IF EXISTS payment_deductions")
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS payment_deductions (
                id INTEGER PRIMARY KEY NOT NULL,
                paymentCalculationId INTEGER NOT NULL,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                FOREIGN KEY (paymentCalculationId) REFERENCES payment_calculations(id) ON DELETE CASCADE
            )
        """
                )

                // Create indices for better performance
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_payment_calculations_payPeriodId ON payment_calculations(payPeriodId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_payment_deductions_calculation_id ON payment_deductions(paymentCalculationId)"
                )
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop existing tables if necessary
                database.execSQL("DROP TABLE IF EXISTS salary_tax_settings")
                database.execSQL("DROP TABLE IF EXISTS hourly_tax_settings")
                database.execSQL("DROP TABLE IF EXISTS salary_deductions")
                database.execSQL("DROP TABLE IF EXISTS hourly_deductions")

                // Recreate the salary_tax_settings table
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS salary_tax_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                federalWithholding INTEGER NOT NULL,
                stateTaxEnabled INTEGER NOT NULL,
                stateWithholdingPercentage REAL NOT NULL,
                cityTaxEnabled INTEGER NOT NULL,
                cityWithholdingPercentage REAL NOT NULL,
                medicareTaxEnabled INTEGER NOT NULL,
                socialSecurityTaxEnabled INTEGER NOT NULL
            )
        """
                )

                // Recreate the hourly_tax_settings table
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS hourly_tax_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                federalWithholding INTEGER NOT NULL,
                stateTaxEnabled INTEGER NOT NULL,
                stateWithholdingPercentage REAL NOT NULL,
                cityTaxEnabled INTEGER NOT NULL,
                cityWithholdingPercentage REAL NOT NULL,
                medicareTaxEnabled INTEGER NOT NULL,
                socialSecurityTaxEnabled INTEGER NOT NULL
            )
        """
                )

                // Recreate the salary_deductions table
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS salary_deductions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                frequency TEXT NOT NULL,
                type TEXT NOT NULL,
                taxable INTEGER NOT NULL
            )
        """
                )

                // Recreate the hourly_deductions table
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS hourly_deductions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                frequency TEXT NOT NULL,
                type TEXT NOT NULL,
                taxable INTEGER NOT NULL
            )
        """
                )
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop existing tables
                database.execSQL("DROP TABLE IF EXISTS salary_tax_settings")
                database.execSQL("DROP TABLE IF EXISTS hourly_tax_settings")
                database.execSQL("DROP TABLE IF EXISTS salary_deductions")
                database.execSQL("DROP TABLE IF EXISTS hourly_deductions")

                // Recreate the salary_tax_settings table with new federalTaxRate column
                database.execSQL(
                    """
    CREATE TABLE IF NOT EXISTS salary_tax_settings (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        federalWithholding INTEGER NOT NULL,
        federalTaxRate REAL NOT NULL DEFAULT 22.0,
        stateTaxEnabled INTEGER NOT NULL,
        stateWithholdingPercentage REAL NOT NULL,
        cityTaxEnabled INTEGER NOT NULL,
        cityWithholdingPercentage REAL NOT NULL,
        medicareTaxEnabled INTEGER NOT NULL,
        socialSecurityTaxEnabled INTEGER NOT NULL
    )
"""
                )

                // Recreate the hourly_tax_settings table with new federalTaxRate column
                database.execSQL(
                    """
    CREATE TABLE IF NOT EXISTS hourly_tax_settings (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        federalWithholding INTEGER NOT NULL,
        federalTaxRate REAL NOT NULL DEFAULT 22.0,
        stateTaxEnabled INTEGER NOT NULL,
        stateWithholdingPercentage REAL NOT NULL,
        cityTaxEnabled INTEGER NOT NULL,
        cityWithholdingPercentage REAL NOT NULL,
        medicareTaxEnabled INTEGER NOT NULL,
        socialSecurityTaxEnabled INTEGER NOT NULL
    )
"""
                )

                // Recreate the salary_deductions table (unchanged)
                database.execSQL(
                    """
    CREATE TABLE IF NOT EXISTS salary_deductions (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        amount REAL NOT NULL,
        frequency TEXT NOT NULL,
        type TEXT NOT NULL,
        taxable INTEGER NOT NULL
    )
"""
                )

                // Recreate the hourly_deductions table (unchanged)
                database.execSQL(
                    """
    CREATE TABLE IF NOT EXISTS hourly_deductions (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        amount REAL NOT NULL,
        frequency TEXT NOT NULL,
        type TEXT NOT NULL,
        taxable INTEGER NOT NULL
    )
"""
                )
            }
        }
    }
}

