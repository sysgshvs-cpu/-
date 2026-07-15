package com.modark.pubgunlocker.models

data class SkinData(
    val id: String,
    val name: String,
    val category: String, // مثل: "weapons", "vehicles", "outfits"
    val sourcePath: String, // مسار الملف المعدل داخل التطبيق
    val targetPath: String  // المسار المستهدف داخل ملفات اللعبة (Android/data)
)
