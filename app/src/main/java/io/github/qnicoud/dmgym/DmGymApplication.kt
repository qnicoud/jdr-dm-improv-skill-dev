package io.github.qnicoud.dmgym

import android.app.Application
import io.github.qnicoud.dmgym.app.AppContainer
import io.github.qnicoud.dmgym.app.DefaultAppContainer

class DmGymApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer() }
}
