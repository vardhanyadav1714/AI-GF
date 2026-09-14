package com.eva.ai.data.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Thin wrapper around Google Play Billing for the Eva Premium monthly
 * subscription. Purchases are verified by the backend, so this class only
 * handles the Play-side flow and hands purchase tokens to [onPurchased].
 */
class PlayBillingManager(
    context: Context,
    private val onPurchased: (purchaseToken: String, productId: String) -> Unit,
    private val onRestoreError: (String) -> Unit = {}
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val PREMIUM_PRODUCT_ID = "eva_premium_monthly"
    }

    private val listener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@PurchasesUpdatedListener
        val bought = purchases?.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED } ?: return@PurchasesUpdatedListener
        mainHandler.post {
            bought.forEach { purchase ->
                val productId = purchase.products.firstOrNull() ?: return@forEach
                onPurchased(purchase.purchaseToken, productId)
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
        .build()

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                queryAlreadyOwned()
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun launchSubscribe(activity: Activity, onError: (String) -> Unit) {
        if (!billingClient.isReady) {
            connect()
            onError("Google Play is connecting. Try again in a moment.")
            return
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsResult ->
            val details = productDetailsResult.productDetailsList.firstOrNull()
            if (result.responseCode != BillingClient.BillingResponseCode.OK || details == null) {
                mainHandler.post {
                    onError("The Eva Premium plan is not available in Google Play yet.")
                }
                return@queryProductDetailsAsync
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .apply {
                                details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let {
                                    setOfferToken(it)
                                }
                            }
                            .build()
                    )
                )
                .build()

            mainHandler.post {
                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    /** Acknowledges the purchase after backend verification (required by Play within 3 days). */
    fun acknowledge(purchaseToken: String) {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { _, purchases ->
            val purchase = purchases.firstOrNull { it.purchaseToken == purchaseToken } ?: return@queryPurchasesAsync
            if (purchase.isAcknowledged) return@queryPurchasesAsync
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
            ) { }
        }
    }

    private fun queryAlreadyOwned() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                mainHandler.post { onRestoreError("Could not check existing Google Play purchases.") }
                return@queryPurchasesAsync
            }
            val owned = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            if (owned.isEmpty()) return@queryPurchasesAsync
            mainHandler.post {
                owned.forEach { purchase ->
                    val productId = purchase.products.firstOrNull() ?: return@forEach
                    onPurchased(purchase.purchaseToken, productId)
                }
            }
        }
    }
}
