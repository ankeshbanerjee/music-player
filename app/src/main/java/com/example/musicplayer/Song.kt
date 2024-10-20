package com.example.musicplayer

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

data class Track(
    val name: String,
    val desc: String,
    @RawRes val resId: Int,
    @DrawableRes val imageId: Int
)

val songs: List<Track> = listOf(
    Track(
        name = "First Song",
        desc = "First Song Description",
        resId = R.raw.one,
        imageId = R.drawable.one
    ),
    Track(
        name = "Second Song",
        desc = "Second Song Description",
        resId = R.raw.two,
        imageId = R.drawable.two
    ),
    Track(
        name = "Third Song",
        desc = "Third Song Description",
        resId = R.raw.three,
        imageId = R.drawable.three
    ),
    Track(
        name = "Fourth Song",
        desc = "Fourth Song Description",
        resId = R.raw.four,
        imageId = R.drawable.four
    ),
    Track(
        name = "Fifth Song",
        desc = "Fifth Song Description",
        resId = R.raw.five,
        imageId = R.drawable.five
    ),
)
