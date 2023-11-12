package online.pasaka.scheduleTasks

import kotlinx.coroutines.*
import online.pasaka.scheduleTasks.orderStatus.ExpireBuyOrders
import online.pasaka.scheduleTasks.orderStatus.ExpireSellOrders
import online.pasaka.threads.Threads
import java.util.concurrent.Executors

@OptIn(DelicateCoroutinesApi::class)
suspend fun startTasksSchedulers() {
    val customDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, Threads.TASK_SCHEDULERS)
    }.asCoroutineDispatcher()

    val coroutineScope = CoroutineScope(customDispatcher)

    coroutineScope.launch {
        CurrencyUpdater.updateCurrenciesInRedis()
    }
    coroutineScope.launch { ExpireBuyOrders.updateExpiredOrders() }
    coroutineScope.launch { AutoReleaseBuyOrders.autoReleaseBuyOrders() }
    coroutineScope.launch { AutoReleaseSellOrders.autoReleaseSellOrders() }
    coroutineScope.launch {
        ExpireSellOrders.updateExpiredSellOrders()
    }


}