package daily

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class DailyTaskExecutor(dailyTask: DailyTask) {
    private val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val dailyTask: DailyTask

    fun startExecutionAt(targetHour: Int, targetMin: Int, targetSec: Int) {
        val taskWrapper = Runnable {
            dailyTask.execute()
            startExecutionAt(targetHour, targetMin, targetSec)
        }
        val delay = computeNextDelay(targetHour, targetMin, targetSec)
        executorService.schedule(taskWrapper, delay, TimeUnit.SECONDS)
    }

    private fun computeNextDelay(targetHour: Int, targetMin: Int, targetSec: Int): Long {
        val localNow = LocalDateTime.now()
        val currentZone = ZoneId.systemDefault()
        val zonedNow = ZonedDateTime.of(localNow, currentZone)
        var zonedNextTarget = zonedNow.withHour(targetHour).withMinute(targetMin).withSecond(targetSec)
        if (zonedNow >= zonedNextTarget) zonedNextTarget = zonedNextTarget.plusDays(1)
        val duration: Duration = Duration.between(zonedNow, zonedNextTarget)
        return duration.seconds
    }

    fun stop() {
        executorService.shutdown()
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS)
        } catch (e: InterruptedException) {
            System.err.println("Something went wrong when trying to stop DailyTaskExecutor")
            e.printStackTrace()
        }
    }

    init {
        this.dailyTask = dailyTask
    }
}