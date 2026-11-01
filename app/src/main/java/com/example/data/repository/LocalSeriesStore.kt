package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * Local persistent store for Series, Seasons, Episodes, and Episode Sources.
 * Guarantees that Creator-created series, episodes, and sources persist locally,
 * appear in Creator Studio and across the app, and remain editable/deletable
 * even when offline or when Supabase database tables are unprovisioned or unreachable.
 */
class LocalSeriesStore(context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val seriesFile = File(context.filesDir, "qylomi_local_series.json")
    private val seasonsFile = File(context.filesDir, "qylomi_local_seasons.json")
    private val episodesFile = File(context.filesDir, "qylomi_local_episodes.json")
    private val sourcesFile = File(context.filesDir, "qylomi_local_sources.json")

    private val seriesListAdapter = moshi.adapter<List<Series>>(
        Types.newParameterizedType(List::class.java, Series::class.java)
    )
    private val seasonListAdapter = moshi.adapter<List<Season>>(
        Types.newParameterizedType(List::class.java, Season::class.java)
    )
    private val episodeListAdapter = moshi.adapter<List<Episode>>(
        Types.newParameterizedType(List::class.java, Episode::class.java)
    )
    private val sourceListAdapter = moshi.adapter<List<EpisodeSource>>(
        Types.newParameterizedType(List::class.java, EpisodeSource::class.java)
    )

    @Synchronized
    fun getAllSeries(): List<Series> {
        if (!seriesFile.exists()) return emptyList()
        return try {
            val json = seriesFile.readText()
            seriesListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getSeriesForUser(userId: String?): List<Series> {
        val all = getAllSeries()
        if (userId.isNullOrBlank()) return all
        val userFiltered = all.filter { it.createdBy == userId }
        return if (userFiltered.isNotEmpty()) userFiltered else all
    }

    @Synchronized
    fun saveSeries(series: Series) {
        val current = getAllSeries().toMutableList()
        val index = current.indexOfFirst { it.id == series.id || it.slug == series.slug }
        if (index >= 0) {
            current[index] = series
        } else {
            current.add(0, series)
        }
        writeSeriesList(current)
    }

    @Synchronized
    fun deleteSeries(seriesId: String) {
        val current = getAllSeries().filterNot { it.id == seriesId }
        writeSeriesList(current)

        // Delete associated seasons, episodes, sources
        val seasons = getAllSeasons().filterNot { it.seriesId == seriesId }
        writeSeasonList(seasons)

        val seriesEpisodeIds = getAllEpisodes().filter { it.seriesId == seriesId }.map { it.id }.toSet()
        val episodes = getAllEpisodes().filterNot { it.seriesId == seriesId }
        writeEpisodeList(episodes)

        val sources = getAllSources().filterNot { it.episodeId in seriesEpisodeIds }
        writeSourceList(sources)
    }

    private fun writeSeriesList(list: List<Series>) {
        try {
            val json = seriesListAdapter.toJson(list)
            seriesFile.writeText(json)
        } catch (_: Exception) {}
    }

    // --- Seasons ---
    @Synchronized
    fun getAllSeasons(): List<Season> {
        if (!seasonsFile.exists()) return emptyList()
        return try {
            val json = seasonsFile.readText()
            seasonListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getSeasons(seriesId: String): List<Season> {
        return getAllSeasons().filter { it.seriesId == seriesId }.sortedBy { it.seasonNumber }
    }

    @Synchronized
    fun saveSeason(season: Season) {
        val current = getAllSeasons().toMutableList()
        val index = current.indexOfFirst { it.id == season.id }
        if (index >= 0) {
            current[index] = season
        } else {
            current.add(season)
        }
        writeSeasonList(current)
    }

    @Synchronized
    fun deleteSeason(seasonId: String) {
        val current = getAllSeasons().filterNot { it.id == seasonId }
        writeSeasonList(current)
    }

    private fun writeSeasonList(list: List<Season>) {
        try {
            val json = seasonListAdapter.toJson(list)
            seasonsFile.writeText(json)
        } catch (_: Exception) {}
    }

    // --- Episodes ---
    @Synchronized
    fun getAllEpisodes(): List<Episode> {
        if (!episodesFile.exists()) return emptyList()
        return try {
            val json = episodesFile.readText()
            episodeListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getEpisodes(seriesId: String, publishedOnly: Boolean = false): List<Episode> {
        val list = getAllEpisodes().filter { it.seriesId == seriesId }
        return if (publishedOnly) list.filter { it.isPublished } else list
    }

    @Synchronized
    fun saveEpisode(episode: Episode) {
        val current = getAllEpisodes().toMutableList()
        val index = current.indexOfFirst { it.id == episode.id }
        if (index >= 0) {
            current[index] = episode
        } else {
            current.add(0, episode)
        }
        writeEpisodeList(current)
    }

    @Synchronized
    fun deleteEpisode(episodeId: String) {
        val current = getAllEpisodes().filterNot { it.id == episodeId }
        writeEpisodeList(current)

        // Remove associated sources
        val sources = getAllSources().filterNot { it.episodeId == episodeId }
        writeSourceList(sources)
    }

    private fun writeEpisodeList(list: List<Episode>) {
        try {
            val json = episodeListAdapter.toJson(list)
            episodesFile.writeText(json)
        } catch (_: Exception) {}
    }

    // --- Sources ---
    @Synchronized
    fun getAllSources(): List<EpisodeSource> {
        if (!sourcesFile.exists()) return emptyList()
        return try {
            val json = sourcesFile.readText()
            sourceListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getSources(episodeId: String, activeOnly: Boolean = false): List<EpisodeSource> {
        val list = getAllSources().filter { it.episodeId == episodeId }
        return if (activeOnly) list.filter { it.isActive } else list
    }

    @Synchronized
    fun saveSource(source: EpisodeSource) {
        val current = getAllSources().toMutableList()
        val index = current.indexOfFirst { it.id == source.id }
        if (index >= 0) {
            current[index] = source
        } else {
            current.add(source)
        }
        writeSourceList(current)
    }

    @Synchronized
    fun deleteSource(sourceId: String) {
        val current = getAllSources().filterNot { it.id == sourceId }
        writeSourceList(current)
    }

    private fun writeSourceList(list: List<EpisodeSource>) {
        try {
            val json = sourceListAdapter.toJson(list)
            sourcesFile.writeText(json)
        } catch (_: Exception) {}
    }
}
