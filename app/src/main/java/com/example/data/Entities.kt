package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activation_codes")
data class ActivationCode(
    @PrimaryKey val code: String,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val isUsed: Boolean = false,
    val installationId: String = ""
)

@Entity(tableName = "cheat_settings")
data class CheatSetting(
    @PrimaryKey val id: Int = 1,
    val headshotOnly: Boolean = true,
    val espLocations: Boolean = true,
    val wallHack: Boolean = false,
    val freezeEnemy: Boolean = false,
    val autoKill: Boolean = false,
    val menuActive: Boolean = true
)
