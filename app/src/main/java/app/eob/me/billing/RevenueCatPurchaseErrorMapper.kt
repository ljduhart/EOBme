package app.eob.me.billing

import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesTransactionException

internal object RevenueCatPurchaseErrorMapper {
    fun noticeKeyFor(transactionError: PurchasesTransactionException): String {
        if (transactionError.userCancelled) {
            return "billing_user_canceled"
        }
        return when (transactionError.code) {
            PurchasesErrorCode.PaymentPendingError -> "billing_payment_pending"
            PurchasesErrorCode.PurchaseInvalidError,
            PurchasesErrorCode.StoreProblemError,
            PurchasesErrorCode.InvalidReceiptError,
            PurchasesErrorCode.SignatureVerificationError -> "billing_payment_declined"
            PurchasesErrorCode.ProductNotAvailableForPurchaseError -> "billing_product_unavailable"
            PurchasesErrorCode.ProductAlreadyPurchasedError -> "billing_already_subscribed"
            PurchasesErrorCode.NetworkError -> "billing_not_ready"
            PurchasesErrorCode.PurchaseCancelledError -> "billing_user_canceled"
            else -> "billing_flow_failed"
        }
    }
}
