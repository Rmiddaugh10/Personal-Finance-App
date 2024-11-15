package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.cards
import com.example.myapplication.viewmodels.WalletViewModel


@Composable
fun CardsSection(viewModel: WalletViewModel) {
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
                    text = "Accounts",
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "View and manage your accounts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "View All",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* Handle click */ }
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cards.size) { index ->
                EnhancedCardItem(index = index, viewModel = viewModel)
            }
        }
    }
}




@Composable
fun EnhancedCardItem(index: Int, viewModel: WalletViewModel) {
    val context = LocalContext.current
    val card = cards[index]
    var lastItemPaddingEnd = 0.dp
    if (index == cards.size - 1) {
        lastItemPaddingEnd = 16.dp
    }

    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = lastItemPaddingEnd)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(25.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
    ) {
        Card(
            modifier = Modifier
                .width(250.dp)
                .height(170.dp)
                .clickable {
                    when {
                        card.cardNumber == "Community America Credit Union" && card.cardName == "Checking" -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cacuonlinebanking.com/dbank/live/app/home/olb/history?accountId=D1"))
                            context.startActivity(intent)
                        }
                        card.cardNumber == "Community America Credit Union" && card.cardName == "High Interest Savings" -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cacuonlinebanking.com/dbank/live/app/home/olb/history?accountId=D3"))
                            context.startActivity(intent)
                        }
                        card.cardNumber == "Other" && card.cardName == "Savings" -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cacuonlinebanking.com/dbank/live/app/home/olb/history?accountId=D2"))
                            context.startActivity(intent)
                        }
                        card.cardNumber == "Wallet" && card.cardName == "Cash" -> {
                            viewModel.navigateToWalletScreen()
                        }
                        card.cardNumber == "Vanguard" && card.cardName == "401K" -> {
                            val intent = context.packageManager.getLaunchIntentForPackage("vanguard.com")
                            intent?.let { context.startActivity(it) }
                        }
                        card.cardNumber == "Alight Mobile" && card.cardName == "Pension" -> {
                            val intent = context.packageManager.getLaunchIntentForPackage("alightmobile.com")
                            intent?.let { context.startActivity(it) }
                        }
                        card.cardNumber == "Robinhood" && card.cardName == "Investments" -> {
                            val intent = context.packageManager.getLaunchIntentForPackage("robinhood.com")
                            intent?.let { context.startActivity(it) }
                        }
                    }
                           },
            shape = RoundedCornerShape(25.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(card.color)
            ) {
                // Add subtle pattern overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Add subtle pattern or gradient overlay
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = when (card.cardType) {
                                "MASTER CARD" -> painterResource(id = R.drawable.ic_mastercard)
                                else -> painterResource(id = R.drawable.ic_visa)
                            },
                            contentDescription = card.cardName,
                            modifier = Modifier.width(60.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = card.cardName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$ ${card.balance}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = card.cardNumber,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


