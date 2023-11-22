package online.pasaka.domain.model.escrow

data class BuyEscrowWallet(
    val orderId:String,
    val merchantAdId:String,
    val merchantEmail:String,
    val buyerEmail:String,
    val cryptoName:String,
    val cryptoSymbol: String,
    val cryptoAmount:Double,
    val escrowState: online.pasaka.domain.model.escrow.EscrowState,
    val debitedAt:String,
    val expiresAt:Long

    )
