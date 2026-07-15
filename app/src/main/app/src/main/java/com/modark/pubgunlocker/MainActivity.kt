package com.modark.pubgunlocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.modark.pubgunlocker.core.SkinInjector
import com.modark.pubgunlocker.utils.PermissionHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var injector: SkinInjector
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var unlockButton: Button
    private lateinit var resetButton: Button

    private val PERMISSION_REQUEST_CODE = 100
    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        injector = SkinInjector(this)
        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)
        unlockButton = findViewById(R.id.unlockButton)
        resetButton = findViewById(R.id.resetButton)

        checkAndRequestPermissions()

        permissionButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        unlockButton.setOnClickListener {
            unlockAllSkins()
        }

        resetButton.setOnClickListener {
            resetAllSkins()
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onPermissionsGranted()
            } else {
                showPermissionDialog()
            }
        } else {
            val writePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val readPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            
            if (writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED) {
                onPermissionsGranted()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("مطلوب صلاحية الوصول للملفات")
            .setMessage("يحتاج التطبيق للوصول إلى وحدة التخزين لتعديل ملفات اللعبة وتفعيل السكنات. يرجى تفعيل الخيار في الشاشة التالية.")
            .setPositiveButton("موافق") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE)
                }
            }
            .setNegativeButton("إلغاء") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "تم رفض الصلاحية، لن يعمل التطبيق بشكل صحيح", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionsGranted()
            } else {
                statusText.text = "تم رفض الصلاحيات"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    onPermissionsGranted()
                } else {
                    statusText.text = "لم يتم منح الصلاحية الكاملة"
                    statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                }
            }
        }
    }

    private fun onPermissionsGranted() {
        statusText.text = "تم منح الصلاحيات بنجاح!"
        statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        permissionButton.visibility = Button.GONE
        unlockButton.isEnabled = true
        resetButton.isEnabled = true
    }

    private fun unlockAllSkins() {
        if (checkPermissions()) {
            unlockButton.isEnabled = false
            statusText.text = "جاري فتح السكنات وتعديل الملفات..."
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            
            val result = injector.unlockAll()
            if (result) {
                statusText.text = "تم التعديل وفتح السكنات بنجاح!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                Toast.makeText(this, "تم التعديل بنجاح!", Toast.LENGTH_SHORT).show()
            } else {
                statusText.text = "فشل التعديل! تأكد من وجود ملفات اللعبة."
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                Toast.makeText(this, "حدث خطأ أثناء التعديل", Toast.LENGTH_SHORT).show()
            }
            unlockButton.isEnabled = true
        }
    }

    private fun resetAllSkins() {
        if (checkPermissions()) {
            val result = injector.resetAll()
            if (result) {
                statusText.text = "تم استعادة الملفات الأصلية!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                Toast.makeText(this, "تمت الاستعادة بنجاح", Toast.LENGTH_SHORT).show()
            } else {
                statusText.text = "فشل الاستعادة!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            write == PackageManager.PERMISSION_GRANTED
        }
    }
}
