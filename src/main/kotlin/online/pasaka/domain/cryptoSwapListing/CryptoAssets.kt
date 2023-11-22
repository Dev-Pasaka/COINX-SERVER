package online.pasaka.domain.cryptoSwapListing

object CryptoAssets {
    val swappableCryptoAssets = listOf(
        CryptoListing(name = "Bitcoin", symbol = "BTC" ),
        CryptoListing(name = "Ethereum", symbol = "ETH" ),
        CryptoListing(name = "Tether", symbol = "USDT" ),
        CryptoListing(name = "BNB", symbol = "BNB" ),
        CryptoListing(name = "Cardano", symbol = "ADA" ),
        CryptoListing(name = "Dogecoin", symbol = "DOGE" ),
        CryptoListing(name = "Solana", symbol = "SOL" ),
        CryptoListing(name = "TRON", symbol = "TRX" ),
        CryptoListing(name = "Toncoin", symbol = "TON" ),
        CryptoListing(name = "Dai", symbol = "DAI" ),
        CryptoListing(name = "Polygon", symbol = "MATIC" ),
        CryptoListing(name = "Litecoin", symbol = "LTC" ),
        CryptoListing(name = "Bitcoin Cash", symbol = "BCH" ),
        CryptoListing(name = "Wrapped Bitcoin", symbol = "WBTC" ),
        CryptoListing(name = "Shiba Inu", symbol = "SHIB" ),
        CryptoListing(name = "Chainlink", symbol = "LINK" ),
        CryptoListing(name = "UNUS SED LEO", symbol = "LEO" ),
        CryptoListing(name = "Avalanche", symbol = "AVAX" ),
        CryptoListing(name = "Stellar", symbol = "XLM" ),
        CryptoListing(name = "Monero", symbol = "XMR" ),
    )
}

data class CryptoListing(
    val name:String,
    val symbol:String
)