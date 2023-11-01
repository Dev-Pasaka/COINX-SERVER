package online.pasaka.Kafka.consumers

import kotlinx.coroutines.*
import online.pasaka.threads.Threads
import java.util.concurrent.Executors

@OptIn(DelicateCoroutinesApi::class)
suspend fun startConsumers() {

    val customDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, Threads.CONSUMERS)
    }.asCoroutineDispatcher()

    val coroutineScope = CoroutineScope(customDispatcher)
    coroutineScope.launch {
        launch { merchantTopUpConsumer() }
        launch { merchantWithdrawalConsumer() }
        launch {
            cryptoBuyOrderConsumer()
            cryptoBuyOrderConsumer()
        }
        launch {
            emailNotificationConsumer()
            emailNotificationConsumer()
        }
    }
}