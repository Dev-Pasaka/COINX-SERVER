package online.pasaka.domain.repository.cache

interface RedisRepository {

    suspend fun setData(key:String, data:Any, expiresAt:Long = 120):Boolean
    suspend fun getData(key:String): String?
    suspend fun delData(key:String):Boolean


}