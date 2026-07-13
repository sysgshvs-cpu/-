package com.example.pubgskinpatcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val STORAGE_PERMISSION_CODE = 101
    private lateinit var injectorPath: String

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                checkCurrentPermissions()
            }
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")

        extractAssets()
    }

    private fun extractAssets() {
        // استخراج ملف الـ injector التنفيذي الحقيقي المكتوب بـ C++ وتشغيله كأداة نظام
        injectorPath = filesDir.absolutePath + "/injector"
        try {
            val injectorFile = File(injectorPath)
            if (!injectorFile.exists()) {
                val assetStream = assets.open("injector")
                val outStream = FileOutputStream(injectorFile)
                val buffer = ByteArray(1024)
                var read: Int
                while (assetStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                }
                assetStream.close()
                outStream.flush()
                outStream.close()
                Runtime.getRuntime().exec("chmod 777 $injectorPath")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkCurrentPermissions() {
        val hasStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (hasStorage) {
            webView.evaluateJavascript("javascript:updatePermissionStatus('storage', true);", null)
        }

        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        if (hasOverlay) {
            webView.evaluateJavascript("javascript:updatePermissionStatus('overlay', true);", null)
        }
    }

    inner class AndroidBridge {

        @JavascriptInterface
        fun requestRootPermission() {
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                os.writeBytes("exit\n")
                os.flush()
                val exitVal = process.waitFor()
                if (exitVal == 0) {
                    runOnUiThread {
                        webView.evaluateJavascript("javascript:updatePermissionStatus('root', true);", null)
                        Toast.makeText(this@MainActivity, "ROOT GRANTED", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "ROOT DENIED", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun requestStoragePermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivity(intent)
                }
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }

        @JavascriptInterface
        fun activateAntiCheatBypass() {
            try {
                // تصفية جدار الحماية وحظر خوادم الفحص بشكل حقيقي
                executeShellCommand("su -c 'iptables -F'")
                executeShellCommand("su -c 'iptables -A OUTPUT -d telemetry.pubgmobile.com -j REJECT'")
                executeShellCommand("su -c 'iptables -A OUTPUT -d file.igamecj.com -j REJECT'")
                executeShellCommand("su -c 'iptables -A OUTPUT -d cs.mbgame.anticheatexpert.com -j REJECT'")
                executeShellCommand("su -c 'setenforce 0'")
                runOnUiThread {
                    webView.evaluateJavascript("javascript:updatePermissionStatus('bypass', true);", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JavascriptInterface
        fun injectSkin(offsetAddress: String, skinValue: String) {
            val pid = getGamePID("com.tencent.ig")
            if (pid != "0") {
                // استدعاء أداة الـ C++ وتمرير الـ PID الحقيقي وعناوين الذاكرة والقيم لتعديل الذاكرة فوراً
                val cmd = "su -c '$injectorPath -p $pid -a $offsetAddress -v $skinValue'"
                val result = executeShellCommand(cmd)
                runOnUiThread {
                    webView.evaluateJavascript("javascript:appendLog('Inject Result: ${result.trim()}', 'sys');", null)
                }
            } else {
                runOnUiThread {
                    webView.evaluateJavascript("javascript:appendLog('Error: Game Process Not Found (com.tencent.ig is not running)', 'error');", null)
                }
            }
        }

        @JavascriptInterface
        fun executeShell(command: String): String {
            return executeShellCommand(command)
        }
    }

    private fun getGamePID(packageName: String): String {
        return try {
            val process = Runtime.getRuntime().exec("pidof $packageName")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val pid = reader.readLine()?.trim() ?: "0"
            reader.close()
            if (pid.isEmpty()) "0" else pid
        } catch (e: Exception) {
            "0"
        }
    }

    private fun executeShellCommand(command: String): String {
        val output = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
        } catch (e: Exception) {
            return e.message ?: "Error"
        }
        return if (output.isEmpty()) "Success" else output.toString()
    }
}
