package online.pasaka.database

import com.mongodb.client.MongoDatabase
import online.pasaka.model.cryptoAds.CreateCryptoBuyAd
import online.pasaka.model.cryptoAds.CreateCryptoSellAd
import online.pasaka.model.escrow.BuyEscrowWallet
import online.pasaka.model.merchant.Merchant
import online.pasaka.model.merchant.wallet.MerchantAdsHistory
import online.pasaka.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.model.merchant.wallet.MerchantWallet
import online.pasaka.model.merchant.wallet.MerchantsWithdrawalsHistory
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.user.User
import online.pasaka.model.wallet.Wallet
import org.litote.kmongo.getCollection

object Entries {
    /** Database Instance*/
    private val database: MongoDatabase = DatabaseConnection.database

    /** User collection Instances*/
    val dbUser = DatabaseConnection.database.getCollection<User>()
    val dbTraderWallet = database.getCollection<Wallet>()

    /** Merchant collection Instances*/
    val dbMerchant = DatabaseConnection.database.getCollection<Merchant>()
    val dbMerchantWallet = DatabaseConnection.database.getCollection<MerchantWallet>()
    val dbMerchantsWithdrawalsHistory = DatabaseConnection.database.getCollection<MerchantsWithdrawalsHistory>()
    val dbMerchantTopUpsHistory = DatabaseConnection.database.getCollection<MerchantTopUpsHistory>()
    val dbCreateCreateCryptoBuyAd = DatabaseConnection.database.getCollection<CreateCryptoBuyAd>("buyAds")
    val dbCreateSellAd = DatabaseConnection.database.getCollection<CreateCryptoSellAd>("sellAds")
    val merchantAdHistory = DatabaseConnection.database.getCollection<MerchantAdsHistory>("AdsHistory")

    /** Crypto Buy Order collection instances*/
    val buyEscrowWallet = DatabaseConnection.database.getCollection<BuyEscrowWallet>("escrowWallet")
    val cryptoBuyOrders = DatabaseConnection.database.getCollection<BuyOrder>("buyOrders")
    val cryptoBuyAd = DatabaseConnection.database.getCollection<CreateCryptoBuyAd>("buyAds")
    val merchantCryptoBuyAd = DatabaseConnection.database.getCollection<CreateCryptoBuyAd>("buyAds")
    val buyersWallet = DatabaseConnection.database.getCollection<Wallet>()


}