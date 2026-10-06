package com.hajun.layar.data

import com.hajun.layar.model.Show

interface ShowRepository {
    suspend fun browseShows(): List<Show>
    suspend fun browsePage(page: Int): BrowsePage
    suspend fun searchShows(query: String): List<Show>
    suspend fun getShow(id: Int): Show
}
