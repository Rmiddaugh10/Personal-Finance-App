package com.example.myapplication.utils

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.myapplication.data.ExpenseData
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import com.opencsv.ICSVParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

object CsvParserUtility {
    private const val TAG = "CsvParserUtility"

    private data class FileInspectionResult(
        val separator: Char,
        val hasQuotes: Boolean,
        val firstFewLines: List<String>,
        val approximateLineCount: Int
    )

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy")
    )

    fun parseCsvFile(uri: Uri, contentResolver: ContentResolver): List<ExpenseData> {
        // First inspect the file
        val fileInspection = inspectFileContent(uri, contentResolver)

        // Determine source based on both filename and content
        val source = determineSource(uri, fileInspection.firstFewLines)
        Log.d(TAG, "Parsing CSV file from source: $source (File: ${uri.lastPathSegment})")

        return try {
            when (source) {
                "Bank of America" -> parseBoACsv(uri, contentResolver, source)
                "Community America" -> parseCommunityAmericaCsv(uri, contentResolver, fileInspection, source)
                else -> {
                    Log.e(TAG, "Unknown file format. First few lines: ${fileInspection.firstFewLines}")
                    emptyList()
                }
            }.also { expenses ->
                Log.d(TAG, "Successfully parsed ${expenses.size} transactions from $source")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CSV file", e)
            emptyList()
        }
    }

    private fun inspectFileContent(uri: Uri, contentResolver: ContentResolver): FileInspectionResult {
        var separator = ','
        var hasQuotes = false
        val firstFewLines = mutableListOf<String>()
        var lineCount = 0

        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = null

            while (reader.readLine()?.also { line = it } != null && firstFewLines.size < 10) {
                line?.let {
                    if (firstFewLines.size < 5) {
                        firstFewLines.add(it)
                    }
                    if (it.contains("|")) separator = '|'
                    if (it.contains("\"")) hasQuotes = true
                    lineCount++
                }
            }

            while (reader.readLine() != null) {
                lineCount++
            }
        }

        return FileInspectionResult(separator, hasQuotes, firstFewLines, lineCount)
    }

    private fun determineSource(uri: Uri, firstFewLines: List<String>): String {
        val fileName = uri.lastPathSegment?.lowercase(Locale.US) ?: ""
        return when {
            fileName.contains("stmt") ||
                    firstFewLines.any { it.contains("Bank of America", ignoreCase = true) } ||
                    firstFewLines.any { it.contains("Date|Description|Amount|Running Bal.", ignoreCase = true) } -> "Bank of America"
            fileName.contains("export") ||
                    firstFewLines.any { it.contains("Cashback Free Checking", ignoreCase = true) } -> "Community America"
            else -> "Unknown"
        }
    }

    private fun parseAmount(amountStr: String): Double? {
        return try {
            val cleanAmount = amountStr
                .replace("$", "")
                .replace(",", "")
                .replace("\"", "") // Add this line to remove quotes
                .trim()

            if (cleanAmount.isEmpty()) {
                Log.d(TAG, "Empty amount string")
                null
            } else {
                BigDecimal(cleanAmount).toDouble().also {
                    Log.d(TAG, "Successfully parsed amount '$amountStr' to $it")
                }
            }
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Could not parse amount: '$amountStr'", e)
            null
        }
    }

    private fun parseDate(dateStr: String): LocalDate? {
        val cleanDateStr = dateStr.replace("\"", "").trim()

        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleanDateStr, formatter).also {
                    Log.d(TAG, "Successfully parsed date '$dateStr' to $it")
                }
            } catch (_: DateTimeParseException) {
                continue
            }
        }
        Log.e(TAG, "Could not parse date: '$dateStr'")
        return null
    }

        private fun createCsvReader(reader: BufferedReader, fileInspection: FileInspectionResult): CSVReader {
            val parser = CSVParserBuilder()
                .withSeparator(fileInspection.separator)
                .withQuoteChar(if (fileInspection.hasQuotes) '\"' else ICSVParser.NULL_CHARACTER)
                .withIgnoreQuotations(!fileInspection.hasQuotes)
                .build()

            return CSVReaderBuilder(reader)
                .withCSVParser(parser)
                .build()
        }


    private fun parseCommunityAmericaCsv(
        uri: Uri,
        contentResolver: ContentResolver,
        fileInspection: FileInspectionResult,
        source: String
    ): List<ExpenseData> {
        val expenses = mutableListOf<ExpenseData>()

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val csvReader = createCsvReader(reader, fileInspection)
                var headerFound = false
                var currentLine = 0

                var row: Array<String>?
                while (csvReader.readNext().also { row = it } != null) {
                    currentLine++
                    try {
                        row?.let { currentRow ->
                            // Debug print every row
                            Log.d(TAG, "Line $currentLine: ${currentRow.joinToString("|")}")

                            // Skip the first 3 lines (account info)
                            if (currentLine <= 3) {
                                Log.d(TAG, "Skipping header line $currentLine")
                                return@let
                            }

                            // Process the actual header row (line 4)
                            if (currentLine == 4) {
                                Log.d(TAG, "Processing header row: ${currentRow.joinToString("|")}")
                                headerFound = true
                                return@let
                            }

                            // Process data rows (after line 4)
                            if (headerFound && currentRow.size >= 6) {
                                try {
                                    // Transaction line should start with a number
                                    if (!currentRow[0].trim().matches(Regex("^\\d+$"))) {
                                        Log.d(TAG, "Skipping non-transaction row: ${currentRow[0]}")
                                        return@let
                                    }

                                    val dateStr = currentRow[1].trim()
                                    Log.d(TAG, "Parsing date: $dateStr")

                                    val date = parseDate(dateStr)
                                    if (date == null) {
                                        Log.d(TAG, "Failed to parse date: $dateStr")
                                        return@let
                                    }

                                    val description = buildString {
                                        append(currentRow[2].trim())
                                        if (currentRow[3].isNotBlank()) {
                                            append(" - ")
                                            append(currentRow[3].trim())
                                        }
                                    }

                                    val debitStr = currentRow[4].trim()
                                    val creditStr = currentRow[5].trim()

                                    Log.d(TAG, "Processing amounts - Debit: '$debitStr', Credit: '$creditStr'")

                                    val amount = when {
                                        debitStr.isNotBlank() -> {
                                            val parsed = parseAmount(debitStr)
                                            Log.d(TAG, "Parsed debit amount: $parsed")
                                            -abs(parsed ?: return@let)
                                        }
                                        creditStr.isNotBlank() -> {
                                            val parsed = parseAmount(creditStr)
                                            Log.d(TAG, "Parsed credit amount: $parsed")
                                            abs(parsed ?: return@let)
                                        }
                                        else -> {
                                            Log.d(TAG, "No valid amount found")
                                            return@let
                                        }
                                    }

                                    expenses.add(ExpenseData(
                                        year = date.year,
                                        month = date.monthValue,
                                        day = date.dayOfMonth,
                                        category = "Uncategorized",
                                        amount = amount,
                                        description = description,
                                        source = source
                                    ))

                                    Log.d(TAG, "Added transaction - Date: ${date}, Amount: $amount, Description: $description")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing transaction row: ${currentRow.joinToString("|")}", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing row: ${row?.joinToString("|")}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Community America CSV file", e)
        }

        Log.d(TAG, "Parsed ${expenses.size} Community America transactions")
        return expenses
    }

        private fun parseBoACsv(uri: Uri, contentResolver: ContentResolver, source: String): List<ExpenseData> {
            val expenses = mutableListOf<ExpenseData>()

            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val csvReader = CSVReader(reader)
                    var row: Array<String>?
                    var foundHeader = false

                    while (csvReader.readNext().also { row = it } != null) {
                        try {
                            row?.let {
                                if (!foundHeader) {
                                    if (it.joinToString("|").contains("Date|Description|Amount|Running Bal.")) {
                                        foundHeader = true
                                        return@let
                                    }
                                    return@let
                                }

                                if (it.joinToString().contains("Beginning balance")) return@let
                                if (it.size < 3) return@let

                                val date = parseDate(it[0]) ?: return@let
                                val description = it[1].trim()
                                val amountStr = it[2].trim()

                                if (amountStr.isEmpty()) return@let

                                val amount = parseAmount(amountStr) ?: return@let

                                expenses.add(ExpenseData(
                                    year = date.year,
                                    month = date.monthValue,
                                    day = date.dayOfMonth,
                                    category = "Uncategorized",
                                    amount = amount,
                                    description = description,
                                    source = source
                                ))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing BoA row: ${row?.joinToString("|")}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading BoA CSV file")
            }

            Log.d(TAG, "Parsed ${expenses.size} Bank of America transactions")
            return expenses
        }

        private fun parseGenericCsv(uri: Uri, contentResolver: ContentResolver, source: String): List<ExpenseData> {
            val expenses = mutableListOf<ExpenseData>()

            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val csvReader = CSVReader(reader)
                    csvReader.readNext()
                    var row: Array<String>?

                    while (csvReader.readNext().also { row = it } != null) {
                        try {
                            row?.let {
                                if (it.size < 3) return@let

                                val date = parseDate(it[0]) ?: return@let
                                val description = it[1].trim()
                                val amount = parseAmount(it[2]) ?: return@let

                                expenses.add(ExpenseData(
                                    year = date.year,
                                    month = date.monthValue,
                                    day = date.dayOfMonth,
                                    category = "Uncategorized",
                                    amount = amount,
                                    description = description,
                                    source = source
                                ))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing generic CSV row: ${row?.joinToString(",")}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading generic CSV file")
            }

            Log.d(TAG, "Parsed ${expenses.size} generic format transactions")
            return expenses
        }


}
