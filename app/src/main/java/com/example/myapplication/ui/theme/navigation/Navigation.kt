package com.example.myapplication.ui.theme.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.screens.*
import com.example.myapplication.viewmodels.MainViewModel

// Sealed class for defining routes
sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Cards : Screen("cards")
    object Overview : Screen("overview")
    object Recent : Screen("recent")
    object Budget : Screen("budget")
    object Wallet : Screen("wallet")
    object BudgetVsActualScreen : Screen("budget_vs_actual")
    object FinancialAnalytics : Screen("finance_analytics")
    object AllTransactions : Screen("transactions")
}

// Data class for bottom navigation items
data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

// List of bottom navigation items
val bottomNavItems = listOf(
    BottomNavItem(Screen.Main, "Main", Icons.Filled.Home),
    BottomNavItem(Screen.Cards, "Import CSV", Icons.Filled.CreditCard),
    BottomNavItem(Screen.Overview, "Overview", Icons.Filled.PieChart),
    BottomNavItem(Screen.Recent, "Recent", Icons.Filled.History)
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    repository: ExpenseRepository,
    paddingValues: PaddingValues,
    viewModel: MainViewModel,
    onImportClick: () -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        // Main home screen
        composable(Screen.Main.route) {
            MainScreen(navController = navController, repository = repository, onImportClick = onImportClick)
        }

        // Screen accessed from bottom navigation
        composable(Screen.Cards.route) {
            CardsScreen(
                navController = navController,
                viewModel = viewModel,
                onImportClick = onImportClick
            )
        }

        composable(Screen.Overview.route) { OverviewScreen(navController = navController, budgetComparisons = viewModel.filteredComparisons) }

        composable(Screen.Recent.route) { RecentTransactionsScreen(navController = navController, viewModel = viewModel) }

        // Tile navigation screens - ensure these are accessible only from FinanceSection tiles, not from BottomNavigationBar
        composable(Screen.Budget.route) { BudgetScreen(navController, viewModel) }
        composable(Screen.Wallet.route) { WalletScreen(repository) }
        composable(Screen.FinancialAnalytics.route) { FinancialAnalytics(navController) }
        composable(Screen.AllTransactions.route) { AllTransactions(navController) }
        composable(Screen.BudgetVsActualScreen.route) { BudgetVsActualScreen(navController = navController, viewModel = viewModel) }
    }
}
