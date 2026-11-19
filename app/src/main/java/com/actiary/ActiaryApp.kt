package com.actiary

import android.app.Application

class ActiaryApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
