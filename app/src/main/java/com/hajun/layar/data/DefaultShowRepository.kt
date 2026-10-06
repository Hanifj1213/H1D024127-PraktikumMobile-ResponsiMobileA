package com.hajun.layar.data

import com.hajun.layar.data.remote.SearchResultDto
import com.hajun.layar.data.remote.ShowDto
import com.hajun.layar.data.remote.TvMazeApi
import com.hajun.layar.data.remote.tvMazeJson
import com.hajun.layar.model.Show
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import retrofit2.HttpException

class DefaultShowRepository(private val api: TvMazeApi) : ShowRepository {
    override suspend fun searchShows(query: String): List<Show> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return browseShows()
        return api.searchShows(normalizedQuery).mapNotNull {
            tvMazeJson.decodeFromJsonElement<SearchResultDto?>(it)?.show?.toShow()
        }
    }

    override suspend fun browseShows(): List<Show> = browsePage(0).shows

    override suspend fun browsePage(page: Int): BrowsePage {
        require(page >= 0) { "Page index must not be negative" }
        return try {
            BrowsePage(api.browseShows(page).mapNotNull {
                tvMazeJson.decodeFromJsonElement<ShowDto?>(it)?.toShow()
            })
        } catch (failure: HttpException) {
            if (failure.code() == 404) BrowsePage(emptyList(), endReached = true)
            else throw failure
        }
    }

    override suspend fun getShow(id: Int): Show {
        require(id > 0) { "Show IDs must be positive integers" }
        val show = api.getShow(id).toShow()
        if (show == null || show.id != id) {
            throw SerializationException("The detail response does not match the requested show")
        }
        return show
    }
}
