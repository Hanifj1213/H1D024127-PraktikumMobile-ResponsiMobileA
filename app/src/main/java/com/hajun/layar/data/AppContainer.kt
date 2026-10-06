package com.hajun.layar.data

import com.hajun.layar.data.remote.TvMazeApi

class AppContainer {
    val repository: ShowRepository by lazy { DefaultShowRepository(TvMazeApi.create()) }
}
