package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TextButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxSettingsDialog(
    currentSalarySettings: TaxSettings,
    currentHourlySettings: TaxSettings,
    onDismiss: () -> Unit,
    onConfirm: (salary: TaxSettings, hourly: TaxSettings) -> Unit
) {
    var selectedType by remember { mutableStateOf(0) } // 0 for salary, 1 for hourly
    var salarySettings by remember { mutableStateOf(currentSalarySettings) }
    var hourlySettings by remember { mutableStateOf(currentHourlySettings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tax Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type selector
                TabRow(selectedTabIndex = selectedType) {
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

                val currentSettings = if (selectedType == 0) salarySettings else hourlySettings
                val updateSettings = { newSettings: TaxSettings ->
                    if (selectedType == 0) {
                        salarySettings = newSettings
                    } else {
                        hourlySettings = newSettings
                    }
                }

                // Federal Tax Section with configurable rate
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Federal Tax")
                        Switch(
                            checked = currentSettings.federalWithholding,
                            onCheckedChange = { enabled ->
                                updateSettings(currentSettings.copy(federalWithholding = enabled))
                            }
                        )
                    }
                    if (currentSettings.federalWithholding) {
                        OutlinedTextField(
                            value = currentSettings.federalTaxRate.toString(),
                            onValueChange = { value ->
                                val rate = value.toDoubleOrNull() ?: currentSettings.federalTaxRate
                                updateSettings(currentSettings.copy(federalTaxRate = rate))
                            },
                            label = { Text("Federal Tax Rate") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }

                // State Tax Section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("State Tax")
                        Switch(
                            checked = currentSettings.stateTaxEnabled,
                            onCheckedChange = { enabled ->
                                updateSettings(currentSettings.copy(stateTaxEnabled = enabled))
                            }
                        )
                    }
                    if (currentSettings.stateTaxEnabled) {
                        OutlinedTextField(
                            value = currentSettings.stateWithholdingPercentage.toString(),
                            onValueChange = { value ->
                                val percentage = value.toDoubleOrNull() ?: currentSettings.stateWithholdingPercentage
                                updateSettings(currentSettings.copy(stateWithholdingPercentage = percentage))
                            },
                            label = { Text("State Tax Rate") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }

                // Fixed Rate Taxes
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Standard Deductions",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Medicare
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Medicare (1.45%)")
                            Switch(
                                checked = currentSettings.medicareTaxEnabled,
                                onCheckedChange = { enabled ->
                                    updateSettings(currentSettings.copy(medicareTaxEnabled = enabled))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Social Security
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Social Security (6.2%)")
                                Switch(
                                    checked = currentSettings.socialSecurityTaxEnabled,
                                    onCheckedChange = { enabled ->
                                        updateSettings(currentSettings.copy(socialSecurityTaxEnabled = enabled))
                                    }
                                )
                            }
                            Text(
                                text = "Capped at $147,000 annually",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(salarySettings, hourlySettings) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeductionDialog(
    onDismiss: () -> Unit,
    onConfirm: (deduction: Deduction, isForSalary: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(DeductionFrequency.PER_PAYCHECK) }
    var type by remember { mutableStateOf(DeductionType.OTHER) }
    var taxable by remember { mutableStateOf(false) }
    var isForSalary by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Deduction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Employment Type Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = isForSalary,
                        onClick = { isForSalary = true },
                        label = { Text("Salary") }
                    )
                    FilterChip(
                        selected = !isForSalary,
                        onClick = { isForSalary = false },
                        label = { Text("Hourly") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Deduction Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency Selection
                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                DeductionFrequency.entries.forEach { freq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = frequency == freq,
                                onClick = { frequency = freq }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = frequency == freq,
                            onClick = { frequency = freq }
                        )
                        Text(
                            text = freq.name.lowercase().replace('_', ' '),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Type Selection
                Text("Type", style = MaterialTheme.typography.labelMedium)
                DeductionType.entries.forEach { deductionType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = type == deductionType,
                                onClick = { type = deductionType }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = type == deductionType,
                            onClick = { type = deductionType }
                        )
                        Text(
                            text = deductionType.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Taxable")
                    Switch(
                        checked = taxable,
                        onCheckedChange = { taxable = it }
                    )
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Please enter a deduction name"
                        return@Button
                    }
                    val deductionAmount = amount.toDoubleOrNull()
                    if (deductionAmount == null || deductionAmount <= 0) {
                        error = "Please enter a valid amount"
                        return@Button
                    }

                    onConfirm(
                        Deduction(
                            name = name,
                            amount = deductionAmount,
                            frequency = frequency,
                            type = type,
                            taxable = taxable
                        ),
                        isForSalary
                    )
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySettingsDialog(
    currentSettings: SalarySettings,
    onDismiss: () -> Unit,
    onConfirm: (enabled: Boolean, salary: Double, frequency: PayFrequency) -> Unit
) {
    var annualSalary by remember { mutableStateOf(currentSettings.annualSalary.toString()) }
    var payFrequency by remember { mutableStateOf(currentSettings.payFrequency) }
    var enabled by remember { mutableStateOf(currentSettings.enabled) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salary Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Salary")
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                OutlinedTextField(
                    value = annualSalary,
                    onValueChange = {
                        annualSalary = it
                        error = null
                    },
                    label = { Text("Annual Salary") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled
                )

                Text("Pay Frequency", style = MaterialTheme.typography.labelMedium)
                PayFrequency.entries.forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = payFrequency == frequency,
                                onClick = { payFrequency = frequency }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = payFrequency == frequency,
                            onClick = { payFrequency = frequency }
                        )
                        Text(
                            text = frequency.name.lowercase().replace('_', ' '),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val salary = annualSalary.toDoubleOrNull()
                    if (salary == null && enabled) {
                        error = "Please enter a valid salary"
                        return@Button
                    }
                    onConfirm(enabled, salary ?: 0.0, payFrequency)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HourlySettingsDialog(
    currentSettings: HourlySettings,
    onDismiss: () -> Unit,
    onConfirm: (HourlySettings) -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hourly Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Hourly Pay")
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { enabled ->
                            settings = settings.copy(enabled = enabled)
                        }
                    )
                }

                OutlinedTextField(
                    value = settings.baseRate.toString(),
                    onValueChange = {
                        settings = settings.copy(baseRate = it.toDoubleOrNull() ?: 0.0)
                        error = null
                    },
                    label = { Text("Base Hourly Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.enabled
                )

                OutlinedTextField(
                    value = settings.weekendRate.toString(),
                    onValueChange = {
                        settings = settings.copy(weekendRate = it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Weekend Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.enabled
                )

                OutlinedTextField(
                    value = settings.nightDifferential.toString(),
                    onValueChange = {
                        settings = settings.copy(nightDifferential = it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Night Differential") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.enabled
                )

                Text("Night Shift Hours", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = settings.nightShiftStart.toString(),
                        onValueChange = {
                            val value = it.toIntOrNull() ?: 0
                            if (value in 0..23) {
                                settings = settings.copy(nightShiftStart = value)
                            }
                        },
                        label = { Text("Start Hour (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = settings.enabled
                    )
                    OutlinedTextField(
                        value = settings.nightShiftEnd.toString(),
                        onValueChange = {
                            val value = it.toIntOrNull() ?: 0
                            if (value in 0..23) {
                                settings = settings.copy(nightShiftEnd = value)
                            }
                        },
                        label = { Text("End Hour (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = settings.enabled
                    )
                }

                Text("Pay Frequency", style = MaterialTheme.typography.labelMedium)
                PayFrequency.entries.forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = settings.payFrequency == frequency,
                                onClick = {
                                    settings = settings.copy(payFrequency = frequency)
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.payFrequency == frequency,
                            onClick = {
                                settings = settings.copy(payFrequency = frequency)
                            }
                        )
                        Text(
                            text = frequency.name.lowercase().replace('_', ' '),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (settings.enabled && settings.baseRate <= 0) {
                        error = "Please enter a valid base rate"
                        return@Button
                    }
                    onConfirm(settings)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
