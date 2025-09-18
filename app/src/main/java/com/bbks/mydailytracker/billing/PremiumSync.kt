package com.bbks.mydailytracker.billing

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PremiumSync(
    private val app: Application,
    private val premiumProductId: String,
    private val setPremium: (Boolean) -> Unit,
    private val refreshPreferences: () -> Unit
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val client: BillingClient by lazy {
        BillingClient.newBuilder(app)
            .enablePendingPurchases()
            .setListener(this) // 반드시 설정해야 함
            .build()
    }

    fun init() {
        // 앱 시작 시 1회 연결 + 동기화
        startConnection { refreshPremiumStatus() }

        // 포그라운드 복귀마다 동기화
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { refreshPremiumStatus() }
        })
    }

    private fun startConnection(onReady: (() -> Unit)? = null) {
        if (client.isReady) { onReady?.invoke(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady?.invoke()
            }
            override fun onBillingServiceDisconnected() { /* 자동 재연결은 다음 호출 시 */ }
        })
    }

    fun refreshPremiumStatus() {
        startConnection {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.queryPurchasesAsync(params) { _: BillingResult, list: List<Purchase> ->
                val active = list.any { p ->
                    p.products.contains(premiumProductId) &&
                            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            p.isAcknowledged
                }
                scope.launch {
                    setPremium(active)
                    refreshPreferences()
                    Log.d("PremiumSync", "restore active=$active, purchases=${list.map { it.products }}")
                }
            }
        }
    }

    // 구매 플로우에서 ITEM_ALREADY_OWNED 등 어떤 콜백이 와도 동기화
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        refreshPremiumStatus()
    }
}
