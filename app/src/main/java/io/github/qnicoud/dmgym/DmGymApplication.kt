package io.github.qnicoud.dmgym

import android.app.Application
import io.github.qnicoud.dmgym.app.AppContainer
import io.github.qnicoud.dmgym.app.DefaultAppContainer
import io.github.qnicoud.dmgym.platform.reminder.ContainerProvider

class DmGymApplication :
    Application(),
    ContainerProvider {
    override val container: AppContainer by lazy { DefaultAppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Canal des rappels et réarmement de l'alarme (arrêt forcé, restauration : ADR 0014 §1).
        container.onAppStart()
    }
}
