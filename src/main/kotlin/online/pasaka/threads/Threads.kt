package online.pasaka.threads

import kotlinx.coroutines.*
import java.util.concurrent.Executors

object Threads {
    const val TASK_SCHEDULERS = "TaskSchedulers"
    const val CONSUMERS = "Consumers"
    const val SSE_THREAD = "SSE_THREAD"
}

@OptIn(DelicateCoroutinesApi::class)
fun main(){
    val customDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, Threads.CONSUMERS)
    }.asCoroutineDispatcher()

    val coroutineScope = CoroutineScope(customDispatcher)

    coroutineScope.launch {
        println("Custom thread name ${Thread.currentThread().name}")
    }
    println("Main thread")
}