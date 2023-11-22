package online.pasaka.infrastructure.database

import com.mongodb.client.MongoDatabase

import org.litote.kmongo.getCollection

object Entries {
    /** Database Instance*/
    private val database: MongoDatabase = DatabaseConnection.database

    /** User collection Instances*/
    val dbUser = DatabaseConnection.database.getCollection<online.pasaka.domain.model.user.User>()
    val userWallet = database.getCollection<online.pasaka.domain.model.wallet.Wallet>()

    /** Merchant collection Instances*/
    val dbMerchant = DatabaseConnection.database.getCollection<online.pasaka.domain.model.merchant.Merchant>()
    val dbMerchantWallet = DatabaseConnection.database.getCollection<online.pasaka.domain.model.merchant.wallet.MerchantWallet>()
    val dbMerchantsWithdrawalsHistory = DatabaseConnection.database.getCollection<online.pasaka.domain.model.merchant.wallet.MerchantsWithdrawalsHistory>()
    val dbMerchantTopUpsHistory = DatabaseConnection.database.getCollection<online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory>()
    val dbCreateSellAd = DatabaseConnection.database.getCollection<online.pasaka.domain.model.cryptoAds.SellAd>("sellAds")
    val merchantAdHistory = DatabaseConnection.database.getCollection<online.pasaka.domain.model.merchant.wallet.MerchantAdsHistory>("AdsHistory")

    /** Crypto Buy Order collection instances*/
    val buyEscrowWallet = DatabaseConnection.database.getCollection<online.pasaka.domain.model.escrow.BuyEscrowWallet>("buyEscrowWallet")
    val cryptoBuyOrders = DatabaseConnection.database.getCollection<online.pasaka.domain.model.order.BuyOrder>("buyOrders")
    val buyAd = DatabaseConnection.database.getCollection<online.pasaka.domain.model.cryptoAds.BuyAd>("buyAds")
    val buyersWallet = DatabaseConnection.database.getCollection<online.pasaka.domain.model.wallet.Wallet>()

    /** Crypto Sell Order collection instances */
    val sellAd = DatabaseConnection.database.getCollection<online.pasaka.domain.model.cryptoAds.SellAd>("sellAds")
    val sellEscrowWallet = DatabaseConnection.database.getCollection<online.pasaka.domain.model.escrow.SellEscrowWallet>("sellEscrowWallet")
    val sellOrders = DatabaseConnection.database.getCollection<online.pasaka.domain.model.order.SellOrder>("sellOrder")


}