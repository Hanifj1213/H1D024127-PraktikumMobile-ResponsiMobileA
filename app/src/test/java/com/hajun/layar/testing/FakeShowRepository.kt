package com.hajun.layar.testing

import com.hajun.layar.data.BrowsePage
import com.hajun.layar.data.ShowRepository
import com.hajun.layar.model.Show

class FakeShowRepository : ShowRepository {
    var browseCalls = 0
    val searchQueries = mutableListOf<String>()
    val detailIds = mutableListOf<Int>()
    var browse: suspend () -> List<Show> = { listOf(Show(1, "Browse")) }
    var search: suspend (String) -> List<Show> = { listOf(Show(2, it)) }
    var detail: suspend (Int) -> Show = { Show(it, "Detail") }

    val browsePages = mutableListOf<Int>()
    var browsePageResult: suspend (Int) -> BrowsePage = { BrowsePage(browse()) }

    override suspend fun browseShows(): List<Show> = browsePage(0).shows
    override suspend fun browsePage(page: Int): BrowsePage {
        browseCalls++
        browsePages += page
        return browsePageResult(page)
    }
    override suspend fun searchShows(query: String): List<Show> { searchQueries += query; return search(query) }
    override suspend fun getShow(id: Int): Show { detailIds += id; return detail(id) }
}
