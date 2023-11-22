package online.pasaka.infrastructure.threads

import kotlinx.coroutines.*
import java.util.concurrent.Executors

object Threads {
    const val TASK_SCHEDULERS = "TaskSchedulers"
    const val CONSUMERS = "Consumers"
    const val SSE_THREAD = "SSE_THREAD"
}

