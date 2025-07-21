package com.bbks.mydailytracker.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
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

    fun setup() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            lifecycleScope.launch {
                                setPremiumUser(true)
                                refreshPreferences()
                                onPurchaseComplete()
                            }
                        }
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    onPurchaseCancelled()
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "BillingClient 연결 성공")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("Billing", "BillingClient 연결 끊김")
            }
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
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val isPremiumPurchased = purchasesList.any { purchase ->
                    purchase.products.contains("premium_upgrade") &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (isPremiumPurchased) {
                    lifecycleScope.launch {
                        setPremiumUser(true)
                        refreshPreferences()
                        Log.d("Billing", "✅ 이전 구매 복원됨")
                    }
                } else {
                    Log.d("Billing", "☑️ 프리미엄 구매 이력 없음")
                }
            } else {
                Log.w("Billing", "❌ queryPurchasesAsync 실패: ${billingResult.debugMessage}")
            }
        }
    }
}