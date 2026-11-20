package com.actiary.data.photo

import android.content.Context
import android.net.Uri
import com.actiary.domain.RecognizedLine
import com.actiary.domain.parseHuaweiWorkout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal suspend fun recognizeWorkoutScreenshot(context: Context, uri: String) =
    withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, Uri.parse(uri))
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        try {
            val lines = suspendCancellableCoroutine { continuation ->
                recognizer.process(image).addOnCompleteListener { task ->
                    when {
                        task.isCanceled -> continuation.cancel()
                        task.isSuccessful -> continuation.resumeWith(Result.success(task.result.textBlocks.flatMap { it.lines }))
                        else -> continuation.resumeWith(Result.failure(checkNotNull(task.exception)))
                    }
                }
            }
            parseHuaweiWorkout(lines.mapNotNull { line ->
                line.boundingBox?.let { box ->
                    RecognizedLine(line.text, box.left, box.top, box.right, box.bottom)
                }
            })
        } finally {
            recognizer.close()
        }
    }
