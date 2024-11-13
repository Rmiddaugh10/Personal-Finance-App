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
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.*
import com.example.myapplication.ui.components.DeductionDialog
import com.example.myapplication.ui.components.PayRateDialog
import com.example.myapplication.ui.components.TaxSettingsDialog
import com.example.myapplication.viewmodels.WalletViewModel
import com.example.myapplication.viewmodels.WalletViewModelFactory
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val CALENDAR_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")

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

    Column(modifier = Modifier.fillMaxSize()) {
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

        when (selectedTab) {
            0 -> CashWalletSection(viewModel)
            1 -> PaychecksSection(viewModel)
            2 -> ScheduleSection(viewModel)
            3 -> PaySettingsSection(viewModel)
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
    val payments by viewModel.payments.collectAsState(initial = emptyList())
    val totalAmount = remember(payments) {
        payments.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Upcoming Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                payments.forEachIndexed { index, payment ->
                    PaymentRow(payment = payment)
                    if (index < payments.lastIndex) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Monthly Total",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = NumberFormat.getCurrencyInstance().format(totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: PaymentInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = payment.employeeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (payment.isEstimate) "Estimated" else "Confirmed",
                style = MaterialTheme.typography.bodySmall,
                color = if (payment.isEstimate)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.primary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = NumberFormat.getCurrencyInstance().format(payment.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = payment.date.format(DateTimeFormatter.ofPattern("MMM dd")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScheduleSection(viewModel: WalletViewModel) {
    val shifts by viewModel.shifts.collectAsState()
    var showAddShiftDialog by remember { mutableStateOf(false) }
    var selectedShift by remember { mutableStateOf<WorkShift?>(null) }

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
                .padding(bottom = 80.dp) // Add padding for bottom nav
        ) {
            items(
                items = shifts.groupBy { it.date }.toList(),
                key = { (date, _) -> date.toString() }
            ) { (date, dayShifts) ->
                DayShiftsCard(
                    date = date,
                    shifts = dayShifts,
                    onShiftClick = { selectedShift = it }
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
                            breakDuration = java.time.Duration.ofMinutes(breakLength)
                        )
                    )
                    selectedShift = null
                }
            )
        }
    }
}

@Composable
private fun PaySettingsSection(viewModel: WalletViewModel) {
    var showTaxDialog by remember { mutableStateOf(false) }
    var showPayRateDialog by remember { mutableStateOf(false) }
    var showDeductionDialog by remember { mutableStateOf(false) }
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

        EmploymentType.entries.forEach { type ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showPayRateDialog = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.name.lowercase().capitalize(Locale.current),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = settings.employmentType == type,
                            onCheckedChange = { viewModel.updateEmploymentType(type) }
                        )
                    }

                    if (settings.employmentType == type) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (type == EmploymentType.SALARY) {
                            Text(
                                text = "Annual Salary: $${settings.payRates.basePay}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Paid ${settings.payRates.payFrequency.name.lowercase()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "Hourly Rate: $${settings.payRates.basePay}/hr",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (settings.payRates.weekendRate > 0) {
                                Text("Weekend Rate: $${settings.payRates.weekendRate}/hr")
                            }
                            if (settings.payRates.nightDifferential > 0) {
                                Text("Night Differential: +$${settings.payRates.nightDifferential}/hr")
                            }
                        }
                    }
                }
            }
        }

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
                Column {
                    Text("Federal Withholding: ${if (settings.taxSettings.federalWithholding) "Enabled" else "Disabled"}")
                    Text("State Tax: ${if (settings.taxSettings.stateTaxEnabled) "${settings.taxSettings.stateWithholdingPercentage}%" else "Disabled"}")
                    Text("City Tax: ${if (settings.taxSettings.cityTaxEnabled) "${settings.taxSettings.cityWithholdingPercentage}%" else "Disabled"}")
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
                settings.deductions.forEach { deduction ->
                    DeductionItem(
                        deduction = deduction,
                        onRemove = { viewModel.removeDeduction(it) }
                    )
                }
            }
        }

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

        if (showPayRateDialog) {
            PayRateDialog(
                currentRates = settings.payRates,
                employmentType = settings.employmentType,
                onDismiss = { showPayRateDialog = false },
                onConfirm = {
                    viewModel.updatePayRates(it)
                    showPayRateDialog = false
                }
            )
        }

        if (showTaxDialog) {
            TaxSettingsDialog(
                currentSettings = settings.taxSettings,
                onDismiss = { showTaxDialog = false },
                onConfirm = {
                    viewModel.updateTaxSettings(it)
                    showTaxDialog = false
                }
            )
        }
        if (showDeductionDialog) {
            DeductionDialog(
                onDismiss = { showDeductionDialog = false },
                onConfirm = {
                    viewModel.addDeduction(it)
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
private fun PaycheckCard(paycheck: PaycheckCalculation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gross Pay",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = NumberFormat.getCurrencyInstance().format(paycheck.grossPay),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column {
                HourRow("Regular Hours", paycheck.regularHours)
                HourRow("Overtime Hours", paycheck.overtimeHours)
                HourRow("Weekend Hours", paycheck.weekendHours)
                HourRow("Night Hours", paycheck.nightHours)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column {
                paycheck.deductions.forEach { (name, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name)
                        Text(
                            "-${NumberFormat.getCurrencyInstance().format(amount)}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Net Pay",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = NumberFormat.getCurrencyInstance().format(paycheck.netPay),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun HourRow(label: String, hours: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text("${String.format("%.2f", hours)} hrs")
    }
}

private fun calculateShiftHours(shift: WorkShift): Double {
    val duration = java.time.Duration.between(shift.startTime, shift.endTime)
    return (duration.toMinutes() - shift.breakDuration.toMinutes()) / 60.0
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
    onShiftClick: (WorkShift) -> Unit
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShiftClick(shift) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.1f hrs", calculateShiftHours(shift)), // Format to 1 decimal place
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${shift.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - " +
                                    shift.endTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
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
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
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
                        .width(360.dp)  // Fixed width
                        .height(400.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.fillMaxSize(),
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

