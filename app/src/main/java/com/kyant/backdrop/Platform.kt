// Modified for Actiary: merged expect/actual into plain Android functions; isRenderEffectSupported always true (minSdk 31)
package com.kyant.backdrop

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

fun isRenderEffectSupported(): Boolean = true

@ChecksSdkIntAtLeast(Build.VERSION_CODES.TIRAMISU)
fun isRuntimeShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
