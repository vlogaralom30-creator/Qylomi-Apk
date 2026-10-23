package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Post
import com.example.data.model.PostReactionLiker
import com.example.data.model.UserProfile
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SocialRepository(application)

    // Likers sheet state
    private val _postLikers = MutableStateFlow<List<PostReactionLiker>>(emptyList())
    val postLikers: StateFlow<List<PostReactionLiker>> = _postLikers.asStateFlow()

    private val _isLikersLoading = MutableStateFlow(false)
    val isLikersLoading: StateFlow<Boolean> = _isLikersLoading.asStateFlow()

    private val _selectedPostForLikers = MutableStateFlow<Post?>(null)
    val selectedPostForLikers: StateFlow<Post?> = _selectedPostForLikers.asStateFlow()

    // Report sheet state
    private val _selectedPostForReport = MutableStateFlow<Post?>(null)
    val selectedPostForReport: StateFlow<Post?> = _selectedPostForReport.asStateFlow()

    fun openLikers(post: Post) {
        _selectedPostForLikers.value = post
        _postLikers.value = emptyList()
        _isLikersLoading.value = true
        viewModelScope.launch {
            val res = repository.getPostLikers(post.id)
            res.onSuccess {
                _postLikers.value = it
            }
            _isLikersLoading.value = false
        }
    }

    fun closeLikers() {
        _selectedPostForLikers.value = null
        _postLikers.value = emptyList()
    }

    fun openReport(post: Post) {
        _selectedPostForReport.value = post
    }

    fun closeReport() {
        _selectedPostForReport.value = null
    }

    fun submitReport(
        postId: String?,
        commentId: String?,
        reason: String,
        details: String?,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.reportContent(postId, commentId, reason, details)
            res.onSuccess {
                onDone(true, "Thank you. Your report has been submitted.")
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Could not submit report.")
            }
        }
    }

    fun hidePost(post: Post, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.hidePost(post.id)
            onDone(true)
        }
    }

    fun unhidePost(post: Post) {
        viewModelScope.launch {
            repository.unhidePost(post.id)
        }
    }

    fun togglePin(post: Post, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.togglePinPost(post.id, post.isPinned)
            res.onSuccess { updatedPost ->
                val msg = if (updatedPost.isPinned) "Post pinned to profile" else "Post unpinned from profile"
                onDone(true, msg)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to update pin state")
            }
        }
    }

    fun toggleAllowComments(post: Post, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.toggleAllowComments(post.id, post.allowComments)
            res.onSuccess { updatedPost ->
                val msg = if (updatedPost.allowComments) "Comments enabled" else "Comments turned off"
                onDone(true, msg)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to update comment settings")
            }
        }
    }

    fun toggleFollow(userProfile: UserProfile, onDone: (Boolean, Boolean) -> Unit) {
        viewModelScope.launch {
            val isNowFollowing = repository.toggleFollow(userProfile.id)
            onDone(true, isNowFollowing)
        }
    }

    fun isUserFollowed(userId: String): Boolean = repository.isUserFollowed(userId)

    fun sharePost(context: android.content.Context, post: Post) {
        try {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, post.title ?: "Post on Qylomi")
                val link = "https://qylomi.com/posts/${post.id}"
                val text = buildString {
                    if (!post.title.isNullOrBlank()) append(post.title).append("\n\n")
                    if (!post.content.isNullOrBlank()) append(post.content).append("\n\n")
                    append("Read more on Qylomi: ").append(link)
                }
                putExtra(android.content.Intent.EXTRA_TEXT, text)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Post"))
        } catch (_: Exception) {}
    }
}
