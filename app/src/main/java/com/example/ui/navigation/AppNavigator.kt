package com.example.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.ui.components.SocialTab

sealed interface AppDestination {
    data class UserProfile(val userId: String) : AppDestination
    object EditProfile : AppDestination
    object Groups : AppDestination
    data class GroupDetails(val groupId: String) : AppDestination
    data class Reels(val initialPostId: String? = null) : AppDestination
    object SeriesHome : AppDestination
    data class SeriesList(val shelfSlug: String) : AppDestination
    data class SeriesDetails(val slug: String) : AppDestination
    data class EpisodePlayer(
        val seriesId: String,
        val episodeId: String? = null,
        val startPositionSec: Int = 0
    ) : AppDestination
    object CreatorStudio : AppDestination
    object AdminStudio : AppDestination
    data class CreatePost(val targetGroupId: String? = null) : AppDestination
    data class TagFeed(val tag: String) : AppDestination
    data class Search(val initialQuery: String = "") : AppDestination
    object Settings : AppDestination
}

class AppNavigator(
    initialTab: SocialTab = SocialTab.FEED,
    initialStack: List<AppDestination> = emptyList()
) {
    private val TAG = "Nav"

    val rootTab: MutableState<SocialTab> = mutableStateOf(initialTab)
    val stack: SnapshotStateList<AppDestination> = mutableStateListOf<AppDestination>().apply {
        addAll(initialStack)
    }

    val top: AppDestination?
        get() = stack.lastOrNull()

    val canPop: Boolean
        get() = stack.isNotEmpty()

    private fun log(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {
            println("$TAG: $msg")
        }
    }

    fun push(dest: AppDestination) {
        if (stack.lastOrNull() == dest) {
            log("push: duplicate destination ignored: $dest")
            return
        }
        stack.add(dest)
        log("push: $dest -> stack depth: ${stack.size}")
    }

    fun pop(): Boolean {
        return if (stack.isNotEmpty()) {
            val removed = stack.removeAt(stack.size - 1)
            log("pop: removed $removed -> remaining stack depth: ${stack.size}")
            true
        } else {
            log("pop: stack already empty")
            false
        }
    }

    fun replaceAll(dest: AppDestination) {
        stack.clear()
        stack.add(dest)
        log("replaceAll: $dest")
    }

    fun switchTab(tab: SocialTab) {
        stack.clear()
        rootTab.value = tab
        log("switchTab: $tab -> stack cleared")
    }

    companion object {
        fun encodeDestination(dest: AppDestination): String {
            return when (dest) {
                is AppDestination.UserProfile -> "user:${dest.userId}"
                is AppDestination.EditProfile -> "edit_profile"
                is AppDestination.Groups -> "groups"
                is AppDestination.GroupDetails -> "group:${dest.groupId}"
                is AppDestination.Reels -> "reels:${dest.initialPostId ?: ""}"
                is AppDestination.SeriesHome -> "series_home"
                is AppDestination.SeriesList -> "series_list:${dest.shelfSlug}"
                is AppDestination.SeriesDetails -> "series_details:${dest.slug}"
                is AppDestination.EpisodePlayer -> "player:${dest.seriesId}:${dest.episodeId ?: ""}:${dest.startPositionSec}"
                is AppDestination.CreatorStudio -> "creator_studio"
                is AppDestination.AdminStudio -> "admin_studio"
                is AppDestination.CreatePost -> "create_post:${dest.targetGroupId ?: ""}"
                is AppDestination.TagFeed -> "tag_feed:${dest.tag}"
                is AppDestination.Search -> "search:${dest.initialQuery}"
                is AppDestination.Settings -> "settings"
            }
        }

        fun decodeDestination(raw: String): AppDestination? {
            val parts = raw.split(":")
            val prefix = parts.getOrNull(0) ?: return null
            return when (prefix) {
                "user" -> parts.getOrNull(1)?.let { AppDestination.UserProfile(it) }
                "edit_profile" -> AppDestination.EditProfile
                "groups" -> AppDestination.Groups
                "group" -> parts.getOrNull(1)?.let { AppDestination.GroupDetails(it) }
                "reels" -> {
                    val id = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                    AppDestination.Reels(id)
                }
                "series_home" -> AppDestination.SeriesHome
                "series_list" -> parts.getOrNull(1)?.let { AppDestination.SeriesList(it) }
                "series_details" -> parts.getOrNull(1)?.let { AppDestination.SeriesDetails(it) }
                "player" -> {
                    val seriesId = parts.getOrNull(1) ?: return null
                    val epId = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                    val pos = parts.getOrNull(3)?.toIntOrNull() ?: 0
                    AppDestination.EpisodePlayer(seriesId, epId, pos)
                }
                "creator_studio" -> AppDestination.CreatorStudio
                "admin_studio" -> AppDestination.AdminStudio
                "create_post" -> {
                    val gId = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                    AppDestination.CreatePost(gId)
                }
                "tag_feed" -> parts.getOrNull(1)?.let { AppDestination.TagFeed(it) }
                "search" -> AppDestination.Search(parts.drop(1).joinToString(":"))
                "settings" -> AppDestination.Settings
                else -> null
            }
        }

        val Saver: Saver<AppNavigator, List<String>> = Saver(
            save = { nav ->
                val list = mutableListOf<String>()
                list.add("TAB:${nav.rootTab.value.name}")
                nav.stack.forEach { dest ->
                    list.add("DEST:${encodeDestination(dest)}")
                }
                list
            },
            restore = { list ->
                var tab = SocialTab.FEED
                val dests = mutableListOf<AppDestination>()
                for (item in list) {
                    when {
                        item.startsWith("TAB:") -> {
                            val name = item.removePrefix("TAB:")
                            tab = runCatching { SocialTab.valueOf(name) }.getOrDefault(SocialTab.FEED)
                        }
                        item.startsWith("DEST:") -> {
                            decodeDestination(item.removePrefix("DEST:"))?.let { dests.add(it) }
                        }
                    }
                }
                AppNavigator(initialTab = tab, initialStack = dests)
            }
        )
    }
}

@Composable
fun rememberAppNavigator(): AppNavigator {
    return rememberSaveable(saver = AppNavigator.Saver) {
        AppNavigator()
    }
}
