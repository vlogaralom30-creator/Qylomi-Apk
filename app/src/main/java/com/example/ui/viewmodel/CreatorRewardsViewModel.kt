package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CreatorEngagementSummary
import com.example.data.model.MilestoneReward
import com.example.data.model.PointsHistoryItem
import com.example.data.model.VirtualRewardItem
import com.example.data.repository.CreatorRewardsRepository
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatorRewardsViewModel(application: Application) : AndroidViewModel(application) {

    private val socialRepository = SocialRepository(application)
    private val repository = CreatorRewardsRepository.getInstance(application, socialRepository)

    val summary: StateFlow<CreatorEngagementSummary?> = repository.summary
    val milestones: StateFlow<List<MilestoneReward>> = repository.milestones
    val rewardsCatalog: StateFlow<List<VirtualRewardItem>> = repository.rewardsCatalog
    val pointsHistory: StateFlow<List<PointsHistoryItem>> = repository.pointsHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh(targetUserId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.computeEngagementSummary(targetUserId)
            } catch (e: Exception) {
                _toastMessage.value = "Failed to load creator metrics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun claimMilestone(milestoneId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.claimMilestone(milestoneId)
            result.fold(
                onSuccess = { reward ->
                    _toastMessage.value = "Claimed +${reward.rewardPoints} Q-Points for '${reward.title}'!"
                },
                onFailure = { error ->
                    _toastMessage.value = error.message ?: "Failed to claim milestone"
                }
            )
            _isLoading.value = false
        }
    }

    fun redeemReward(rewardId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.redeemVirtualReward(rewardId)
            result.fold(
                onSuccess = { item ->
                    _toastMessage.value = "Unlocked '${item.title}'! Perk activated on your profile."
                },
                onFailure = { error ->
                    _toastMessage.value = error.message ?: "Failed to redeem reward"
                }
            )
            _isLoading.value = false
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
