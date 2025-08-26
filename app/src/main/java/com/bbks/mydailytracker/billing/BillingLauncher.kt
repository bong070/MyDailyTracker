package com.bbks.mydailytracker.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.launch

class BillingLauncher(
    private val activity: Activity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onPurchaseComplete: () -> Unit,
    private val onPurchaseCancelled: () -> Unit,
    private val setPremiumUser: suspend (Boolean) -> Unit,
    private val refreshPreferences: () -> Unit
) {
    private lateinit var billingClient: BillingClient

    companion object {
        private const val PREMIUM_PRODUCT_ID = "premium_upgrade"
    }

    fun setup() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        if (purchases != null) {
                            handlePurchases(purchases)
                        } else {
                            refreshPremiumStatus()
                        }
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> onPurchaseCancelled()
                    else -> {
                        Log.w("Billing", "onPurchasesUpdated: ${billingResult.debugMessage}")
                        refreshPremiumStatus()
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "BillingClient 연결 성공")
                    refreshPremiumStatus()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    fun launchPurchase(productId: String) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                Log.e("Billing", "상품 정보 가져오기 실패: ${result.debugMessage}")
            }
        }
    }

    fun restorePurchase() {
        refreshPremiumStatus()
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val targets = purchases.filter { it.products.contains(PREMIUM_PRODUCT_ID) }
        if (targets.isEmpty()) {
            refreshPremiumStatus()
            return
        }
        targets.forEach { p ->
            if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(p.purchaseToken).build()
                billingClient.acknowledgePurchase(params) {
                    refreshPremiumStatus()
                    onPurchaseComplete()
                }
            } else {
                refreshPremiumStatus()
                onPurchaseComplete()
            }
        }
    }

    fun refreshPremiumStatus() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(params) { _, list ->
            val isActive = list.any { p ->
                p.products.contains(PREMIUM_PRODUCT_ID) &&
                        p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        p.isAcknowledged
            }
            lifecycleScope.launch {
                setPremiumUser(isActive)
                refreshPreferences()
            }
        }
    }
}