// app/src/main/java/com/examapp/ui/profile/ProfileViewModel.kt
package com.examapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.User
import com.examapp.data.models.UserRole
import com.examapp.data.models.SubscriptionType
import com.examapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _editProfileState = MutableStateFlow<EditProfileState>(EditProfileState())
    val editProfileState: StateFlow<EditProfileState> = _editProfileState.asStateFlow()

    private val _statsState = MutableStateFlow<ProfileStatsState>(ProfileStatsState.Loading)
    val statsState: StateFlow<ProfileStatsState> = _statsState.asStateFlow()

    init {
        loadProfile()
        loadProfileStats()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading

                val currentUser = authRepository.getCurrentUser()
                val userId = currentUser?.id ?: run {
                    _profileState.value = ProfileState.Error("کاربر یافت نشد")
                    return@launch
                }

                val result = authRepository.getUserProfile(userId)

                if (result.isSuccess) {
                    val user = result.getOrNull()
                    user?.let {
                        _profileState.value = ProfileState.Success(it)
                        _editProfileState.value = EditProfileState.fromUser(it)
                    }
                } else {
                    _profileState.value = ProfileState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت پروفایل"
                    )
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        grade: Int? = null,
        school: String? = null,
        avatarUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                _editProfileState.value = _editProfileState.value.copy(isLoading = true)

                val userId = authRepository.getUserId() ?: run {
                    _editProfileState.value = _editProfileState.value.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = "لطفاً وارد شوید"
                    )
                    return@launch
                }

                val result = authRepository.updateUserProfile(
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    grade = grade,
                    school = school,
                    avatarUrl = avatarUrl
                )

                if (result.isSuccess) {
                    val updatedUser = result.getOrNull()
                    updatedUser?.let {
                        _profileState.value = ProfileState.Success(it)
                        _editProfileState.value = EditProfileState(
                            isLoading = false,
                            isSuccess = true,
                            message = "پروفایل با موفقیت به‌روزرسانی شد"
                        )
                    }
                } else {
                    _editProfileState.value = _editProfileState.value.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = result.exceptionOrNull()?.message ?: "خطا در به‌روزرسانی"
                    )
                }
            } catch (e: Exception) {
                _editProfileState.value = _editProfileState.value.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = "خطا در اتصال: ${e.message}"
                )
            }
        }
    }

    fun loadProfileStats() {
        viewModelScope.launch {
            try {
                _statsState.value = ProfileStatsState.Loading

                // اینجا می‌توانید آمار کاربر را از Repositoryهای مربوطه بگیرید
                // فعلاً داده‌های نمونه می‌دهیم
                _statsState.value = ProfileStatsState.Success(
                    ProfileStats(
                        totalExams = 24,
                        passedExams = 18,
                        averageScore = 82,
                        totalStudyTime = "45 ساعت",
                        favoriteSubject = "ریاضی",
                        weakSubject = "علوم",
                        streakDays = 7,
                        rank = 125,
                        successRate = 75
                    )
                )
            } catch (e: Exception) {
                _statsState.value = ProfileStatsState.Error("خطا در دریافت آمار")
            }
        }
    }

    fun updateEditProfileField(
        firstName: String = _editProfileState.value.firstName,
        lastName: String = _editProfileState.value.lastName,
        email: String? = _editProfileState.value.email,
        phone: String? = _editProfileState.value.phone,
        grade: Int? = _editProfileState.value.grade,
        school: String? = _editProfileState.value.school
    ) {
        _editProfileState.value = EditProfileState(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            grade = grade,
            school = school
        )
    }

    fun resetEditProfileState() {
        _editProfileState.value = EditProfileState()
    }

    fun refreshProfile() {
        loadProfile()
        loadProfileStats()
    }

    fun logout() {
        authRepository.logout()
        _profileState.value = ProfileState.LoggedOut
    }

    fun isProUser(): Boolean {
        return authRepository.isProUser()
    }

    fun getUserRole(): String {
        return authRepository.getUserRole()?.toString() ?: "دانش‌آموز"
    }

    fun getCurrentUser(): User? {
        return authRepository.getCurrentUser()
    }
}

// State Classes
sealed class ProfileState {
    data object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
    data object LoggedOut : ProfileState()
}

data class EditProfileState(
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
) {
    companion object {
        fun fromUser(user: User): EditProfileState {
            return EditProfileState(
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                phone = user.phone,
                grade = user.grade,
                school = user.school
            )
        }
    }
}

sealed class ProfileStatsState {
    data object Loading : ProfileStatsState()
    data class Success(val stats: ProfileStats) : ProfileStatsState()
    data class Error(val message: String) : ProfileStatsState()
}

data class ProfileStats(
    val totalExams: Int,
    val passedExams: Int,
    val averageScore: Int,
    val totalStudyTime: String,
    val favoriteSubject: String,
    val weakSubject: String,
    val streakDays: Int,
    val rank: Int,
    val successRate: Int
)