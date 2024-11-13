package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayRateDialog(
    currentRates: PayRates,
    employmentType: EmploymentType,
    onDismiss: () -> Unit,
    onConfirm: (PayRates) -> Unit
) {
    var basePay by remember { mutableStateOf(currentRates.basePay.toString()) }
    var payFrequency by remember { mutableStateOf(currentRates.payFrequency) }
    var weekendRate by remember { mutableStateOf(currentRates.weekendRate.toString()) }
    var nightDifferential by remember { mutableStateOf(currentRates.nightDifferential.toString()) }
    var overtimeMultiplier by remember { mutableStateOf(currentRates.overtimeMultiplier.toString()) }
    var holidayRate by remember { mutableStateOf(currentRates.holidayRate.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employmentType == EmploymentType.SALARY) "Salary Settings" else "Pay Rate Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = basePay,
                    onValueChange = { basePay = it },
                    label = {
                        Text(if (employmentType == EmploymentType.SALARY) "Annual Salary" else "Base Hourly Rate")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Pay Frequency Selection
                Text("Pay Frequency", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PayFrequency.entries.forEach { frequency ->
                        FilterChip(
                            selected = payFrequency == frequency,
                            onClick = { payFrequency = frequency },
                            label = { Text(frequency.name.replace("_", " ")) }
                        )
                    }
                }

                if (employmentType == EmploymentType.HOURLY) {
                    OutlinedTextField(
                        value = weekendRate,
                        onValueChange = { weekendRate = it },
                        label = { Text("Weekend Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nightDifferential,
                        onValueChange = { nightDifferential = it },
                        label = { Text("Night Differential") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = overtimeMultiplier,
                        onValueChange = { overtimeMultiplier = it },
                        label = { Text("Overtime Multiplier") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = holidayRate,
                        onValueChange = { holidayRate = it },
                        label = { Text("Holiday Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        PayRates(
                            basePay = basePay.toDoubleOrNull() ?: 0.0,
                            payFrequency = payFrequency,
                            weekendRate = weekendRate.toDoubleOrNull() ?: 0.0,
                            nightDifferential = nightDifferential.toDoubleOrNull() ?: 0.0,
                            overtimeMultiplier = overtimeMultiplier.toDoubleOrNull() ?: 1.5,
                            holidayRate = holidayRate.toDoubleOrNull() ?: 0.0
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

