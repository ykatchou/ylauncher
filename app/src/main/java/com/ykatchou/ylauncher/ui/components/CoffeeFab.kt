package com.ykatchou.ylauncher.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykatchou.ylauncher.billing.BillingManager
import com.ykatchou.ylauncher.billing.BillingState

@Composable
fun CoffeeFab(
    billingManager: BillingManager,
    billingState: BillingState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    FloatingActionButton(
        onClick = {
            if (billingState == BillingState.READY && activity != null) {
                billingManager.launchTipPurchase(activity)
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/ykatchou"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        },
        containerColor = Color(0xFFFF813F),
        contentColor = Color.White,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = modifier.size(48.dp),
    ) {
        Text(
            text = "☕",
            fontSize = 20.sp,
        )
    }
}
