package online.pasaka.model.escrow

enum class EscrowState {
    PENDING,
    PENDING_MERCHANT_RELEASE,
    AUTO_RELEASED,
    CRYPTO_RELEASED_TO_BUYER,
    CRYPTO_SEND_TO_ORDER_APPEAL
}