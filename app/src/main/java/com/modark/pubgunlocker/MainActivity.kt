package com.modark.pubgunlocker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.modark.pubgunlocker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPermission.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnUnlock.setOnClickListener {
            Toast.makeText(this, "جاري فتح السكنات والسيارات...", Toast.LENGTH_SHORT).show()
        }

        binding.btnReset.setOnClickListener {
            Toast.makeText(this, "جاري استعادة الملفات الأصلية...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                binding.tvStatus.text = "تم منح الصلاحيات بنجاح"
            }
        } else {
            binding.tvStatus.text = "تم منح الصلاحيات بنجاح"
        }
    }
}
