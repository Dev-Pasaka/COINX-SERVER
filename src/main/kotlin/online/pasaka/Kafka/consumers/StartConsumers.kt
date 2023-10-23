package online.pasaka.Kafka.consumers

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun startConsumers() {

    coroutineScope {

        launch { merchantTopUpConsumer() }
        launch { merchantWithdrawalConsumer() }
        launch { cryptoBuyOrderConsumer() }
        launch { emailNotificationConsumer() }
    }
}