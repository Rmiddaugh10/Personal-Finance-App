package com.example.myapplication.data

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.theme.BlueEnd
import com.example.myapplication.ui.theme.BlueStart
import com.example.myapplication.ui.theme.GreenEnd
import com.example.myapplication.ui.theme.GreenStart
import com.example.myapplication.ui.theme.OrangeEnd
import com.example.myapplication.ui.theme.OrangeStart
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart


val cards = listOf(
    Card(cardType = "VISA", cardNumber = "Community America Credit Union", cardName = "Checking", balance = 46.467, color = getGradient(PurpleStart, PurpleEnd)),
    Card(cardType = "MASTER CARD", cardNumber = "Community America Credit Union", cardName = "High Interest Savings", balance = 6.467, color = getGradient(BlueStart, BlueEnd)),
    Card(cardType = "VISA", cardNumber = "Wallet", cardName = "Cash", balance = 3.467, color = getGradient(OrangeStart, OrangeEnd)),
    Card(cardType = "MASTER CARD", cardNumber = "Other", cardName = "Savings", balance = 26.47, color = getGradient(GreenStart, GreenEnd)),
)

fun getGradient(startColor: Color, endColor: Color): Brush {
    return Brush.horizontalGradient(colors = listOf(startColor, endColor))
}

