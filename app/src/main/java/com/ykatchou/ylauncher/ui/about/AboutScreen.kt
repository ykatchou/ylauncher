package com.ykatchou.ylauncher.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ykatchou.ylauncher.billing.BillingManager
import com.ykatchou.ylauncher.billing.PurchaseEvent
import com.ykatchou.ylauncher.data.repository.PrefsRepository
import com.ykatchou.ylauncher.ui.components.CoffeeFab

@Composable
fun AboutScreen(
    billingManager: BillingManager,
    prefsRepository: PrefsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val billingState by billingManager.billingState.collectAsState()
    val purchaseEvent by billingManager.purchaseEvent.collectAsState()
    val showDonation by prefsRepository.showDonation.collectAsState(initial = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(purchaseEvent) {
        when (purchaseEvent) {
            is PurchaseEvent.Success -> {
                snackbarHostState.showSnackbar("Thank you for your support!")
                billingManager.consumePurchaseEvent()
            }
            is PurchaseEvent.Error -> {
                snackbarHostState.showSnackbar("Payment error. Try Ko-fi instead?")
                billingManager.consumePurchaseEvent()
            }
            null -> {}
        }
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            ) {
                // App title
                Text(
                    text = "yLauncher",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "v1.1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Support the developer
                if (showDonation) {
                    SectionHeader("Support the developer")
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "If you enjoy yLauncher, consider buying me a coffee!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        CoffeeFab(
                            billingManager = billingManager,
                            billingState = billingState,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Author
                SectionHeader("Created by")
                Text(
                    text = "Yoann Katchourine",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                LinkText(
                    text = "github.com/ykatchou",
                    onClick = { openUrl("https://github.com/ykatchou") },
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Inspirations
                SectionHeader("Inspirations")
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "◉ OLauncher",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Minimal AF Launcher by tanujnotes — GPL v3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                LinkText(
                    text = "github.com/tanujnotes/Olauncher",
                    onClick = { openUrl("https://github.com/tanujnotes/Olauncher") },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "◉ Niagara Launcher",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Minimalist one-hand launcher by Peter Huber (Bitpit)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                LinkText(
                    text = "play.google.com/store/apps/details?id=bitpit.launcher",
                    onClick = { openUrl("https://play.google.com/store/apps/details?id=bitpit.launcher") },
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Typography
                SectionHeader("Typography")
                Text(
                    text = "Josefin Sans by Santiago Orozco — OFL license",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Text(
                    text = "Work Sans by Wei Huang — OFL license",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Built with
                SectionHeader("Built with")
                Text(
                    text = "Kotlin · Jetpack Compose · Material3 · Room · Hilt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Source code
                SectionHeader("Source code")
                LinkText(
                    text = "github.com/ykatchou/ylauncher",
                    onClick = { openUrl("https://github.com/ykatchou/ylauncher") },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "License: GPL v3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "← Back",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(vertical = 8.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onClick() },
    )
}
