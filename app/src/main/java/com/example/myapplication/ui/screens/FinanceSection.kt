@file:Suppress("DEPRECATION")

package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.theme.navigation.Screen
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


data class Finance(
    val icon: ImageVector,
    val name: String,
    val background: Color
)

val financeList = listOf(
    Finance(
        icon = Icons.Rounded.StarHalf,
        name = "Our\nBudget",
        background = Color(0xFFFF9800)
    ),
    Finance(
        icon = Icons.Rounded.Wallet,
        name = "Our\nWallet",
        background = Color(0xFF4CAF50)
    ),
    Finance(
        icon = Icons.Rounded.Analytics,
        name = "Financial\nAnalytics",
        background = Color(0xFF9C27B0)
    ),
    Finance(
        icon = Icons.Rounded.MonetizationOn,
        name = "All\nTransactions",
        background = Color(0xFF2196F3)
    ),
    Finance(
        icon = Icons.Rounded.MonetizationOn,
        name = "Budget V\nActual",
        background = Color(0xFF2196F3)
),
)


@Composable
fun FinanceSection(navController: NavController, repository: ExpenseRepository) {
    val currentDateTime = remember { LocalDateTime.now() }
    val formattedDate = remember {
        currentDateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp)
    ) {
        // Enhanced Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                IconButton(
                    onClick = { /* Notification handler */ },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Enhanced section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Our Finances",
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Quick access to financial tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(financeList.size) { index ->
                    EnhancedFinanceItem(index = index, navController = navController)
                }
            }
        }
    }
}


    @Composable
    fun EnhancedFinanceItem(index: Int, navController: NavController) {
        val finance = financeList[index]
        var lastPaddingEnd = 0.dp
        if (index == financeList.size - 1) {
            lastPaddingEnd = 16.dp
        }

        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = lastPaddingEnd)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(25.dp),
                    spotColor = finance.background.copy(alpha = 0.1f)
                )
        ) {
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        val route = when (finance.name) {
                            "Our\nBudget" -> Screen.Budget.route
                            "Our\nWallet" -> Screen.Wallet.route
                            "Financial\nAnalytics" -> Screen.FinancialAnalytics.route
                            "All\nTransactions" -> Screen.AllTransactions.route
                            "Budget V\nActual" -> Screen.BudgetVsActualScreen.route
                            else -> ""
                        }
                        if (route.isNotEmpty()) {
                            navController.navigate(route) {
                                popUpTo(Screen.Main.route) { saveState = true }
                            }
                        }
                    },
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(finance.background)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = finance.icon,
                            contentDescription = finance.name,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = finance.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        LinearProgressIndicator(
                            progress = 0.8f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = finance.background
                        )
                    }
                }
            }
        }
    }
