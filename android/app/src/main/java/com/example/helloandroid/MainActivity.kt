package com.example.helloandroid

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.helloandroid.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val accumulatedContent = StringBuilder()
    private var time: Float = 0f
    private var printPath: String = ""

    companion object {
        private var number: Int = 0
        init {
            System.loadLibrary("helloandroid")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sampleText.text = stringFromJNI()

        if (!intent.hasExtra("arg1")) return
        val arg1String = intent.getStringExtra("arg1") ?: "0"
        time = arg1String.toFloatOrNull() ?: 0f
        printPath = intent.getStringExtra("arg2") ?: ""

        val referrer = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
        val callingPackage = callingPackage

        if (!referrer.isNullOrEmpty()) {
            accumulateContent("Opened by app (via referrer): $referrer")
        } else {
            if (callingPackage != null) {
                accumulateContent("Opened by calling app (via activity): $callingPackage")
            } else {
                if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
                    accumulateContent("Opened by service or new task")
                } else {
                    accumulateContent("Opened directly or unable to detect caller")
                }
            }
        }

        val extras = intent.extras
        if (extras != null) {
            val inputArgs = StringBuilder("Input Args:\n")
            var hasExtras = false

            for (key in extras.keySet()) {
                val value = extras.get(key)
                if (value is String) {
                    inputArgs.append("$key = $value\n")
                    hasExtras = true
                }
            }

            if (hasExtras) {
                inputArgs.setLength(inputArgs.length - 1)
                accumulateContent(inputArgs.toString())
            } else {
                accumulateContent("No string extras found")
            }
        }

        if (intent.hasExtra("pending_result_intent")) {
            doTask()
        }

        saveArgsToFile()
    }

    private fun doTask() {
        if (time <= 0f) {
            accumulateContent("No task!")
            sendResultToCallerApp()
            finish()
            return
        }
        accumulateContent("task time: $time")
        Handler(Looper.getMainLooper()).postDelayed({
            sendResultToCallerApp()
            finish()
        }, (time * 1000).toLong())
    }

    private fun accumulateContent(content: String) {
        accumulatedContent.append(content).append("\n")
    }

    private fun saveArgsToFile() {
        if (printPath.isEmpty()) return
        val file = File(printPath)
        file.parentFile?.mkdirs()
        try {
            val outputStream = FileOutputStream(file, false)
            outputStream.write(accumulatedContent.toString().toByteArray())
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun sendResultToCallerApp() {
        ++number
        val pendingResultIntent =
            intent.getParcelableExtra<PendingIntent>("pending_result_intent") ?: return

        val resultData = Intent().apply {
            putExtra(
                "extra_result_data",
                "Hi caller, external task $number completed successfully!"
            )
        }

        try {
            pendingResultIntent.send(this, Activity.RESULT_OK, resultData)
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
        }
    }

    external fun stringFromJNI(): String
}
