package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
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
                modifier = Modifier.fillMaxWidth(),
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

                // Settings for selected type
                val currentSettings = if (selectedType == 0) salarySettings else hourlySettings
                val updateSettings = { newSettings: TaxSettings ->
                    if (selectedType == 0) {
                        salarySettings = newSettings
                    } else {
                        hourlySettings = newSettings
                    }
                }

                // Federal Tax
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Federal Withholding")
                    Switch(
                        checked = currentSettings.federalWithholding,
                        onCheckedChange = {
                            updateSettings(currentSettings.copy(federalWithholding = it))
                        }
                    )
                }

                // State Tax
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("State Tax")
                    Switch(
                        checked = currentSettings.stateTaxEnabled,
                        onCheckedChange = {
                            updateSettings(currentSettings.copy(stateTaxEnabled = it))
                        }
                    )
                }

                if (currentSettings.stateTaxEnabled) {
                    OutlinedTextField(
                        value = currentSettings.stateWithholdingPercentage.toString(),
                        onValueChange = {
                            updateSettings(currentSettings.copy(
                                stateWithholdingPercentage = it.toDoubleOrNull() ?: 0.0
                            ))
                        },
                        label = { Text("State Tax Percentage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // City Tax
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("City Tax")
                    Switch(
                        checked = currentSettings.cityTaxEnabled,
                        onCheckedChange = {
                            updateSettings(currentSettings.copy(cityTaxEnabled = it))
                        }
                    )
                }

                if (currentSettings.cityTaxEnabled) {
                    OutlinedTextField(
                        value = currentSettings.cityWithholdingPercentage.toString(),
                        onValueChange = {
                            updateSettings(currentSettings.copy(
                                cityWithholdingPercentage = it.toDoubleOrNull() ?: 0.0
                            ))
                        },
                        label = { Text("City Tax Percentage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(salarySettings, hourlySettings)
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
