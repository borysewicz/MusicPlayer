package com.example.musicplayer

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import com.example.musicplayer.model.Song
import com.example.musicplayer.MusicService.MusicBinder
import android.content.ContentUris
import android.util.Log







class MusicService : Service(), MediaPlayer.OnPreparedListener,
                                MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener {

    private val player: MediaPlayer = MediaPlayer().also {
        it.setOnPreparedListener(this)
        it.setOnErrorListener(this)
        it.setOnCompletionListener(this)
    }
    private lateinit var songs: List<Song>
    var songPos = 0
    private val musicBind = MusicBinder()
    private val MSG_ERR_LOADING = "Error loading song from uri"
    private val ERR_LOADING = "Error loading"

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
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCompletion(mp: MediaPlayer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }


}