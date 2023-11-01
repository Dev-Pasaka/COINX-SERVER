package online.pasaka.scheduleTasks

import kotlinx.coroutines.*
import online.pasaka.scheduleTasks.orderStatus.ExpiredOrders
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
    coroutineScope.launch {
        ExpiredOrders.updateExpiredOrders()
    }
    coroutineScope.launch {
        AutoRelease.autoReleaseBuyOrders()
    }


}