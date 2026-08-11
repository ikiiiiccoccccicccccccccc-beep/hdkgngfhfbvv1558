package com.example.data

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import java.util.UUID

class AppRepository(private val context: Context, private val db: AppDatabase) {

    private val activationDao = db.activationCodeDao()
    private val cheatDao = db.cheatSettingDao()
    private val prefs = context.getSharedPreferences("amino_activation_prefs", Context.MODE_PRIVATE)

    val cheatSettingsFlow: Flow<CheatSetting?> = cheatDao.getCheatSettingFlow()
    val allCodesFlow: Flow<List<ActivationCode>> = activationDao.getAllCodes()

    fun getInstallationId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (!androidId.isNullOrEmpty()) androidId else "INSTALL_ID_AMINO_FF"
    }

    fun checkSavedActivation(): ValidationResult? {
        val isActivated = prefs.getBoolean("is_activated", false)
        if (!isActivated) return null

        val expiry = prefs.getLong("expiry_ms", 0L)
        val code = prefs.getString("active_code", "") ?: ""
        val now = System.currentTimeMillis()

        if (now <= expiry) {
            val msg = if (code == "1590") {
                "تم التفعيل بنجاح باستخدام الكود القياسي 1590"
            } else {
                "مرحبًا بك، التطبيق مفعل بنجاح!"
            }
            return ValidationResult.Success(msg)
        } else {
            // Expired activation
            prefs.edit().putBoolean("is_activated", false).apply()
            return null
        }
    }

    private fun saveActivation(code: String, expiresAtMs: Long) {
        prefs.edit()
            .putBoolean("is_activated", true)
            .putString("active_code", code)
            .putLong("expiry_ms", expiresAtMs)
            .apply()
    }

    suspend fun validateCode(inputCode: String): ValidationResult {
        val trimmed = inputCode.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("يرجى إدخال كود التفعيل")
        }

        // Standard Default Code 1590 is permanently valid
        if (trimmed == "1590") {
            saveActivation("1590", Long.MAX_VALUE)
            return ValidationResult.Success("تم التفعيل بنجاح باستخدام الكود القياسي 1590")
        }

        val codeEntity = activationDao.getCode(trimmed)
            ?: return ValidationResult.Error("كود التفعيل غير صحيح")

        val now = System.currentTimeMillis()
        if (now > codeEntity.expiresAtMs) {
            return ValidationResult.Error("عذرًا، انتهت صلاحية هذا الكود (صلاحية 12 ساعة)")
        }

        val currentInstallId = getInstallationId()
        if (codeEntity.installationId.isNotEmpty() && codeEntity.installationId != currentInstallId) {
            return ValidationResult.Error("هذا الكود مرتبط بتثبيت جهاز آخر")
        }

        // Bind code to current installation if unassigned
        if (codeEntity.installationId.isEmpty()) {
            activationDao.insertCode(codeEntity.copy(isUsed = true, installationId = currentInstallId))
        }

        saveActivation(trimmed, codeEntity.expiresAtMs)
        return ValidationResult.Success("تم التفعيل بنجاح! الكود صالح لمدة 12 ساعة")
    }

    suspend fun generateNewCode(): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        val codeBuilder = StringBuilder("AMINO-")
        for (i in 0 until 8) {
            val index = random.nextInt(charPool.length)
            codeBuilder.append(charPool[index])
        }
        val generated = codeBuilder.toString()
        val now = System.currentTimeMillis()
        val twelveHours = 12 * 60 * 60 * 1000L

        val entity = ActivationCode(
            code = generated,
            createdAtMs = now,
            expiresAtMs = now + twelveHours,
            isUsed = false,
            installationId = getInstallationId()
        )
        activationDao.insertCode(entity)
        return generated
    }

    suspend fun getCheatSetting(): CheatSetting {
        return cheatDao.getCheatSetting() ?: CheatSetting().also {
            cheatDao.saveCheatSetting(it)
        }
    }

    suspend fun updateCheatSetting(setting: CheatSetting) {
        cheatDao.saveCheatSetting(setting)
    }
}

sealed class ValidationResult {
    data class Success(val message: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
