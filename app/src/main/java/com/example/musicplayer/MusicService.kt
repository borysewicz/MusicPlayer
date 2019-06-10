package com.example.musicplayer

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import com.example.musicplayer.model.Song
import android.content.ContentUris
import android.util.Log


class MusicService : Service(), MediaPlayer.OnPreparedListener,
                                MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener {


    companion object {
        private const val  MSG_ERR_LOADING = "Error loading song from uri"
        private const val ERR_LOADING = "Error loading"
    }

    private val player: MediaPlayer = MediaPlayer().also {
        it.setOnPreparedListener(this)
        it.setOnErrorListener(this)
        it.setOnCompletionListener(this)
    }
    private lateinit var songs: List<Song>
    var songPos = 0
    private val musicBind = MusicBinder()
    private lateinit var listener: MusicServiceListener


    override fun onCreate() {
        super.onCreate()
        player.setWakeMode(applicationContext,PowerManager.PARTIAL_WAKE_LOCK)
    }

    override fun onBind(intent: Intent): IBinder?{
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        player.stop()
        player.release()
        return false
    }

    fun setSongs(songs:List<Song>){
        this.songs = songs
    }

    fun setListener(listener: MusicServiceListener){
        this.listener = listener
    }

    fun playSong(){
        player.reset()
        val playSong = songs[songPos]
        val currSong = playSong.id
        val trackUri = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            currSong
        )
        try {
            player.setDataSource(applicationContext, trackUri)
        } catch (e: Exception) {
            Log.wtf(ERR_LOADING,MSG_ERR_LOADING)
        }
        player.prepareAsync()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        listener.refreshUI(songPos)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        mp?.reset()
        playNext()
    }

    fun getPos(): Int {
        return player.currentPosition
    }

    fun getDur(): Int {
        return player.duration
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun pausePlayer() {
        player.pause()
    }

    fun seek(pos: Int) {
        player.seekTo(pos)
    }

    fun go() {
        player.start()
    }

    fun playPrev(){
        songPos--;
        if (songPos < 0) songPos = songs.size - 1
        playSong()
    }

    fun playNext(){
        songPos = (songPos + 1) % songs.size
        playSong()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }


}