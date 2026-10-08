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
import com.android.billingclient.api.GetBillingConfigParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.eva.ai.BuildConfig
import java.security.MessageDigest

/**
 * Thin wrapper around Google Play Billing for the Eva Premium monthly
 * subscription. Purchases are verified by the backend, so this class only
 * handles the Play-side flow and hands purchase tokens to [onPurchased].
 */
class PlayBillingManager(
    context: Context,
    private val onPurchased: (purchaseToken: String, productId: String) -> Unit,
    private val onRestoreError: (String) -> Unit = {},
    private val onAlternativeBilling: (String, String) -> Unit = { _, _ -> },
    private val accountId: () -> String? = { null }
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val checkoutOwner = BillingCheckoutOwner()
    private var checkoutInProgress = false
    private var useUserChoice = false
    private var closed = false

    companion object {
        const val PREMIUM_PRODUCT_ID = "eva_premium_monthly"
        const val MONTHLY_BASE_PLAN_ID = "monthly"
    }

    private val listener = PurchasesUpdatedListener { billingResult, purchases ->
        checkoutInProgress = false
        checkoutOwner.onPlayUpdate()
        if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            restorePurchases()
            return@PurchasesUpdatedListener
        }
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            mainHandler.post { onRestoreError(if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
                "Purchase cancelled." else "Google Play could not complete the purchase. Please try again.") }
            return@PurchasesUpdatedListener
        }
        if (purchases?.any { it.purchaseState == Purchase.PurchaseState.PENDING } == true) {
            mainHandler.post { onRestoreError("Payment is pending. Premium activates after Google Play confirms payment.") }
        }
        val bought = purchases?.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED } ?: return@PurchasesUpdatedListener
        mainHandler.post {
            bought.forEach { purchase ->
                val productId = purchase.products.firstOrNull { it == PREMIUM_PRODUCT_ID } ?: return@forEach
                onPurchased(purchase.purchaseToken, productId)
            }
        }
    }

    private val standardClient = createClient(context, false)
    private val userChoiceClientDelegate = lazy { createClient(context, true) }
    private val userChoiceClient by userChoiceClientDelegate
    private val billingClient: BillingClient get() = if (useUserChoice) userChoiceClient else standardClient

    private fun createClient(context: Context, userChoice: Boolean): BillingClient = BillingClient.newBuilder(context)
        .setListener(listener)
        .enableAutoServiceReconnection()
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .apply {
            if (userChoice && BuildConfig.ALTERNATIVE_BILLING_ENABLED) {
                enableUserChoiceBilling { details ->
                    mainHandler.post {
                        if (closed) return@post
                        checkoutInProgress = false
                        if (!checkoutOwner.consumeAlternative(accountId())) {
                            onRestoreError("Your account changed. Start checkout again.")
                        } else if (details.products.none { it.id == PREMIUM_PRODUCT_ID } || details.externalTransactionToken.isBlank()) {
                            onRestoreError("Google Play did not return a valid Premium billing choice.")
                        } else onAlternativeBilling(details.externalTransactionToken, "IN")
                    }
                }
            }
        }
        .build()

    fun connect() {
        if (closed) return
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    mainHandler.post { onRestoreError("Google Play billing is unavailable. Please try again.") }
                    return
                }
                restorePurchases()
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun launchSubscribe(activity: Activity, alternativeBillingAllowed: Boolean = false, onError: (String) -> Unit) {
        if (closed || checkoutInProgress) return
        val userId = accountId()?.takeIf { it.isNotBlank() }
        if (userId == null) {
            onError("Sign in before starting Premium.")
            return
        }
        if (!billingClient.isReady) {
            connect()
            onError("Google Play is connecting. Try again in a moment.")
            return
        }

        checkoutInProgress = true
        billingClient.getBillingConfigAsync(GetBillingConfigParams.newBuilder().build()) { result, config ->
            mainHandler.post {
                if (closed) return@post
                if (result.responseCode != BillingClient.BillingResponseCode.OK || config == null || accountId() != userId) {
                    checkoutInProgress = false
                    onError("Could not verify your Google Play billing country. Please try again.")
                    return@post
                }
                // Use this response only for this checkout; never cache it in the user profile.
                useUserChoice = IndiaBillingPolicy.offerUserChoice(BuildConfig.ALTERNATIVE_BILLING_ENABLED && alternativeBillingAllowed, config.countryCode)
                if (billingClient.isReady) queryAndLaunch(activity, userId, onError)
                else billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        mainHandler.post {
                            if (closed) return@post
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) queryAndLaunch(activity, userId, onError)
                            else { checkoutInProgress = false; onError("Google Play billing is unavailable. Please try again.") }
                        }
                    }
                    override fun onBillingServiceDisconnected() = Unit
                })
            }
        }
    }

    private fun queryAndLaunch(activity: Activity, userId: String, onError: (String) -> Unit) {
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
            val details = productDetailsResult.productDetailsList.firstOrNull { it.productId == PREMIUM_PRODUCT_ID }
            if (result.responseCode != BillingClient.BillingResponseCode.OK || details == null) {
                mainHandler.post {
                    checkoutInProgress = false
                    onError("The Eva Premium plan is not available in Google Play yet.")
                }
                return@queryProductDetailsAsync
            }
            val offer = details.subscriptionOfferDetails?.firstOrNull {
                it.basePlanId == MONTHLY_BASE_PLAN_ID && it.offerId == null
            } ?: details.subscriptionOfferDetails?.firstOrNull { it.basePlanId == MONTHLY_BASE_PLAN_ID }
            if (offer == null) {
                mainHandler.post { checkoutInProgress = false; onError("The monthly Premium plan is unavailable for this Google Play account.") }
                return@queryProductDetailsAsync
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setObfuscatedAccountId(MessageDigest.getInstance("SHA-256")
                    .digest("eva-google-play:v1:$userId".toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) })
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offer.offerToken)
                            .build()
                    )
                )
                .build()

            mainHandler.post {
                if (closed || accountId() != userId) {
                    checkoutInProgress = false
                    onError("Your account changed. Start checkout again.")
                    return@post
                }
                checkoutOwner.begin(userId, useUserChoice)
                val launched = billingClient.launchBillingFlow(activity, flowParams)
                if (launched.responseCode != BillingClient.BillingResponseCode.OK) { checkoutOwner.clear(); checkoutInProgress = false }
                if (launched.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) restorePurchases()
                else if (launched.responseCode != BillingClient.BillingResponseCode.OK) onError("Google Play checkout could not open. Please try again.")
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

    fun restorePurchases() {
        if (accountId().isNullOrBlank()) return
        if (!billingClient.isReady) {
            connect()
            return
        }
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
                    val productId = purchase.products.firstOrNull { it == PREMIUM_PRODUCT_ID } ?: return@forEach
                    onPurchased(purchase.purchaseToken, productId)
                }
            }
        }
    }

    fun queryOwnedPurchases(onResult: (List<Purchase>, String?) -> Unit) {
        if (closed || !billingClient.isReady) {
            onResult(emptyList(), "Google Play is connecting. Try again in a moment.")
            return
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(emptyList(), "Could not check existing Google Play purchases.")
            } else onResult(purchases.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED && PREMIUM_PRODUCT_ID in it.products
            }, null)
        }
    }

    fun close() {
        closed = true
        checkoutOwner.clear()
        mainHandler.removeCallbacksAndMessages(null)
        standardClient.endConnection()
        if (userChoiceClientDelegate.isInitialized()) userChoiceClient.endConnection()
    }
}
