package com.example

import com.example.data.api.FlexibleBooleanAdapter
import com.example.data.api.FlexibleStringAdapter
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SeriesModelTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(FlexibleBooleanAdapter())
            .add(FlexibleStringAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun userProfile_roleDefaultsAndHelpers() {
        val defaultProfile = UserProfile(id = "user-1", username = "regular_user")
        assertEquals("user", defaultProfile.role)
        assertFalse(defaultProfile.isCreator)
        assertFalse(defaultProfile.isAdmin)
        assertFalse(defaultProfile.canCreateSeries)

        val creatorProfile = UserProfile(id = "user-2", username = "animator", role = "creator")
        assertTrue(creatorProfile.isCreator)
        assertFalse(creatorProfile.isAdmin)
        assertTrue(creatorProfile.canCreateSeries)

        val adminProfile = UserProfile(id = "user-3", username = "superadmin", role = "admin")
        assertTrue(adminProfile.isCreator)
        assertTrue(adminProfile.isAdmin)
        assertTrue(adminProfile.canCreateSeries)
    }

    @Test
    fun parseSeriesCard_nullBannerFallback() {
        val json = """
            {
              "id": "7876a4a4-5678-4321-abcd-1234567890ab",
              "slug": "naruto-shippuden",
              "title": "Naruto Shippuden",
              "original_title": "NARUTO -ナルト- 疾風伝",
              "description": "Naruto Uzumaki wants to be the best ninja in the land.",
              "poster_url": "https://pub-b3b8e974b48a4470bcaf08e042ed655e.r2.dev/naruto_poster.jpg",
              "banner_url": null,
              "category": { "id": "cat-1", "slug": "anime", "name": "Anime" },
              "genres": ["action", "adventure"],
              "languages": ["hindi", "japanese"],
              "rating": 8.7,
              "avg_user_rating": 9.2,
              "ratings_count": 142,
              "status": "completed",
              "release_year": 2007,
              "age_rating": "13+",
              "is_mature": false,
              "episodes_count": 500,
              "last_episode_at": "2026-10-24T00:00:00Z",
              "views_count": 10543,
              "likes_count": 892,
              "created_at": "2026-10-20T12:00:00Z"
            }
        """.trimIndent()

        val adapter = moshi.adapter(SeriesCard::class.java)
        val card = adapter.fromJson(json)

        assertNotNull(card)
        assertEquals("naruto-shippuden", card?.slug)
        assertNull(card?.bannerUrl)
        assertEquals(card?.posterUrl, card?.effectiveBannerUrl)
        assertEquals("Anime", card?.category?.name)
        assertEquals(2, card?.genres?.size)
        assertEquals(892, card?.likesCount)
    }

    @Test
    fun parseSeriesHomeRpcResponse() {
        val json = """
            [
              {
                "shelf": {
                  "id": "shelf-1",
                  "slug": "featured",
                  "title": "Featured",
                  "type": "hero",
                  "layout": "hero",
                  "position": 0
                },
                "items": [
                  {
                    "id": "s1",
                    "slug": "naruto-shippuden",
                    "title": "Naruto Shippuden",
                    "poster_url": "https://example.com/poster.jpg",
                    "banner_url": null,
                    "genres": ["action"],
                    "languages": ["hindi"],
                    "rating": 8.7,
                    "likes_count": 150
                  }
                ]
              },
              {
                "shelf": {
                  "id": "shelf-2",
                  "slug": "top-rated",
                  "title": "Top 10 Rated",
                  "type": "top_rated",
                  "layout": "top10",
                  "position": 30
                },
                "items": []
              }
            ]
        """.trimIndent()

        val type = Types.newParameterizedType(List::class.java, ShelfWithItems::class.java)
        val adapter = moshi.adapter<List<ShelfWithItems>>(type)
        val shelves = adapter.fromJson(json)

        assertNotNull(shelves)
        assertEquals(2, shelves?.size)
        assertEquals("hero", shelves?.get(0)?.shelf?.layout)
        assertEquals(1, shelves?.get(0)?.items?.size)
        assertEquals("top10", shelves?.get(1)?.shelf?.layout)
        assertEquals(0, shelves?.get(1)?.items?.size)
    }

    @Test
    fun parseSeriesRowWithArraysAndEmbed() {
        val json = """
            {
              "id": "series-uuid-99",
              "slug": "attack-on-titan",
              "title": "Attack on Titan",
              "original_title": "進撃の巨人",
              "category_id": "cat-uuid-1",
              "category": { "slug": "anime", "name": "Anime" },
              "genres": ["dark fantasy", "action", "apocalyptic"],
              "languages": ["hindi dub", "japanese"],
              "subtitle_languages": ["english", "hindi"],
              "rating": 9.0,
              "status": "completed",
              "is_published": true,
              "is_featured": true,
              "is_blocked": false,
              "total_episodes": 89,
              "avg_user_rating": 9.5,
              "ratings_count": 890,
              "views_count": 50000,
              "episodes_count": 1,
              "created_by": "profile-uuid-1"
            }
        """.trimIndent()

        val adapter = moshi.adapter(Series::class.java)
        val series = adapter.fromJson(json)

        assertNotNull(series)
        assertEquals("attack-on-titan", series?.slug)
        assertEquals(3, series?.genres?.size)
        assertEquals(2, series?.subtitleLanguages?.size)
        assertEquals(9.5, series?.avgUserRating)
        assertEquals(50000L, series?.viewsCount)
        assertEquals("Anime", series?.category?.name)
    }

    @Test
    fun parseEpisodeSourcesRow() {
        val json = """
            {
              "id": "src-uuid-1",
              "episode_id": "ep-uuid-1",
              "label": "Server 1 (R2 Fast)",
              "url": "https://pub-b3b8e974b48a4470bcaf08e042ed655e.r2.dev/naruto_s01e01.mp4",
              "platform": "direct",
              "language": "Hindi Dub",
              "quality": "720p",
              "priority": 0,
              "is_active": true,
              "fail_count": 2,
              "last_ok_at": "2026-10-24T05:00:00Z"
            }
        """.trimIndent()

        val adapter = moshi.adapter(EpisodeSource::class.java)
        val source = adapter.fromJson(json)

        assertNotNull(source)
        assertEquals("direct", source?.platform)
        assertEquals(0, source?.priority)
        assertEquals(2, source?.failCount)
        assertTrue(source?.isActive == true)
    }

    @Test
    fun seriesResult_parsesP0001AndPermissionErrors() {
        val p0001ErrorJson = """{"code":"P0001","message":"Cannot remove the last admin"}"""
        val parsedP0001 = SeriesResult.parseError(p0001ErrorJson, 400)
        assertEquals("Cannot remove the last admin.", parsedP0001.message)
        assertTrue(parsedP0001.isLastAdminError)

        val permErrorJson = """{"code":"42501","message":"permission denied for table series"}"""
        val parsedPerm = SeriesResult.parseError(permErrorJson, 403)
        assertEquals("You don't have permission to perform this action.", parsedPerm.message)
        assertTrue(parsedPerm.isPermissionDenied)

        val conflictJson = """{"code":"23505","message":"duplicate key value violates unique constraint"}"""
        val parsedConflict = SeriesResult.parseError(conflictJson, 409)
        assertEquals("Already exists (duplicate slug or episode number).", parsedConflict.message)
        assertTrue(parsedConflict.isConflict)
    }

    @Test
    fun writeWhitelist_requestBodiesNeverContainReadOnlyFields() {
        val seriesInsert = SeriesInsert(
            title = "My New Show",
            slug = "my-new-show",
            createdBy = "uid-123",
            genres = listOf("action", "fantasy"),
            languages = listOf("hindi", "english"),
            status = "ongoing"
        )
        val seriesJson = moshi.adapter(SeriesInsert::class.java).toJson(seriesInsert)
        assertFalse("SeriesInsert must never contain views_count", seriesJson.contains("views_count"))
        assertFalse("SeriesInsert must never contain likes_count", seriesJson.contains("likes_count"))
        assertFalse("SeriesInsert must never contain avg_user_rating", seriesJson.contains("avg_user_rating"))
        assertFalse("SeriesInsert must never contain episodes_count", seriesJson.contains("episodes_count"))
        assertFalse("SeriesInsert must never contain ratings_count", seriesJson.contains("ratings_count"))
        assertFalse("SeriesInsert must never contain last_episode_at", seriesJson.contains("last_episode_at"))

        val epInsert = EpisodeInsert(
            seriesId = "s-1",
            seasonNumber = 1,
            episodeNumber = 1,
            createdBy = "uid-123",
            title = "Pilot"
        )
        val epJson = moshi.adapter(EpisodeInsert::class.java).toJson(epInsert)
        assertFalse("EpisodeInsert must never contain likes_count", epJson.contains("likes_count"))
        assertFalse("EpisodeInsert must never contain views_count", epJson.contains("views_count"))
        assertFalse("EpisodeInsert must never contain comments_count", epJson.contains("comments_count"))

        val srcInsert = SourceInsert(
            episodeId = "ep-1",
            label = "Main Server",
            url = "https://example.com/video.mp4"
        )
        val srcJson = moshi.adapter(SourceInsert::class.java).toJson(srcInsert)
        assertFalse("SourceInsert must never contain fail_count", srcJson.contains("fail_count"))
        assertFalse("SourceInsert must never contain last_ok_at", srcJson.contains("last_ok_at"))
    }
}
