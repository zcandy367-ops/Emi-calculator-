package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CurrencyOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "emi_pulse_prefs")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

data class UserPreferences(
    val currency: CurrencyOption,
    val themeMode: ThemeMode,
    val useDeviceLocaleSeparators: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DEVICE_LOCALE_SEPARATORS = booleanPreferencesKey("use_device_locale_separators")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val defaultCurrency = detectDefaultCurrency()
        val currency = CurrencyOption.fromCode(prefs[Keys.CURRENCY_CODE] ?: defaultCurrency.code)
        val themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE])
        val useDeviceLocale = prefs[Keys.USE_DEVICE_LOCALE_SEPARATORS] ?: false
        UserPreferences(
            currency = currency,
            themeMode = themeMode,
            useDeviceLocaleSeparators = useDeviceLocale
        )
    }

    suspend fun setCurrency(currency: CurrencyOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CODE] = currency.code
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setUseDeviceLocaleSeparators(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_DEVICE_LOCALE_SEPARATORS] = enabled
        }
    }

    private fun detectDefaultCurrency(): CurrencyOption {
        val country = Locale.getDefault().country.uppercase(Locale.ROOT)
        return when (country) {
            "IN" -> CurrencyOption.INR
            "GB" -> CurrencyOption.GBP
            "JP" -> CurrencyOption.JPY
            "CH" -> CurrencyOption.CHF
            "BR" -> CurrencyOption.BRL
            "AE" -> CurrencyOption.AED
            "SG" -> CurrencyOption.SGD
            "CA" -> CurrencyOption.CAD
            "AU" -> CurrencyOption.AUD
            "DE", "FR", "IT", "ES", "NL", "IE", "PT", "BE", "AT", "FI" -> CurrencyOption.EUR
            else -> CurrencyOption.USD
        }
    }
}
