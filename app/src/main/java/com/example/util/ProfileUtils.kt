package com.example.util

import com.example.data.model.UserProfile

object ProfileUtils {
    const val SHOW_COMPLETE_PROFILE_BADGE = true

    fun isProfileComplete(profile: UserProfile): Boolean {
        if (!SHOW_COMPLETE_PROFILE_BADGE) return false

        val baseFields = listOf(
            profile.avatarUrl,
            profile.coverUrl,
            profile.displayName,
            profile.username,
            profile.bio,
            profile.headline,
            profile.location
        )
        
        val allBaseFieldsFilled = baseFields.all { !it.isNullOrBlank() }
        
        val extraFields = listOf(
            profile.skills?.isNotEmpty() == true,
            !profile.website.isNullOrBlank(),
            !profile.socialLinks.isNullOrEmpty(),
            !profile.work.isNullOrBlank(),
            !profile.education.isNullOrBlank()
        )
        
        val oneExtraFieldFilled = extraFields.any { it }
        
        return allBaseFieldsFilled && oneExtraFieldFilled
    }
}
