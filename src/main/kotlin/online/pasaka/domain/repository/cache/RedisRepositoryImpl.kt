package online.pasaka.domain.repository.cache

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.infrastructure.config.Redis
import redis.clients.jedis.Jedis

class RedisRepositoryImpl(redisUrl:String = Redis.REDIS_CLOUD_URL, redisPort:String = Redis.REDIS_PORT):RedisRepository {
    val gson = Gson()
    private val jedis  =  Jedis(redisUrl)

    override suspend fun setData(key: String, data: Any, expiresAt:Long): Boolean {
        return coroutineScope {
            try {
                async(Dispatchers.IO) {
                    jedis.set(key,gson.toJson(data))
                    jedis.expire(key, expiresAt)
                }.await()
                true
            }catch (e:Exception){false}
            finally { jedis.close()}
        }

    }

    override suspend fun getData(key: String): String? {
        return coroutineScope {
            try {
                async(Dispatchers.IO) {jedis.get(key) }.await()
            } catch (e: Exception) {
                null
            } finally {
                jedis.close()
            }
        }
    }

    override suspend fun delData(key: String): Boolean {
        return coroutineScope {
            try {
                async(Dispatchers.IO) { jedis.del(key) }.await()
                true
            }catch (e:Exception){ false }
            finally { jedis.close()}
        }

    }
}

