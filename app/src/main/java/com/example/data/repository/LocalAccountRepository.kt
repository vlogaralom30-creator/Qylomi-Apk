package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SavedAccount
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class LocalAccountRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("qylomi_accounts_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SavedAccount::class.java)
    private val jsonAdapter = moshi.adapter<List<SavedAccount>>(listType)

    fun getSavedAccounts(): List<SavedAccount> {
        val json = prefs.getString("saved_accounts_json", null) ?: return emptyList()
        return try {
            jsonAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAccount(account: SavedAccount) {
        val current = getSavedAccounts().toMutableList()
        val index = current.indexOfFirst { it.email.equals(account.email, ignoreCase = true) }
        if (index >= 0) {
            current[index] = account
        } else {
            current.add(account)
        }
        val json = jsonAdapter.toJson(current)
        prefs.edit().putString("saved_accounts_json", json).apply()
        setActiveAccount(account.email)
    }

    fun removeAccount(email: String) {
        val current = getSavedAccounts().toMutableList()
        current.removeAll { it.email.equals(email, ignoreCase = true) }
        val json = jsonAdapter.toJson(current)
        prefs.edit().putString("saved_accounts_json", json).apply()
        
        if (getActiveAccountEmail()?.equals(email, ignoreCase = true) == true) {
            val nextAccount = current.firstOrNull()
            if (nextAccount != null) {
                setActiveAccount(nextAccount.email)
            } else {
                clearActiveAccount()
            }
        }
    }

    fun getActiveAccountEmail(): String? {
        return prefs.getString("active_account_email", null)
    }

    fun getActiveAccount(): SavedAccount? {
        val activeEmail = getActiveAccountEmail() ?: return null
        return getSavedAccounts().find { it.email.equals(activeEmail, ignoreCase = true) }
    }

    fun setActiveAccount(email: String) {
        prefs.edit().putString("active_account_email", email).apply()
    }

    fun clearActiveAccount() {
        prefs.edit().remove("active_account_email").apply()
    }

    fun setGuestMode(isGuest: Boolean) {
        prefs.edit().putBoolean("is_guest_mode", isGuest).apply()
        if (isGuest) {
            clearActiveAccount()
        }
    }

    fun isGuestMode(): Boolean {
        return prefs.getBoolean("is_guest_mode", false)
    }

    companion object {
        fun maskPassword(password: String): String {
            if (password.isBlank()) return "****"
            return "•".repeat(minOf(password.length, 8))
        }
    }
}
