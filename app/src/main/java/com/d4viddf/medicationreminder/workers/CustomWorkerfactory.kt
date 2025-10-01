package com.d4viddf.medicationreminder.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.di.WorkerDependenciesEntryPoint // IMPORTA TU ENTRYPOINT
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject // Necesario para el constructor de CustomWorkerFasctory si Hilt lo va a crear

// Si Hilt va a crear esta instancia de CustomWorkerFasctory para inyectarla
// en Application, este constructor debe ser simple o tener dependencias que Hilt pueda proveer.
// Para máxima flexibilidad con el EntryPoint, el constructor puede no necesitar nada,
// ya que las dependencias de los WORKERS se obtienen a través del EntryPoint.
class CustomWorkerFactory @Inject constructor() : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        // Accede al EntryPoint usando el appContext
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WorkerDependenciesEntryPoint::class.java
        )

        return when (workerClassName) {
            ReminderSchedulingWorker::class.java.name -> {
                ReminderSchedulingWorker(
                    appContext,
                    workerParameters,
                    hiltEntryPoint.medicationRepository(),
                    hiltEntryPoint.medicationScheduleRepository(),
                    hiltEntryPoint.medicationReminderRepository(),
                    hiltEntryPoint.medicationDosageRepository(),
                    hiltEntryPoint.notificationScheduler()
                )
            }
            else -> {
                // Opcional: Lanza una excepción si es un worker desconocido que esperabas manejar
                // throw IllegalArgumentException("Unknown worker class name: $workerClassName")
                // O retorna null para que WorkManager intente con otro factory (si hubiera) o falle.
                null
            }
        }
    }
}