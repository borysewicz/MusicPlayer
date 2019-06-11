package com.example.musicplayer.musicservice

interface MusicServiceListener {
    fun refreshUI(songPos: Int)
}