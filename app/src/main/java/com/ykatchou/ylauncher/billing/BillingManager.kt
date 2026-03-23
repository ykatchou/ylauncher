package com.ykatchou.ylauncher.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.ykatchou.ylauncher.data.repository.PrefsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepository: PrefsRepository,
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_TIP = "tip_coffee"
    }

    private var billingClient: BillingClient? = null
    private var tipProductDetails: ProductDetails? = null

    private val _billingState = MutableStateFlow(BillingState.DISCONNECTED)
    val billingState = _billingState.asStateFlow()

    private val _purchaseEvent = MutableStateFlow<PurchaseEvent?>(null)
    val purchaseEvent = _purchaseEvent.asStateFlow()

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        connect()
    }

    private fun connect() {
        _billingState.value = BillingState.CONNECTING
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    _billingState.value = BillingState.CONNECTED
                    queryProducts()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _billingState.value = BillingState.UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
                _billingState.value = BillingState.DISCONNECTED
                tipProductDetails = null
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_TIP)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                tipProductDetails = productDetailsList.firstOrNull()
                if (tipProductDetails != null) {
                    Log.d(TAG, "Product found: ${tipProductDetails?.name}")
                    _billingState.value = BillingState.READY
                } else {
                    Log.w(TAG, "Product $PRODUCT_TIP not found in store")
                    _billingState.value = BillingState.UNAVAILABLE
                }
            } else {
                Log.w(TAG, "Product query failed: ${billingResult.debugMessage}")
                _billingState.value = BillingState.UNAVAILABLE
            }
        }
    }

    fun launchTipPurchase(activity: Activity) {
        val product = tipProductDetails
        if (product == null) {
            _purchaseEvent.value = PurchaseEvent.Error("Product not available")
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )
            )
            .build()
        billingClient?.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        consumePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase cancelled by user")
            }
            else -> {
                Log.w(TAG, "Purchase error: ${billingResult.debugMessage}")
                _purchaseEvent.value = PurchaseEvent.Error(billingResult.debugMessage)
            }
        }
    }

    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.consumeAsync(consumeParams) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Tip consumed successfully")
                _purchaseEvent.value = PurchaseEvent.Success
                CoroutineScope(Dispatchers.IO).launch {
                    prefsRepository.setShowDonation(false)
                }
            } else {
                Log.w(TAG, "Consume failed: ${billingResult.debugMessage}")
                _purchaseEvent.value = PurchaseEvent.Error(billingResult.debugMessage)
            }
        }
    }

    fun consumePurchaseEvent() {
        _purchaseEvent.value = null
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}

enum class BillingState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    READY,
    UNAVAILABLE,
}

sealed class PurchaseEvent {
    data object Success : PurchaseEvent()
    data class Error(val message: String) : PurchaseEvent()
}
