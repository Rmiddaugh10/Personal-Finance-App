package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
    currentSettings: TaxSettings,
    onDismiss: () -> Unit,
    onConfirm: (TaxSettings) -> Unit
) {
    var federalWithholding by remember { mutableStateOf(currentSettings.federalWithholding) }
    var stateTaxEnabled by remember { mutableStateOf(currentSettings.stateTaxEnabled) }
    var statePercentage by remember { mutableStateOf(currentSettings.stateWithholdingPercentage.toString()) }
    var cityTaxEnabled by remember { mutableStateOf(currentSettings.cityTaxEnabled) }
    var cityPercentage by remember { mutableStateOf(currentSettings.cityWithholdingPercentage.toString()) }
    var medicareTaxEnabled by remember { mutableStateOf(currentSettings.medicareTaxEnabled) }
    var socialSecurityTaxEnabled by remember { mutableStateOf(currentSettings.socialSecurityTaxEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tax Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Federal Tax Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Federal Withholding")
                    Switch(
                        checked = federalWithholding,
                        onCheckedChange = { federalWithholding = it }
                    )
                }

                // State Tax Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("State Tax")
                    Switch(
                        checked = stateTaxEnabled,
                        onCheckedChange = { stateTaxEnabled = it }
                    )
                }

                if (stateTaxEnabled) {
                    OutlinedTextField(
                        value = statePercentage,
                        onValueChange = { statePercentage = it },
                        label = { Text("State Tax Percentage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // City Tax Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("City Tax")
                    Switch(
                        checked = cityTaxEnabled,
                        onCheckedChange = { cityTaxEnabled = it }
                    )
                }

                if (cityTaxEnabled) {
                    OutlinedTextField(
                        value = cityPercentage,
                        onValueChange = { cityPercentage = it },
                        label = { Text("City Tax Percentage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Medicare and Social Security
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Medicare Tax")
                    Switch(
                        checked = medicareTaxEnabled,
                        onCheckedChange = { medicareTaxEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Social Security Tax")
                    Switch(
                        checked = socialSecurityTaxEnabled,
                        onCheckedChange = { socialSecurityTaxEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        TaxSettings(
                            federalWithholding = federalWithholding,
                            stateTaxEnabled = stateTaxEnabled,
                            stateWithholdingPercentage = statePercentage.toDoubleOrNull() ?: 0.0,
                            cityTaxEnabled = cityTaxEnabled,
                            cityWithholdingPercentage = cityPercentage.toDoubleOrNull() ?: 0.0,
                            medicareTaxEnabled = medicareTaxEnabled,
                            socialSecurityTaxEnabled = socialSecurityTaxEnabled
                        )
                    )
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
    currentDeduction: Deduction? = null,
    onDismiss: () -> Unit,
    onConfirm: (Deduction) -> Unit
) {
    var name by remember { mutableStateOf(currentDeduction?.name ?: "") }
    var amount by remember { mutableStateOf(currentDeduction?.amount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(currentDeduction?.frequency ?: DeductionFrequency.PER_PAYCHECK) }
    var type by remember { mutableStateOf(currentDeduction?.type ?: DeductionType.OTHER) }
    var taxable by remember { mutableStateOf(currentDeduction?.taxable == true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentDeduction == null) "Add Deduction" else "Edit Deduction") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Deduction Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency Selection
                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeductionFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.name.replace("_", " ")) }
                        )
                    }
                }

                // Type Selection
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeductionType.entries.forEach { deductionType ->
                        FilterChip(
                            selected = type == deductionType,
                            onClick = { type = deductionType },
                            label = { Text(deductionType.name) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Taxable")
                    Switch(
                        checked = taxable,
                        onCheckedChange = { taxable = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && amount.toDoubleOrNull() != null) {
                        onConfirm(
                            Deduction(
                                name = name,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                frequency = frequency,
                                type = type,
                                taxable = taxable
                            )
                        )
                    }
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
