package com.example.ui.navigation

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.SocialTab

sealed interface NavScreenKey {
    data class TabKey(val tab: SocialTab) : NavScreenKey
    data class DestKey(val dest: AppDestination) : NavScreenKey

    val saveableKey: String
        get() = when (this) {
            is TabKey -> "TAB_${tab.name}"
            is DestKey -> "DEST_${AppNavigator.encodeDestination(dest)}"
        }
}

object AppTransitions {
    const val PUSH_DURATION = 280
    const val MODAL_DURATION = 320
    const val TAB_DURATION = 180
    const val DRAWER_REPLACE_IN = 210
    const val DRAWER_REPLACE_OUT = 90

    @Composable
    fun areAnimationsDisabled(): Boolean {
        val context = LocalContext.current
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Throwable) {
            false
        }
    }

    fun getTransition(
        initial: NavScreenKey,
        target: NavScreenKey,
        isAnimationsDisabled: Boolean = false
    ): ContentTransform {
        if (isAnimationsDisabled) {
            return ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None
            )
        }

        val targetDest = (target as? NavScreenKey.DestKey)?.dest
        val initialDest = (initial as? NavScreenKey.DestKey)?.dest

        // 1. Episode Player: Slide up from bottom with fade / on exit slide down
        if (targetDest is AppDestination.EpisodePlayer) {
            return (slideInVertically(
                animationSpec = tween(MODAL_DURATION, easing = FastOutSlowInEasing),
                initialOffsetY = { it }
            ) + fadeIn(tween(MODAL_DURATION))).togetherWith(
                fadeOut(tween(150))
            )
        }
        if (initialDest is AppDestination.EpisodePlayer) {
            return fadeIn(tween(150)).togetherWith(
                slideOutVertically(
                    animationSpec = tween(MODAL_DURATION, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                ) + fadeOut(tween(MODAL_DURATION))
            )
        }

        // 2. Studios and CreatePost: Modal slide up from bottom
        if (targetDest is AppDestination.CreatorStudio || targetDest is AppDestination.AdminStudio || targetDest is AppDestination.CreatePost) {
            return (slideInVertically(
                animationSpec = tween(MODAL_DURATION, easing = FastOutSlowInEasing),
                initialOffsetY = { it }
            ) + fadeIn(tween(MODAL_DURATION))).togetherWith(
                fadeOut(tween(150))
            )
        }
        if (initialDest is AppDestination.CreatorStudio || initialDest is AppDestination.AdminStudio || initialDest is AppDestination.CreatePost) {
            return fadeIn(tween(150)).togetherWith(
                slideOutVertically(
                    animationSpec = tween(MODAL_DURATION, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                ) + fadeOut(tween(MODAL_DURATION))
            )
        }

        // 3. Top-level drawer jumps (SeriesHome, Groups, Reels)
        val isTargetTopLevelDrawer = targetDest is AppDestination.SeriesHome || targetDest is AppDestination.Groups || targetDest is AppDestination.Reels
        val isInitialTopLevelDrawer = initialDest is AppDestination.SeriesHome || initialDest is AppDestination.Groups || initialDest is AppDestination.Reels
        if (isTargetTopLevelDrawer && (isInitialTopLevelDrawer || initial is NavScreenKey.TabKey)) {
            return (fadeIn(tween(DRAWER_REPLACE_IN, easing = FastOutSlowInEasing)) + scaleIn(tween(DRAWER_REPLACE_IN), initialScale = 0.98f)).togetherWith(
                fadeOut(tween(DRAWER_REPLACE_OUT, easing = FastOutSlowInEasing))
            )
        }

        // 4. Tab to Tab crossfade
        if (initial is NavScreenKey.TabKey && target is NavScreenKey.TabKey) {
            return fadeIn(tween(TAB_DURATION, easing = FastOutSlowInEasing)).togetherWith(
                fadeOut(tween(TAB_DURATION, easing = FastOutSlowInEasing))
            )
        }

        // 5. Default detail Push / Pop (Slide horizontal with parallax)
        return if (target is NavScreenKey.DestKey && initial is NavScreenKey.DestKey) {
            (slideInHorizontally(
                animationSpec = tween(PUSH_DURATION, easing = FastOutSlowInEasing),
                initialOffsetX = { it / 4 }
            ) + fadeIn(tween(PUSH_DURATION))).togetherWith(
                slideOutHorizontally(
                    animationSpec = tween(PUSH_DURATION, easing = FastOutSlowInEasing),
                    targetOffsetX = { -it / 4 }
                ) + fadeOut(tween(PUSH_DURATION))
            )
        } else if (target is NavScreenKey.DestKey) {
            (slideInHorizontally(
                animationSpec = tween(PUSH_DURATION, easing = FastOutSlowInEasing),
                initialOffsetX = { it / 4 }
            ) + fadeIn(tween(PUSH_DURATION))).togetherWith(
                slideOutHorizontally(
                    animationSpec = tween(PUSH_DURATION, easing = FastOutSlowInEasing),
                    targetOffsetX = { -it / 4 }
                ) + fadeOut(tween(PUSH_DURATION))
            )
        } else {
            (slideInHorizontally(
                animationSpec = tween(PUSH_DURATION, easing = FastOutSlowInEasing),
                initialOffsetX = { -it / 4 }
            ) + fadeIn(tween(PUSH_DURATION))).togetherWith(
                slideOutHorizontally(
                    animationSpec = tween(PUSH_DURATION, easing = FastOutSlowInEasing),
                    targetOffsetX = { it / 4 }
                ) + fadeOut(tween(PUSH_DURATION))
            )
        }
    }
}
