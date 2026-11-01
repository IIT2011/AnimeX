package com.animex.app.core.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.animex.app.core.network.model.UserGoogleAccount

object GoogleAuthManager {

    private const val PREFS_NAME = "animex_google_auth"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_EMAIL = "google_email"
    private const val KEY_NAME = "google_display_name"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"

    /**
     * Inspects device Google accounts configured in Android Settings.
     * Returns a list of real email addresses found on the device.
     */
    fun getAvailableDeviceGoogleAccounts(context: Context): List<String> {
        val detectedAccounts = mutableListOf<String>()
        try {
            val accountManager = AccountManager.get(context)
            val accounts: Array<Account> = accountManager.getAccountsByType("com.google")
            for (acc in accounts) {
                if (!acc.name.isNullOrBlank() && acc.name.contains("@")) {
                    detectedAccounts.add(acc.name)
                }
            }
        } catch (e: Exception) {
            // Permission or security restriction, fallback gracefully
        }
        return detectedAccounts
    }

    /**
     * Creates an Intent to prompt the user with Android's native Google Account Chooser.
     */
    fun createChooseAccountIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } else {
            @Suppress("DEPRECATION")
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                false,
                null,
                null,
                null,
                null
            )
        }
    }

    /**
     * Parses the result from the Google account picker activity.
     */
    fun parseAccountFromIntent(data: Intent?): UserGoogleAccount? {
        if (data == null) return null
        val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) ?: return null

        val displayName = accountName.substringBefore("@")
            .replace(".", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        return UserGoogleAccount(
            email = accountName,
            displayName = displayName.ifBlank { "Google Pilot" },
            isCloudSyncEnabled = true,
            lastSyncedTime = "Just now"
        )
    }

    /**
     * Instantly binds an existing email address as the active Google Account.
     */
    fun signInWithEmail(context: Context, email: String, name: String? = null): UserGoogleAccount {
        val calculatedName = name ?: email.substringBefore("@")
            .replace(".", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val account = UserGoogleAccount(
            email = email,
            displayName = calculatedName.ifBlank { "Google User" },
            isCloudSyncEnabled = true,
            lastSyncedTime = "Just now"
        )
        saveSession(context, account)
        return account
    }

    fun saveSession(context: Context, account: UserGoogleAccount) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_NAME, account.displayName)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun loadSession(context: Context): UserGoogleAccount? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return null

        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val name = prefs.getString(KEY_NAME, "Google Pilot") ?: "Google Pilot"

        return UserGoogleAccount(
            email = email,
            displayName = name,
            isCloudSyncEnabled = true,
            lastSyncedTime = "Synced"
        )
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
