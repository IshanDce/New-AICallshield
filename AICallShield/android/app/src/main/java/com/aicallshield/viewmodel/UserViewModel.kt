package com.aicallshield.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context

/**
 * ViewModel for user profile data (name, gender).
 * Persists data using SharedPreferences.
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "aicallshield_user_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_GENDER = "user_gender"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userGender = MutableStateFlow(prefs.getString(KEY_USER_GENDER, "Male") ?: "Male")
    val userGender: StateFlow<String> = _userGender.asStateFlow()

    fun saveProfile(name: String, gender: String) {
        _userName.value = name
        _userGender.value = gender
        viewModelScope.launch {
            prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_GENDER, gender)
                .apply()
        }
    }
}
