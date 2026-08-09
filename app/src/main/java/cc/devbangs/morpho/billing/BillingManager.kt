package cc.devbangs.morpho.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.android.billingclient.api.*
import cc.devbangs.morpho.ads.AdState

/**
 * Manages Morpho Plus subscriptions via Google Play Billing (v9).
 * Fails gracefully: if products don't exist yet (pre-launch), stays on Free, no crash.
 * When the products are created in Play Console, this lights up automatically.
 */
object BillingManager {
    // Product IDs — MUST match what you create in Play Console.
    const val PRODUCT_MONTHLY = "morpho_plus_monthly"
    const val PRODUCT_YEARLY = "morpho_plus_yearly"

    private var billingClient: BillingClient? = null

    // Observable state for the UI
    val isReady = mutableStateOf(false)
    val monthlyPrice = mutableStateOf<String?>(null)   // e.g. "$2.99"
    val yearlyPrice = mutableStateOf<String?>(null)     // e.g. "$19.99"
    val statusMessage = mutableStateOf<String?>(null)

    private var monthlyDetails: ProductDetails? = null
    private var yearlyDetails: ProductDetails? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            statusMessage.value = null
        }
    }

    /** Connect to Play Billing. Call once at app start. */
    fun start(ctx: Context) {
        if (billingClient != null) return
        val client = BillingClient.newBuilder(ctx)
            .setListener(purchasesListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        billingClient = client
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    isReady.value = true
                    queryProducts()
                    restorePurchases()
                }
            }
            override fun onBillingServiceDisconnected() { isReady.value = false }
        })
    }

    /** Query the subscription products + their localized prices. */
    private fun queryProducts() {
        val client = billingClient ?: return
        val products = listOf(PRODUCT_MONTHLY, PRODUCT_YEARLY).map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            productDetailsResult.productDetailsList.forEach { pd ->
                val price = pd.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    ?.formattedPrice
                when (pd.productId) {
                    PRODUCT_MONTHLY -> { monthlyDetails = pd; monthlyPrice.value = price }
                    PRODUCT_YEARLY -> { yearlyDetails = pd; yearlyPrice.value = price }
                }
            }
        }
    }

    /** Launch the purchase flow for a plan. */
    fun purchase(activity: Activity, yearly: Boolean) {
        val client = billingClient ?: return
        val details = if (yearly) yearlyDetails else monthlyDetails
        if (details == null) {
            statusMessage.value = "Plans aren't available yet. Please try again later."
            return
        }
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    /** Check existing purchases (so paid users stay Plus after reinstall/launch). */
    private fun restorePurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val active = purchases.any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                AdState.isPlus.value = active
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    /** Handle a new/existing purchase: unlock Plus + acknowledge it. */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        // Unlock Plus immediately
        AdState.isPlus.value = true
        statusMessage.value = null
        // Acknowledge (required within 3 days or Google refunds the purchase)
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { /* acknowledged */ }
        }
    }

    /** Manual restore (for a "Restore purchase" button). */
    fun restore() { restorePurchases() }
}
