package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.BudgetComparison
import kotlinx.coroutines.flow.StateFlow

// Define the ChartData data class
data class ChartData(val x: Float, val y: Float)

// Function to generate dummy data for the chart
fun getData(): List<ChartData> {
    return listOf(
        ChartData(1f, 2f),
        ChartData(2f, 4f),
        ChartData(3f, 1.5f),
        ChartData(4f, 3f),
        ChartData(5f, 5f)
    )
}


@Composable
fun OverviewScreen(navController: NavController, budgetComparisons: StateFlow<List<BudgetComparison>>) {
    // Collect budget comparisons from StateFlow
    val budgetComparisonList = budgetComparisons.collectAsState(initial = emptyList()).value

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val chartHeight = screenWidth * 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Overview",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            LineChart(
                data = getData(),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display budget comparisons
        budgetComparisonList.forEach { comparison ->
            Text(text = comparison.toString())
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {}
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}



@Composable
fun LineChart(data: List<ChartData>, modifier: Modifier = Modifier) {
    // Use the chart library's component to draw the chart
    // For example, using a line chart library
    // Example chart setup goes here
}

