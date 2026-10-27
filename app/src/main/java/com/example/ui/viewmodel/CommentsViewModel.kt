package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.CommentLiker
import com.example.data.model.Post
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CommentSortOrder {
    NEWEST, TOP, OLDEST
}

class CommentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SocialRepository(application)

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOrder = MutableStateFlow(CommentSortOrder.NEWEST)
    val sortOrder: StateFlow<CommentSortOrder> = _sortOrder.asStateFlow()

    private val _replyToComment = MutableStateFlow<Comment?>(null)
    val replyToComment: StateFlow<Comment?> = _replyToComment.asStateFlow()

    private val _editingComment = MutableStateFlow<Comment?>(null)
    val editingComment: StateFlow<Comment?> = _editingComment.asStateFlow()

    // Comment likers sheet
    private val _commentLikers = MutableStateFlow<List<CommentLiker>>(emptyList())
    val commentLikers: StateFlow<List<CommentLiker>> = _commentLikers.asStateFlow()

    private val _selectedCommentForLikers = MutableStateFlow<Comment?>(null)
    val selectedCommentForLikers: StateFlow<Comment?> = _selectedCommentForLikers.asStateFlow()

    private val _isCommentLikersLoading = MutableStateFlow(false)
    val isCommentLikersLoading: StateFlow<Boolean> = _isCommentLikersLoading.asStateFlow()

    fun openComments(post: Post) {
        _selectedPost.value = post
        _comments.value = emptyList()
        _replyToComment.value = null
        _editingComment.value = null
        loadComments(post.id)
    }

    fun closeComments() {
        _selectedPost.value = null
        _comments.value = emptyList()
        _replyToComment.value = null
        _editingComment.value = null
    }

    fun setSortOrder(order: CommentSortOrder) {
        _sortOrder.value = order
        _selectedPost.value?.let { loadComments(it.id) }
    }

    fun setReplyTo(comment: Comment?) {
        _replyToComment.value = comment
    }

    fun setEditingComment(comment: Comment?) {
        _editingComment.value = comment
    }

    fun loadComments(postId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val res = repository.getComments(postId)
            res.onSuccess { rawList ->
                val listWithLikes = rawList.map { c ->
                    c.copy(isLikedByMe = repository.isCommentLikedByMe(c.id))
                }
                _comments.value = sortComments(listWithLikes, _sortOrder.value)
            }
            _isLoading.value = false
        }
    }

    private fun sortComments(list: List<Comment>, order: CommentSortOrder): List<Comment> {
        val topLevel = list.filter { it.parentId == null }
        val sortedTopLevel = when (order) {
            CommentSortOrder.NEWEST -> topLevel.sortedByDescending { it.createdAt }
            CommentSortOrder.TOP -> topLevel.sortedByDescending { it.likesCount }
            CommentSortOrder.OLDEST -> topLevel.sortedBy { it.createdAt }
        }
        val repliesMap = list.filter { it.parentId != null }.groupBy { it.parentId }

        val result = mutableListOf<Comment>()
        sortedTopLevel.forEach { parent ->
            result.add(parent)
            repliesMap[parent.id]?.sortedBy { it.createdAt }?.forEach { reply ->
                result.add(reply)
            }
        }
        return result
    }

    fun addComment(text: String, onDone: (Boolean, String?) -> Unit) {
        val post = _selectedPost.value ?: return
        if (text.isBlank()) return

        val parent = _replyToComment.value
        val parentId = parent?.id
        val user = repository.currentUser.value ?: return
        val tempId = "temp_${System.currentTimeMillis()}"

        val optimisticComment = Comment(
            id = tempId,
            postId = post.id,
            userId = user.id,
            content = text.trim(),
            parentId = parentId,
            authorProfile = user,
            createdAt = java.time.Instant.now().toString(),
            likesCount = 0,
            isLikedByMe = false
        )

        // Optimistically insert
        _comments.value = _comments.value + optimisticComment
        _replyToComment.value = null

        viewModelScope.launch {
            val res = repository.addComment(post.id, text.trim())
            res.onSuccess { newComment ->
                val enriched = newComment.copy(parentId = parentId, authorProfile = user)
                _comments.value = _comments.value.map { if (it.id == tempId) enriched else it }

                // Send notification
                val recipientId = parent?.userId ?: post.userId
                val notifyType = "reply"
                repository.sendNotificationRpc(recipientId, notifyType, post.id, enriched.id)

                onDone(true, null)
            }.onFailure { e ->
                // Roll back optimistic insertion
                _comments.value = _comments.value.filter { it.id != tempId }
                onDone(false, e.localizedMessage ?: "Failed to send comment")
            }
        }
    }

    fun updateComment(commentId: String, newContent: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateComment(commentId, newContent)
            res.onSuccess { updated ->
                _comments.value = _comments.value.map {
                    if (it.id == commentId) it.copy(content = newContent, updatedAt = updated.updatedAt ?: java.time.Instant.now().toString()) else it
                }
                _editingComment.value = null
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to update comment")
            }
        }
    }

    fun deleteComment(commentId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteComment(commentId)
            res.onSuccess {
                // Delete comment + any child replies
                _comments.value = _comments.value.filter { it.id != commentId && it.parentId != commentId }
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to delete comment")
            }
        }
    }

    fun toggleCommentLike(comment: Comment) {
        val wasLiked = comment.isLikedByMe
        val newLiked = !wasLiked
        val newCount = if (newLiked) comment.likesCount + 1 else (comment.likesCount - 1).coerceAtLeast(0)

        // Optimistic UI update
        _comments.value = _comments.value.map {
            if (it.id == comment.id) it.copy(isLikedByMe = newLiked, likesCount = newCount) else it
        }

        viewModelScope.launch {
            repository.toggleCommentLike(comment.id)
        }
    }

    fun openCommentLikers(comment: Comment) {
        _selectedCommentForLikers.value = comment
        _commentLikers.value = emptyList()
        _isCommentLikersLoading.value = true
        viewModelScope.launch {
            val res = repository.getCommentLikers(comment.id)
            res.onSuccess {
                _commentLikers.value = it
            }
            _isCommentLikersLoading.value = false
        }
    }

    fun closeCommentLikers() {
        _selectedCommentForLikers.value = null
        _commentLikers.value = emptyList()
    }
}
