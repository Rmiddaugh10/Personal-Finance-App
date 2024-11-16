package com.example.myapplication.ui.screens

//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.*
import com.example.myapplication.ui.components.DeductionDialog
import com.example.myapplication.ui.components.HourlySettingsDialog
import com.example.myapplication.ui.components.SalarySettingsDialog
import com.example.myapplication.ui.components.TaxSettingsDialog
import com.example.myapplication.viewmodels.WalletViewModel
import com.example.myapplication.viewmodels.WalletViewModelFactory
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    repository: ExpenseRepository,
    viewModel: WalletViewModel = viewModel(factory = WalletViewModelFactory(repository))
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cash Wallet", "Paychecks", "Schedule", "Settings")
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier
            .weight(1f)
            .padding(bottom = 55.dp) // Adding padding to avoid being cut off by the navigation bar
        ) {
            when (selectedTab) {
                0 -> CashWalletSection(viewModel)
                1 -> PaychecksSection(viewModel)
                2 -> ScheduleSection(viewModel)
                3 -> PaySettingsSection(viewModel)
            }
        }
    }
}



@Composable
private fun CashWalletSection(viewModel: WalletViewModel) {
    val walletBalance by viewModel.walletBalance.collectAsState()
    val showDialog by viewModel.showUpdateBalanceDialog.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cash Balance",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = NumberFormat.getCurrencyInstance()
                        .format(walletBalance?.cashAmount ?: 0.0),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                walletBalance?.let { balance ->
                    Text(
                        text = "Last updated: ${
                            DateTimeFormatter
                                .ofPattern("MMM dd, yyyy HH:mm")
                                .format(balance.lastUpdated)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.showUpdateBalanceDialog() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Balance")
        }

        if (showDialog) {
            UpdateWalletBalanceDialog(
                initialBalance = walletBalance?.cashAmount ?: 0.0,
                onUpdate = { newAmount ->
                    viewModel.updateWalletBalance(newAmount)
                    viewModel.hideUpdateBalanceDialog()
                },
                onDismiss = { viewModel.hideUpdateBalanceDialog() }
            )
        }
    }
}

@Composable
private fun PaychecksSection(viewModel: WalletViewModel) {
    val paychecks by viewModel.paychecks.collectAsState()
    val settings by viewModel.paySettings.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Row for switching between Salary and Hourly views
        if (settings.salarySettings.enabled && settings.hourlySettings.enabled) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                ) {
                    Text(
                        text = "Combined",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                ) {
                    Text(
                        text = "Salary",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 }
                ) {
                    Text(
                        text = "Hourly",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pay Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val totalPay = paychecks.sumOf { it.grossPay }
                val totalNetPay = paychecks.sumOf { it.netPay }

                PaySummaryRow("Gross Pay", totalPay)
                PaySummaryRow("Net Pay", totalNetPay)

                if (settings.hourlySettings.enabled) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Hours Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val totalRegularHours = paychecks.sumOf { it.regularHours }
                    val totalOvertimeHours = paychecks.sumOf { it.overtimeHours }
                    val totalWeekendHours = paychecks.sumOf { it.weekendHours }
                    val totalNightHours = paychecks.sumOf { it.nightHours }

                    HoursSummaryRow("Regular Hours", totalRegularHours)
                    HoursSummaryRow("Overtime Hours", totalOvertimeHours)
                    HoursSummaryRow("Weekend Hours", totalWeekendHours)
                    HoursSummaryRow("Night Hours", totalNightHours)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Paychecks List
        Text(
            text = "Upcoming Paychecks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filteredPaychecks = when (selectedTabIndex) {
                1 -> paychecks.filter { settings.salarySettings.enabled }
                2 -> paychecks.filter { settings.hourlySettings.enabled }
                else -> paychecks
            }

            items(filteredPaychecks) { paycheck ->
                PaycheckCard(
                    paycheck = paycheck,
                    showHourlyDetails = settings.hourlySettings.enabled
                )
            }
        }
    }
}
@Composable
private fun PaySummaryRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(
            text = NumberFormat.getCurrencyInstance(Locale.US).format(amount),
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
private fun HoursSummaryRow(label: String, hours: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format("%.1f hrs", hours),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
private fun ScheduleSection(viewModel: WalletViewModel) {
    val shifts by viewModel.shifts.collectAsState()
    var showAddShiftDialog by remember { mutableStateOf(false) }
    var selectedShift by remember { mutableStateOf<WorkShift?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<WorkShift?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Work Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { showAddShiftDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Shift")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn with proper padding for bottom nav
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            items(
                items = shifts.groupBy { it.date }.toList(),
                key = { (date, _) -> date.toString() }
            ) { (date, dayShifts) ->
                DayShiftsCard(
                    date = date,
                    shifts = dayShifts,
                    onShiftClick = { selectedShift = it },
                    onDeleteClick = { showDeleteConfirmation = it }
                )
            }
        }

        // Dialogs
        if (showAddShiftDialog) {
            AddEditShiftDialog(
                shift = null,
                onDismiss = { showAddShiftDialog = false },
                onConfirm = { date, start, end, breakLength ->
                    viewModel.addShift(date, start, end, breakLength)
                    showAddShiftDialog = false
                }
            )
        }

        selectedShift?.let { shift ->
            AddEditShiftDialog(
                shift = shift,
                onDismiss = { selectedShift = null },
                onConfirm = { date, start, end, breakLength ->
                    viewModel.updateShift(
                        shift.copy(
                            date = date,
                            startTime = start,
                            endTime = end,
                            breakDuration = Duration.ofMinutes(breakLength)
                        )
                    )
                    selectedShift = null
                }
            )
        }

        // Delete confirmation dialog
        showDeleteConfirmation?.let { shift ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Confirm Delete") },
                text = { Text("Are you sure you want to delete this shift?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteShift(shift)
                            showDeleteConfirmation = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun calculateShiftHours(shift: WorkShift): Double {
    val duration = Duration.between(shift.startTime, shift.endTime)
    return (duration.toMinutes() - shift.breakDuration.toMinutes()) / 60.0
}

@Composable
private fun PaySettingsSection(viewModel: WalletViewModel) {
    var showTaxDialog by remember { mutableStateOf(false) }
    var showDeductionDialog by remember { mutableStateOf(false) }
    var showSalarySettingsDialog by remember { mutableStateOf(false) }
    var showHourlySettingsDialog by remember { mutableStateOf(false) }

    val settings = viewModel.paySettings.collectAsState().value
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Employment Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Salary Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showSalarySettingsDialog = true }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Salary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = settings.salarySettings.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSalarySettings(
                                enabled = enabled,
                                annualSalary = settings.salarySettings.annualSalary,
                                frequency = settings.salarySettings.payFrequency
                            )
                        }
                    )
                }

                if (settings.salarySettings.enabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Annual Salary: $${NumberFormat.getNumberInstance(Locale.US).format(settings.salarySettings.annualSalary)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Paid ${settings.salarySettings.payFrequency.name.lowercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Hourly Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showHourlySettingsDialog = true }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hourly",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = settings.hourlySettings.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateHourlySettings(
                                enabled = enabled,
                                baseRate = settings.hourlySettings.baseRate,
                                weekendRate = settings.hourlySettings.weekendRate,
                                nightDifferential = settings.hourlySettings.nightDifferential,
                                overtimeMultiplier = settings.hourlySettings.overtimeMultiplier,
                                nightShiftStart = settings.hourlySettings.nightShiftStart,
                                nightShiftEnd = settings.hourlySettings.nightShiftEnd,
                                frequency = settings.hourlySettings.payFrequency
                            )
                        }
                    )
                }

                if (settings.hourlySettings.enabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Base Rate: $${settings.hourlySettings.baseRate}/hr",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (settings.hourlySettings.weekendRate > 0) {
                        Text(
                            text = "Weekend Rate: $${settings.hourlySettings.weekendRate}/hr",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (settings.hourlySettings.nightDifferential > 0) {
                        Text(
                            text = "Night Differential: +$${settings.hourlySettings.nightDifferential}/hr",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Night Hours: ${settings.hourlySettings.nightShiftStart}:00 - ${settings.hourlySettings.nightShiftEnd}:00",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Paid ${settings.hourlySettings.payFrequency.name.lowercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Tax Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showTaxDialog = true }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tax Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                var selectedType by remember { mutableStateOf(0) } // 0 for salary, 1 for hourly

                TabRow(
                    selectedTabIndex = selectedType,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Tab(
                        selected = selectedType == 0,
                        onClick = { selectedType = 0 }
                    ) {
                        Text("Salary")
                    }
                    Tab(
                        selected = selectedType == 1,
                        onClick = { selectedType = 1 }
                    ) {
                        Text("Hourly")
                    }
                }

                // Show tax settings based on selected type
                if (selectedType == 0) {
                    Column {
                        Text("Federal Withholding: ${if (settings.salaryTaxSettings.federalWithholding) "Enabled" else "Disabled"}")
                        Text("State Tax: ${if (settings.salaryTaxSettings.stateTaxEnabled) "${settings.salaryTaxSettings.stateWithholdingPercentage}%" else "Disabled"}")
                        Text("City Tax: ${if (settings.salaryTaxSettings.cityTaxEnabled) "${settings.salaryTaxSettings.cityWithholdingPercentage}%" else "Disabled"}")
                    }
                } else {
                    Column {
                        Text("Federal Withholding: ${if (settings.hourlyTaxSettings.federalWithholding) "Enabled" else "Disabled"}")
                        Text("State Tax: ${if (settings.hourlyTaxSettings.stateTaxEnabled) "${settings.hourlyTaxSettings.stateWithholdingPercentage}%" else "Disabled"}")
                        Text("City Tax: ${if (settings.hourlyTaxSettings.cityTaxEnabled) "${settings.hourlyTaxSettings.cityWithholdingPercentage}%" else "Disabled"}")
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deductions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showDeductionDialog = true }) {
                        Icon(Icons.Default.Add, "Add Deduction")
                    }
                }

                var selectedType by remember { mutableStateOf(0) } // 0 for salary, 1 for hourly

                TabRow(
                    selectedTabIndex = selectedType,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Tab(
                        selected = selectedType == 0,
                        onClick = { selectedType = 0 }
                    ) {
                        Text("Salary")
                    }
                    Tab(
                        selected = selectedType == 1,
                        onClick = { selectedType = 1 }
                    ) {
                        Text("Hourly")
                    }
                }

                // Show deductions based on selected type
                if (selectedType == 0) {
                    if (settings.salaryDeductions.isEmpty()) {
                        Text(
                            "No salary deductions added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        settings.salaryDeductions.forEach { deduction ->
                            DeductionItem(
                                deduction = deduction,
                                onRemove = { viewModel.removeDeduction(it, true) }
                            )
                        }
                    }
                } else {
                    if (settings.hourlyDeductions.isEmpty()) {
                        Text(
                            "No hourly deductions added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        settings.hourlyDeductions.forEach { deduction ->
                            DeductionItem(
                                deduction = deduction,
                                onRemove = { viewModel.removeDeduction(it, false) }
                            )
                        }
                    }
                }
            }
        }

        // Show error if any
        if (error != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error ?: "")
            }
        }

        // Dialogs
        if (showSalarySettingsDialog) {
            SalarySettingsDialog(
                currentSettings = settings.salarySettings,
                onDismiss = { showSalarySettingsDialog = false },
                onConfirm = { enabled, salary, frequency ->
                    viewModel.updateSalarySettings(enabled, salary, frequency)
                    showSalarySettingsDialog = false
                }
            )
        }

        if (showHourlySettingsDialog) {
            HourlySettingsDialog(
                currentSettings = settings.hourlySettings,
                onDismiss = { showHourlySettingsDialog = false },
                onConfirm = { settings ->
                    viewModel.updateHourlySettings(
                        enabled = settings.enabled,
                        baseRate = settings.baseRate,
                        weekendRate = settings.weekendRate,
                        nightDifferential = settings.nightDifferential,
                        overtimeMultiplier = settings.overtimeMultiplier,
                        nightShiftStart = settings.nightShiftStart,
                        nightShiftEnd = settings.nightShiftEnd,
                        frequency = settings.payFrequency
                    )
                    showHourlySettingsDialog = false
                }
            )
        }

        // Update the tax dialog section
        if (showTaxDialog) {
            TaxSettingsDialog(
                currentSalarySettings = settings.salaryTaxSettings,
                currentHourlySettings = settings.hourlyTaxSettings,
                onDismiss = { showTaxDialog = false },
                onConfirm = { salarySettings, hourlySettings ->
                    viewModel.updateTaxSettings(salarySettings, hourlySettings)
                    showTaxDialog = false
                }
            )
        }

// Update the deduction dialog section
        if (showDeductionDialog) {
            DeductionDialog(
                onDismiss = { showDeductionDialog = false },
                onConfirm = { deduction, isForSalary ->
                    viewModel.addDeduction(deduction, isForSalary)
                    showDeductionDialog = false
                }
            )
        }
    }
}


@Composable
private fun DeductionItem(
    deduction: Deduction,
    onRemove: (Deduction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(deduction.name)
            Text(
                "$${deduction.amount}/${deduction.frequency.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { onRemove(deduction) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove deduction",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PaycheckCard(
    paycheck: PaycheckCalculation,
    showHourlyDetails: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Paycheck Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaySummaryRow("Gross Pay", paycheck.grossPay)

            if (showHourlyDetails && (paycheck.regularHours > 0 ||
                        paycheck.overtimeHours > 0 ||
                        paycheck.weekendHours > 0 ||
                        paycheck.nightHours > 0)) {

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Hours Breakdown",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (paycheck.regularHours > 0) {
                    HoursSummaryRow("Regular Hours", paycheck.regularHours)
                }
                if (paycheck.overtimeHours > 0) {
                    HoursSummaryRow("Overtime Hours", paycheck.overtimeHours)
                }
                if (paycheck.weekendHours > 0) {
                    HoursSummaryRow("Weekend Hours", paycheck.weekendHours)
                }
                if (paycheck.nightHours > 0) {
                    HoursSummaryRow("Night Hours", paycheck.nightHours)
                }
            }

            if (paycheck.deductions.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Deductions",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))

                paycheck.deductions.forEach { (name, amount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${NumberFormat.getCurrencyInstance(Locale.US).format(amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            PaySummaryRow("Net Pay", paycheck.netPay)
        }
    }
}

@Composable
fun UpdateWalletBalanceDialog(
    initialBalance: Double,
    onUpdate: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(initialBalance.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Cash Balance") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let(onUpdate) ?: run {
                        error = "Please enter a valid amount"
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DayShiftsCard(
    date: LocalDate,
    shifts: List<WorkShift>,
    onShiftClick: (WorkShift) -> Unit,
    onDeleteClick: (WorkShift) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            shifts.forEach { shift ->
                ShiftItem(
                    shift = shift,
                    onClick = { onShiftClick(shift) },
                    onDeleteClick = { onDeleteClick(shift) }
                )
            }
        }
    }
}

@Composable
private fun ShiftItem(
    shift: WorkShift,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = String.format("%.1f hrs", calculateShiftHours(shift)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${shift.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - " +
                        shift.endTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                style = MaterialTheme.typography.bodyMedium
            )
            if (shift.isNightShift) {
                Text(
                    text = "Night Shift",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(
            onClick = onDeleteClick,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete shift"
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditShiftDialog(
    shift: WorkShift?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalTime, LocalTime, Long) -> Unit
) {
    var selectedDate by remember { mutableStateOf(shift?.date ?: LocalDate.now()) }
    var startTime by remember { mutableStateOf(shift?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(shift?.endTime ?: LocalTime.of(17, 0)) }
    var breakDuration by remember { mutableStateOf(shift?.breakDuration?.toMinutes()?.toString() ?: "30") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (shift == null) "Add Shift" else "Edit Shift",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(24.dp))


                // Calendar section with increased width for full visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)  // Small padding to prevent edge touching
                ) {
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        showModeToggle = false
                    )
                }

                LaunchedEffect(datePickerState.selectedDateMillis) {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant
                            .ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Time Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Start Time
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TimePickerButton(
                            time = startTime,
                            onClick = { showStartTimePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // End Time
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TimePickerButton(
                            time = endTime,
                            onClick = { showEndTimePicker = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Break Duration
                OutlinedTextField(
                    value = breakDuration,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            breakDuration = it
                        }
                    },
                    label = { Text("Break Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val duration = breakDuration.toIntOrNull() ?: 30
                            onConfirm(selectedDate, startTime, endTime, duration.toLong())
                        }
                    ) {
                        Text(if (shift == null) "Add" else "Update")
                    }
                }
            }
        }

        // Time Pickers
        if (showStartTimePicker) {
            CustomTimePickerDialog(
                initialTime = startTime,
                onConfirm = { newTime ->
                    startTime = newTime
                    showStartTimePicker = false
                },
                onDismiss = { showStartTimePicker = false }
            )
        }

        if (showEndTimePicker) {
            CustomTimePickerDialog(
                initialTime = endTime,
                onConfirm = { newTime ->
                    endTime = newTime
                    showEndTimePicker = false
                },
                onDismiss = { showEndTimePicker = false }
            )
        }
    }
}



@Composable
private fun TimePickerButton(
    time: LocalTime,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = time.format(DateTimeFormatter.ofPattern("hh:mm a")),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CircularTimePicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = {
                if (value < range.last) onValueChange(value + 1)
            }
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase"
            )
        }

        Text(
            text = "%02d".format(value),
            style = MaterialTheme.typography.headlineMedium
        )

        IconButton(
            onClick = {
                if (value > range.first) onValueChange(value - 1)
            }
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease"
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CustomTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularTimePicker(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    range = 0..23,
                    label = "Hour"
                )

                Text(":")

                CircularTimePicker(
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    range = 0..59,
                    label = "Minute"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(LocalTime.of(selectedHour, selectedMinute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

