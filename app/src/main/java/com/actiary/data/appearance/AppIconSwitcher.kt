package com.actiary.data.appearance

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

private const val GLASS_LAUNCHER_ALIAS = "com.actiary.MainActivityGlass"
private const val P5_LAUNCHER_ALIAS = "com.actiary.MainActivityP5"

fun isP5IconEnabled(context: Context): Boolean {
    val pm = context.packageManager
    val p5Component = ComponentName(context, P5_LAUNCHER_ALIAS)
    return pm.getComponentEnabledSetting(p5Component) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
}

fun applyAppIcon(context: Context, useP5Icon: Boolean) {
    val pm = context.packageManager
    val targetComponent = ComponentName(context, if (useP5Icon) P5_LAUNCHER_ALIAS else GLASS_LAUNCHER_ALIAS)
    val previousComponent = ComponentName(context, if (useP5Icon) GLASS_LAUNCHER_ALIAS else P5_LAUNCHER_ALIAS)
    // Enable the target launcher alias before disabling the previous one so the launcher never drops the entry.
    pm.setComponentEnabledSetting(
        targetComponent,
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
    )
    pm.setComponentEnabledSetting(
        previousComponent,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
}
