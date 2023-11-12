package online.pasaka.database

import com.mongodb.client.MongoDatabase
import online.pasaka.model.cryptoAds.BuyAd
import online.pasaka.model.cryptoAds.SellAd
import online.pasaka.model.escrow.BuyEscrowWallet
import online.pasaka.model.escrow.SellEscrowWallet
import online.pasaka.model.merchant.Merchant
import online.pasaka.model.merchant.wallet.MerchantAdsHistory
import online.pasaka.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.model.merchant.wallet.MerchantWallet
import online.pasaka.model.merchant.wallet.MerchantsWithdrawalsHistory
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.SellOrder
import online.pasaka.model.user.User
import online.pasaka.model.wallet.Wallet
import org.litote.kmongo.getCollection

object Entries {
    /** Database Instance*/
    private val database: MongoDatabase = DatabaseConnection.database

    /** User collection Instances*/
    val dbUser = DatabaseConnection.database.getCollection<User>()
    val userWallet = database.getCollection<Wallet>()

    /** Merchant collection Instances*/
    val dbMerchant = DatabaseConnection.database.getCollection<Merchant>()
    val dbMerchantWallet = DatabaseConnection.database.getCollection<MerchantWallet>()
    val dbMerchantsWithdrawalsHistory = DatabaseConnection.database.getCollection<MerchantsWithdrawalsHistory>()
    val dbMerchantTopUpsHistory = DatabaseConnection.database.getCollection<MerchantTopUpsHistory>()
    val dbCreateSellAd = DatabaseConnection.database.getCollection<SellAd>("sellAds")
    val merchantAdHistory = DatabaseConnection.database.getCollection<MerchantAdsHistory>("AdsHistory")

    /** Crypto Buy Order collection instances*/
    val buyEscrowWallet = DatabaseConnection.database.getCollection<BuyEscrowWallet>("buyEscrowWallet")
    val cryptoBuyOrders = DatabaseConnection.database.getCollection<BuyOrder>("buyOrders")
    val buyAd = DatabaseConnection.database.getCollection<BuyAd>("buyAds")
    val buyersWallet = DatabaseConnection.database.getCollection<Wallet>()

    /** Crypto Sell Order collection instances */
    val sellAd = DatabaseConnection.database.getCollection<SellAd>("sellAds")
    val sellEscrowWallet = DatabaseConnection.database.getCollection<SellEscrowWallet>("sellEscrowWallet")
    val sellOrders = DatabaseConnection.database.getCollection<SellOrder>("sellOrder")


}