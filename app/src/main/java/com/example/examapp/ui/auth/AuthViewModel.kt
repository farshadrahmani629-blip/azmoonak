// app/src/main/java/com/examapp/ui/auth/AuthViewModel.kt
package com.examapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                _loginState.value = LoginState(
                    username = username,
                    password = password,
                    isLoading = true
                )

                val result = authRepository.login(username, password)

                if (result.isSuccess) {
                    _authState.value = AuthState.Success
                    _loginState.value = LoginState(
                        username = username,
                        isLoading = false,
                        isSuccess = true,
                        message = "ورود موفقیت‌آمیز بود"
                    )
                } else {
                    _authState.value = AuthState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در ورود"
                    )
                    _loginState.value = LoginState(
                        username = username,
                        password = password,
                        isLoading = false,
                        isError = true,
                        errorMessage = result.exceptionOrNull()?.message ?: "خطا در ورود"
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("خطا در اتصال: ${e.message}")
                _loginState.value = LoginState(
                    username = username,
                    password = password,
                    isLoading = false,
                    isError = true,
                    errorMessage = "خطا در اتصال: ${e.message}"
                )
            }
        }
    }

    fun register(
        username: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        email: String? = null,
        phone: String? = null,
        grade: Int? = null,
        school: String? = null,
        role: UserRole = UserRole.STUDENT
    ) {
        viewModelScope.launch {
            try {
                // Validation
                if (password != confirmPassword) {
                    _registerState.value = RegisterState(
                        username = username,
                        password = password,
                        confirmPassword = confirmPassword,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        grade = grade,
                        school = school,
                        isError = true,
                        errorMessage = "رمز عبور و تأیید آن مطابقت ندارند"
                    )
                    return@launch
                }

                if (username.length < 3) {
                    _registerState.value = RegisterState(
                        username = username,
                        password = password,
                        confirmPassword = confirmPassword,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        grade = grade,
                        school = school,
                        isError = true,
                        errorMessage = "نام کاربری باید حداقل ۳ حرف باشد"
                    )
                    return@launch
                }

                if (password.length < 6) {
                    _registerState.value = RegisterState(
                        username = username,
                        password = password,
                        confirmPassword = confirmPassword,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        grade = grade,
                        school = school,
                        isError = true,
                        errorMessage = "رمز عبور باید حداقل ۶ حرف باشد"
                    )
                    return@launch
                }

                _authState.value = AuthState.Loading
                _registerState.value = RegisterState(
                    username = username,
                    password = password,
                    confirmPassword = confirmPassword,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    grade = grade,
                    school = school,
                    isLoading = true
                )

                val result = authRepository.register(
                    username = username,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    grade = grade,
                    school = school,
                    role = role
                )

                if (result.isSuccess) {
                    _authState.value = AuthState.Success
                    _registerState.value = RegisterState(
                        username = username,
                        isLoading = false,
                        isSuccess = true,
                        message = "ثبت‌نام موفقیت‌آمیز بود"
                    )
                } else {
                    _authState.value = AuthState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در ثبت‌نام"
                    )
                    _registerState.value = RegisterState(
                        username = username,
                        password = password,
                        confirmPassword = confirmPassword,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        grade = grade,
                        school = school,
                        isLoading = false,
                        isError = true,
                        errorMessage = result.exceptionOrNull()?.message ?: "خطا در ثبت‌نام"
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("خطا در اتصال: ${e.message}")
                _registerState.value = RegisterState(
                    username = username,
                    password = password,
                    confirmPassword = confirmPassword,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    grade = grade,
                    school = school,
                    isLoading = false,
                    isError = true,
                    errorMessage = "خطا در اتصال: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
            resetLoginState()
            resetRegisterState()
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState()
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState()
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun getCurrentUser() = authRepository.getCurrentUser()

    fun updateLoginUsername(username: String) {
        _loginState.value = _loginState.value.copy(username = username)
    }

    fun updateLoginPassword(password: String) {
        _loginState.value = _loginState.value.copy(password = password)
    }

    fun updateRegisterField(
        username: String = _registerState.value.username,
        password: String = _registerState.value.password,
        confirmPassword: String = _registerState.value.confirmPassword,
        firstName: String = _registerState.value.firstName,
        lastName: String = _registerState.value.lastName,
        email: String? = _registerState.value.email,
        phone: String? = _registerState.value.phone,
        grade: Int? = _registerState.value.grade,
        school: String? = _registerState.value.school
    ) {
        _registerState.value = RegisterState(
            username = username,
            password = password,
            confirmPassword = confirmPassword,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            grade = grade,
            school = school
        )
    }
}

// State Classes
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val message: String = ""
)

data class RegisterState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val grade: Int? = null,
    val school: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val message: String = ""
)

// UserRole enum (should be in data models)
enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}