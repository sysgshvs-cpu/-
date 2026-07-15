package com.modark.pubgunlocker.models

data class SkinData(
    val id: String,
    val name: String,
    val category: String,
    val rarity: String,
    val season: Int,
    val colorVariants: List<String>,
    val offset: Int,
    val pakName: String,
    val originalCode: String,
    val unlockCode: String
)

data class UnlockConfig(
    val version: String,
    val skins: List<SkinData>,
    val vehicles: List<VehicleData>,
    val outfits: List<OutfitData>
)

data class VehicleData(
    val id: String,
    val name: String,
    val skinName: String,
    val colorVariants: List<String>,
    val offset: Int,
    val pakName: String,
    val originalCode: String,
    val unlockCode: String
)

data class OutfitData(
    val id: String,
    val name: String,
    val colorVariants: List<String>,
    val offset: Int,
    val pakName: String,
    val originalCode: String,
    val unlockCode: String
)
