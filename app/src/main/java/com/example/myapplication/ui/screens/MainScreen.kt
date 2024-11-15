package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.theme.navigation.ScreenWithBottomBar
import com.example.myapplication.viewmodels.ExpenseViewModelFactory
import com.example.myapplication.viewmodels.MainViewModel
import com.example.myapplication.viewmodels.WalletViewModel
import com.example.myapplication.viewmodels.WalletViewModelFactory

@Composable
fun MainScreen(navController: NavHostController, repository: ExpenseRepository, onImportClick: () -> Unit) {
    val viewModel: MainViewModel = viewModel(factory = ExpenseViewModelFactory(repository))
    val walletViewModelFactory = WalletViewModelFactory(repository)
    val walletViewModel: WalletViewModel = viewModel(factory = walletViewModelFactory)

    val expenses by viewModel.expenses.collectAsState()
    val budgetComparisons by viewModel.filteredComparisons.collectAsState()

    ScreenWithBottomBar(navController = navController) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                FinanceSection(navController = navController, repository = repository)
            }
            item {
                CardsSection(viewModel = walletViewModel)
            }
            item {
                OverviewSection(viewModel = viewModel)
            }
        }
    }
}










