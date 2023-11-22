package online.pasaka.infrastructure.scheduleTasks

import kotlinx.coroutines.*
import online.pasaka.infrastructure.scheduleTasks.orderStatus.ExpireBuyOrders
import online.pasaka.infrastructure.scheduleTasks.orderStatus.ExpireSellOrders
import online.pasaka.infrastructure.threads.Threads
import java.util.concurrent.Executors

suspend fun startTasksSchedulers() {
    val customDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, Threads.TASK_SCHEDULERS)
    }.asCoroutineDispatcher()

    val coroutineScope = CoroutineScope(customDispatcher)

    coroutineScope.launch { ExpireBuyOrders.updateExpiredOrders() }
    coroutineScope.launch { AutoReleaseBuyOrders.autoReleaseBuyOrders() }
    coroutineScope.launch { AutoReleaseSellOrders.autoReleaseSellOrders() }
    coroutineScope.launch { ExpireSellOrders.updateExpiredSellOrders() }


}