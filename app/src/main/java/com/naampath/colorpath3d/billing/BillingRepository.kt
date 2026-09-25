package com.naampath.colorpath3d.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.naampath.colorpath3d.data.ProgressRepository
import com.naampath.colorpath3d.firebase.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ShopProduct(val id: String, val title: String, val coins: Int, val removesAds: Boolean, val premium: Boolean, val hints: Int) {
    REMOVE_ADS("remove_ads", "Remove Ads", 0, true, false, 0),
    COINS_SMALL("coins_small", "Coin Pack Small", 200, false, false, 0),
    COINS_MEDIUM("coins_medium", "Coin Pack Medium", 600, false, false, 0),
    COINS_LARGE("coins_large", "Coin Pack Large", 1500, false, false, 0),
    PREMIUM("premium", "Premium", 800, true, true, 10)
}

class BillingRepository(
    context: Context,
    private val progress: ProgressRepository,
    private val analytics: AnalyticsManager
) : PurchasesUpdatedListener {
    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    private val details = mutableMapOf<String, com.android.billingclient.api.ProductDetails>()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun connect() {
        if (client.isReady) {
            _ready.value = true
            queryProducts()
            restore()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                _ready.value = result.responseCode == BillingClient.BillingResponseCode.OK
                if (_ready.value) {
                    queryProducts()
                    restore()
                } else {
                    _message.value = "Billing is unavailable. Gameplay still works offline."
                }
            }
            override fun onBillingServiceDisconnected() {
                _ready.value = false
            }
        })
    }

    fun launch(activity: Activity, product: ShopProduct) {
        val detail = details[product.id]
        if (!_ready.value || detail == null) {
            _message.value = "Billing is unavailable right now."
            connect()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(detail)
                        .build()
                )
            ).build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        purchases.forEach { handle(it) }
    }

    private fun queryProducts() {
        val products = ShopProduct.entries.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { billingResult, list ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                list.forEach { details[it.productId] = it }
            }
        }
    }

    fun restore() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handle(it) }
            }
        }
    }

    private fun handle(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        purchase.products.forEach { id ->
            val product = ShopProduct.entries.find { it.id == id } ?: return@forEach
            kotlinx.coroutines.runBlocking {
                progress.applyEntitlement(product.removesAds, product.premium, product.coins, product.hints)
            }
            analytics.event("purchase_completed", mapOf("product" to id))
        }
        if (!purchase.isAcknowledged) {
            val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            client.acknowledgePurchase(ack) { }
        }
    }
}
