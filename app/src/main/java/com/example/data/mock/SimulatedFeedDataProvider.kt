package com.example.data.mock

import com.example.data.model.PhotoPost
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost

/**
 * Simulated data model providing rich, realistic social posts with images for Coil loading
 * and Material 3 card presentation.
 */
object SimulatedFeedDataProvider {

    val creators = listOf(
        UserProfile(
            id = "creator_tanvir",
            username = "tanvir_art",
            displayName = "Tanvir Ahmed",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
            bio = "Visual storyteller & landscape photographer from Dhaka 🇧🇩",
            creatorRank = "Master Visualist",
            creatorLevel = 12,
            points = 2450,
            badges = listOf("verified_creator", "photo_pro", "top_contributor")
        ),
        UserProfile(
            id = "creator_maya",
            username = "maya_dev",
            displayName = "Maya Lin",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
            bio = "Android engineer crafting fluid Jetpack Compose animations ✨",
            creatorRank = "Code Architect",
            creatorLevel = 9,
            points = 1820,
            badges = listOf("compose_wizard", "open_sourcer")
        ),
        UserProfile(
            id = "creator_arif",
            username = "arif_bangla",
            displayName = "আরিফুর রহমান",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
            bio = "বাংলা সাহিত্য, ঐতিহ্য ও ভ্রমণ পিপাসু মানুষ 🌿",
            creatorRank = "Community Guide",
            creatorLevel = 8,
            points = 1530,
            badges = listOf("heritage_keeper", "bangla_voice")
        ),
        UserProfile(
            id = "creator_elena",
            username = "elena_motion",
            displayName = "Elena Rostova",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&auto=format&fit=crop&q=80",
            bio = "Motion graphics artist & digital sketch animator 🎨⚡",
            creatorRank = "Prime Animator",
            creatorLevel = 15,
            points = 3200,
            badges = listOf("reels_star", "motion_master")
        ),
        UserProfile(
            id = "creator_zahed",
            username = "zahed_tech",
            displayName = "Zahedul Islam",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
            bio = "Tech curator, UI design reviewer and productivity nerd 💻",
            creatorRank = "Senior Curator",
            creatorLevel = 7,
            points = 1290,
            badges = listOf("resource_scout")
        )
    )

    fun getSimulatedPosts(): List<Post> {
        return listOf(
            Post(
                id = "sim_post_1",
                userId = creators[0].id,
                authorProfile = creators[0],
                type = "Photo",
                title = "Golden Hour in Sylhet Tea Gardens",
                content = "Captured the morning mist settling over the lush rolling tea hills in Sreemangal. The golden light through the canopy was truly breathtaking! 🍃📷 #nature #photography #sylhet #bangla",
                externalUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1080&auto=format&fit=crop&q=80",
                photoPosts = listOf(
                    PhotoPost(
                        id = "sim_photo_1",
                        postId = "sim_post_1",
                        caption = "Sunbeams filtering through the tea leaves",
                        photoUrls = listOf(
                            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1080&auto=format&fit=crop&q=80",
                            "https://images.unsplash.com/photo-1511497584788-87676104235f?w=1080&auto=format&fit=crop&q=80"
                        ),
                        category = "Photography",
                        tags = listOf("nature", "bangla", "travel")
                    )
                ),
                appreciationsCount = 284,
                repliesCount = 38,
                relaysCount = 19,
                createdAt = "2026-11-01T08:15:00Z",
                isLikedByMe = true,
                isBookmarkedByMe = true,
                rankScore = 9.85,
                rankPosition = 1
            ),
            Post(
                id = "sim_post_2",
                userId = creators[1].id,
                authorProfile = creators[1],
                type = "Idea",
                title = "Modern Material 3 Card Architectures in Jetpack Compose",
                content = "Designing modern social feeds requires generous breathing room, clean elevation, and reactive image pipelines with Coil. Here is a preview of fluid state handling using snapshotFlow and debounced tracking! What do you prioritize in your feed layouts? 🚀📱 #android #jetpackcompose #material3",
                externalUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=1080&auto=format&fit=crop&q=80",
                photoPosts = listOf(
                    PhotoPost(
                        id = "sim_photo_2",
                        postId = "sim_post_2",
                        caption = "Clean architecture code structure in Compose",
                        photoUrls = listOf(
                            "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=1080&auto=format&fit=crop&q=80"
                        ),
                        category = "Tech",
                        tags = listOf("android", "compose", "ui")
                    )
                ),
                appreciationsCount = 195,
                repliesCount = 42,
                relaysCount = 14,
                createdAt = "2026-11-01T07:45:00Z",
                isLikedByMe = false,
                isBookmarkedByMe = false,
                rankScore = 9.10,
                rankPosition = 2
            ),
            Post(
                id = "sim_post_3",
                userId = creators[2].id,
                authorProfile = creators[2],
                type = "Post",
                title = "নদীমাতৃক বাংলাদেশ - নদীর বুকে গোধূলির আলো",
                content = "মেঘনা নদীর তীরে দাঁড়িয়ে সূর্যাস্ত দেখার মতো শান্তি আর কোথাও পাওয়া যায় না। নদীর বুকে মাঝির গান আর শীতল বাতাস মনকে এক অদ্ভুত আনন্দে ভরিয়ে তোলে। 🌅 নৌকা ও জলের এই মিলন আমাদের চিরায়ত ঐতিহ্য। #বাংলা #বাংলাদেশ #গ্রামবাংলা #নদী",
                externalUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1080&auto=format&fit=crop&q=80",
                photoPosts = listOf(
                    PhotoPost(
                        id = "sim_photo_3",
                        postId = "sim_post_3",
                        caption = "নদীর বুকে গোধূলির প্রতিচ্ছবি",
                        photoUrls = listOf(
                            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1080&auto=format&fit=crop&q=80"
                        ),
                        category = "Culture",
                        tags = listOf("বাংলা", "বাংলাদেশ", "heritage")
                    )
                ),
                appreciationsCount = 342,
                repliesCount = 67,
                relaysCount = 28,
                createdAt = "2026-11-01T06:30:00Z",
                isLikedByMe = false,
                isBookmarkedByMe = true,
                rankScore = 8.75,
                rankPosition = 3
            ),
            Post(
                id = "sim_post_4",
                userId = creators[3].id,
                authorProfile = creators[3],
                type = "Video",
                title = "Cyberpunk Cityscape Motion Sketch",
                content = "60-second speed animation process: painting neon rain reflections and holographic billboards on a dark urban canvas! Rendered with hardware-accelerated shaders 🎨⚡ #animation #motiongraphics #digitalart",
                externalUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1080&auto=format&fit=crop&q=80",
                videoPosts = listOf(
                    VideoPost(
                        id = "sim_vid_4",
                        postId = "sim_post_4",
                        title = "Cyberpunk Cityscape Motion Sketch",
                        videoUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1080&auto=format&fit=crop&q=80",
                        thumbnailUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1080&auto=format&fit=crop&q=80",
                        platform = "direct",
                        durationSeconds = 60,
                        isVertical = true
                    )
                ),
                appreciationsCount = 412,
                repliesCount = 53,
                relaysCount = 46,
                createdAt = "2026-11-01T05:20:00Z",
                isLikedByMe = true,
                isBookmarkedByMe = false,
                rankScore = 8.40,
                rankPosition = 4
            ),
            Post(
                id = "sim_post_5",
                userId = creators[4].id,
                authorProfile = creators[4],
                type = "Resource",
                title = "Curated Kotlin Multiplatform & Android Architecture Toolkit",
                content = "Here is a complete checklist of production-proven libraries for offline-first caching, Coil 3 image caching strategies, and Material 3 design token harmonization. Save this to your bookmarks! 🛠️📦 #android #kotlin #toolkit #resources",
                externalUrl = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=1080&auto=format&fit=crop&q=80",
                photoPosts = listOf(
                    PhotoPost(
                        id = "sim_photo_5",
                        postId = "sim_post_5",
                        caption = "Clean architecture development setup",
                        photoUrls = listOf(
                            "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=1080&auto=format&fit=crop&q=80"
                        ),
                        category = "Development",
                        tags = listOf("android", "resources", "kotlin")
                    )
                ),
                appreciationsCount = 168,
                repliesCount = 22,
                relaysCount = 31,
                createdAt = "2026-11-01T04:10:00Z",
                isLikedByMe = false,
                isBookmarkedByMe = true,
                rankScore = 7.95,
                rankPosition = 5
            ),
            Post(
                id = "sim_post_6",
                userId = creators[1].id,
                authorProfile = creators[1],
                type = "Question",
                title = "Design Systems: Dynamic Material You vs Minimalist Typography?",
                content = "When building content-centric mobile apps, do you prefer expressive dynamic theming based on user wallpaper colors, or strict high-contrast editorial minimalism? Vote and drop your reasoning below! 👇 #ui #ux #design #discussion",
                externalUrl = "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=1080&auto=format&fit=crop&q=80",
                photoPosts = listOf(
                    PhotoPost(
                        id = "sim_photo_6",
                        postId = "sim_post_6",
                        caption = "Color palettes and typography exploration",
                        photoUrls = listOf(
                            "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=1080&auto=format&fit=crop&q=80"
                        ),
                        category = "Design",
                        tags = listOf("design", "ui", "question")
                    )
                ),
                appreciationsCount = 135,
                repliesCount = 58,
                relaysCount = 9,
                createdAt = "2026-11-01T03:00:00Z",
                isLikedByMe = false,
                isBookmarkedByMe = false,
                rankScore = 7.50,
                rankPosition = 6
            )
        )
    }
}
