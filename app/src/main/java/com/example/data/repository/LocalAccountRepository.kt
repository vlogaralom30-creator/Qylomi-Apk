package com.example.data.repository

import android.content.Context
import com.example.data.api.SessionStore
import com.example.data.model.SavedAccount
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class LocalAccountRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("qylomi_saved_accounts", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SavedAccount::class.java)
    private val adapter = moshi.adapter<List<SavedAccount>>(listType)

    fun getSavedAccounts(): List<SavedAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAccount(account: SavedAccount) {
        val current = getSavedAccounts().toMutableList()
        current.removeAll { it.email.equals(account.email, ignoreCase = true) }
        current.add(0, account)
        val capped = current.take(5)
        prefs.edit().putString(KEY_ACCOUNTS, adapter.toJson(capped)).apply()
    }

    fun removeAccount(account: SavedAccount) {
        val current = getSavedAccounts().toMutableList()
        current.removeAll { it.email.equals(account.email, ignoreCase = true) }
        prefs.edit().putString(KEY_ACCOUNTS, adapter.toJson(current)).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_ACCOUNTS).apply()
    }

    companion object {
        private const val KEY_ACCOUNTS = "saved_accounts_list"
    }
}
