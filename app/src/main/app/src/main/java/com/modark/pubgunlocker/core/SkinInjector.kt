package com.modark.pubgunlocker.core

import android.content.Context
import com.google.gson.Gson
import com.modark.pubgunlocker.models.UnlockConfig
import com.modark.pubgunlocker.models.SkinData
import com.modark.pubgunlocker.utils.FileUtils
import java.io.File

class SkinInjector(private val context: Context) {

    private val gson = Gson()
    private val fileUtils = FileUtils(context)

    fun loadConfig(): UnlockConfig? {
        return try {
            val json = context.assets.open("skin_data.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, UnlockConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun unlockAll(): Boolean {
        val config = loadConfig() ?: return false
        val gamePath = fileUtils.findPubgPath() ?: return false

        return try {
            config.skins.forEach { skin ->
                fileUtils.modifyPakFile(gamePath, skin)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resetAll(): Boolean {
        val gamePath = fileUtils.findPubgPath() ?: return false
        return fileUtils.restoreOriginal(gamePath)
    }
}
