package com.example.util

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the currently active user identity for multi-user isolation.
 * Ensures each Google account has its own isolated Room Database, SharedPreferences,
 * and Google Drive backup structure.
 */
object UserManager {
    private const val PREFS_NAME = "pdf_master_user_prefs"
    private const val KEY_ACTIVE_USER_KEY = "active_user_key"
    private const val KEY_ACTIVE_USER_EMAIL = "active_user_email"

    private val _currentUserKey = MutableStateFlow("guest")
    val currentUserKey: StateFlow<String> = _currentUserKey.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    fun init(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val key = computeUserKey(account)
        val email = account?.email
        _currentUserKey.value = key
        _currentUserEmail.value = email

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTIVE_USER_KEY, key)
            .putString(KEY_ACTIVE_USER_EMAIL, email)
            .apply()
    }

    fun onAccountChanged(context: Context, account: GoogleSignInAccount?) {
        val key = computeUserKey(account)
        val email = account?.email
        _currentUserKey.value = key
        _currentUserEmail.value = email

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTIVE_USER_KEY, key)
            .putString(KEY_ACTIVE_USER_EMAIL, email)
            .apply()
    }

    fun getCurrentUserKey(context: Context): String {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return computeUserKey(account)
    }

    fun getCurrentUserEmail(context: Context): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.email
    }

    fun computeUserKey(account: GoogleSignInAccount?): String {
        if (account == null || account.email.isNullOrBlank()) {
            return "guest"
        }
        val email = account.email!!.trim().lowercase()
        val safeEmail = email.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        return "user_$safeEmail"
    }

    fun getUserDatabaseName(userKey: String): String {
        return if (userKey == "guest" || userKey == "default") {
            "pdf_master_database"
        } else {
            "pdf_master_${userKey}.db"
        }
    }
}
