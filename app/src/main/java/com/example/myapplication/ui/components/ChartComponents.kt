package com.example.myapplication.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@Composable
fun DonutChartCompose(totalBudget: Double, totalActual: Double, centerText: String) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                setUsePercentValues(true)
                isDrawHoleEnabled = true
                holeRadius = 50f
                setTransparentCircleAlpha(0)
                setCenterText(centerText)
                setCenterTextSize(16f)
                legend.isEnabled = false

                // Ensure labels fit within the chart
                setEntryLabelTextSize(12f)
                setDrawEntryLabels(true)
                setEntryLabelColor(Color.BLACK)
            }
        },
        update = { chart ->
            val entries = listOf(
                PieEntry(totalBudget.toFloat(), "Budgeted"),
                PieEntry(totalActual.toFloat(), "Actual")
            )

            val dataSet = PieDataSet(entries, "Budget Categories").apply {
                colors = listOf(
                    Color.parseColor("#FFB74D"), // Match the legend color
                    Color.parseColor("#66BB6A")  // Match the legend color
                )
                sliceSpace = 3f
                valueTextColor = Color.BLACK
                valueTextSize = 12f
                setDrawValues(true)
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp)
    )
}



