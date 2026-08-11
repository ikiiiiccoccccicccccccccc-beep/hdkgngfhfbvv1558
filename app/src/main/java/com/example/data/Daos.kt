package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivationCodeDao {
    @Query("SELECT * FROM activation_codes WHERE code = :code LIMIT 1")
    suspend fun getCode(code: String): ActivationCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: ActivationCode)

    @Query("SELECT * FROM activation_codes ORDER BY createdAtMs DESC")
    fun getAllCodes(): Flow<List<ActivationCode>>
}

@Dao
interface CheatSettingDao {
    @Query("SELECT * FROM cheat_settings WHERE id = 1 LIMIT 1")
    fun getCheatSettingFlow(): Flow<CheatSetting?>

    @Query("SELECT * FROM cheat_settings WHERE id = 1 LIMIT 1")
    suspend fun getCheatSetting(): CheatSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCheatSetting(setting: CheatSetting)
}
