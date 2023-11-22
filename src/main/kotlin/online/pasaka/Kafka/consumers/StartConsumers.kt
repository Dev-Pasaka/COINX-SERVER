package online.pasaka.Kafka.consumers

import kotlinx.coroutines.*
import online.pasaka.infrastructure.threads.Threads
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
        launch { buyOrderConsumer() }
        launch { sellOrderConsumer() }
        launch { EmailNotificationConsumer() }
    }
}