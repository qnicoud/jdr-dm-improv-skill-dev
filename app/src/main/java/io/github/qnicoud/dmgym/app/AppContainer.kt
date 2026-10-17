package io.github.qnicoud.dmgym.app

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.time.SystemZoneClock
import io.github.qnicoud.dmgym.data.history.RoomHistoryRepository
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import io.github.qnicoud.dmgym.data.history.periodicTicks
import io.github.qnicoud.dmgym.data.settings.DataStoreSettingsRepository
import io.github.qnicoud.dmgym.feature.home.ExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.SettingsExerciseCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import java.time.Duration

/**
 * Injection manuelle (ADR 0005). Seul endroit qui connaît les implémentations ; les classes métier
 * reçoivent leurs dépendances par le constructeur. Les ViewModels sont construits dans les `entry<>` de navigation.
 */
interface AppContainer {
    val settingsRepository: SettingsRepository
    val historyRepository: HistoryRepository
    val exerciseCatalog: ExerciseCatalog

    /** Horloge civile du téléphone : le jour (`localEpochDay`) bascule à minuit local (D16). */
    val clock: Clock
}

/** Ne retient que l'[Application] (jamais une Activity). */
class DefaultAppContainer(
    private val application: Application,
) : AppContainer {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val clock: Clock = SystemZoneClock()

    override val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(
            dataStore = DataStoreSettingsRepository.createDataStore(application, appScope),
            durationDefaults = ExerciseId.DEFAULT_DURATIONS,
            untimedByDefault = ExerciseId.UNTIMED_BY_DEFAULT,
        )
    }

    private val database: DmGymDatabase by lazy {
        Room
            .databaseBuilder<DmGymDatabase>(application, application.getDatabasePath(DATABASE_NAME).absolutePath)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    override val historyRepository: HistoryRepository by lazy {
        RoomHistoryRepository(database.sessionDao(), clock, periodicTicks(Duration.ofMinutes(1)))
    }

    override val exerciseCatalog: ExerciseCatalog by lazy { SettingsExerciseCatalog(settingsRepository) }

    private companion object {
        const val DATABASE_NAME = "dmgym.db"
    }
}
