package online.pasaka.scheduleTasks

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun startTasksSchedulers(){
    coroutineScope {
        launch {
            CurrencyUpdater.updateCurrenciesInRedis()
        }
    }

}