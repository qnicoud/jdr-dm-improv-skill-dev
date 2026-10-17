package io.github.qnicoud.dmgym.app

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.time.SystemZoneClock
import io.github.qnicoud.dmgym.data.history.RoomHistoryRepository
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import io.github.qnicoud.dmgym.data.history.periodicTicks
import io.github.qnicoud.dmgym.data.settings.DataStoreSettingsRepository
import io.github.qnicoud.dmgym.data.tables.AssetTableSource
import io.github.qnicoud.dmgym.feature.exercise.ExerciseDependencies
import io.github.qnicoud.dmgym.feature.home.ExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.SettingsExerciseCatalog
import io.github.qnicoud.dmgym.platform.AndroidEndSignal
import io.github.qnicoud.dmgym.platform.SystemMonotonicClock
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
    val exerciseRegistry: ExerciseRegistry
    val exerciseDependencies: ExerciseDependencies

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

    override val exerciseRegistry: ExerciseRegistry = ExerciseRegistry.Default

    override val exerciseDependencies: ExerciseDependencies by lazy {
        ExerciseDependencies(
            tables = TableRepository(AssetTableSource(application.assets, Dispatchers.IO)),
            history = historyRepository,
            settings = settingsRepository,
            endSignal = AndroidEndSignal(application),
            monotonicClock = SystemMonotonicClock(),
            wallClock = Clock.systemDefaultZone(),
        )
    }

    private companion object {
        const val DATABASE_NAME = "dmgym.db"
    }
}
