package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import com.example.musicplayer.model.Song
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.graphics.Rect
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import android.content.ServiceConnection
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.MediaController


class MainActivity : AppCompatActivity(), SongListListener, MediaController.MediaPlayerControl, MusicServiceListener {

    private var songList : MutableList<Song> = mutableListOf()
    private lateinit var songAdapter : SongListAdapter
    private lateinit var musicService: MusicService
    private lateinit var controller: MusicController
    private var playIntent : Intent? = null
    private var isBound = false
    private var hasPermission = false

    companion object {
        private const val MARGIN = 8
        private const val READ_PERM_KEY = 667
    }

    private val musicConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService.setSongs(songList)
            musicService.setListener(this@MainActivity)
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val songView = findViewById<RecyclerView>(R.id.home_songList_RV)
        handlePermissions()
        songAdapter = SongListAdapter(songList, this)
        val viewManager = LinearLayoutManager(this)
        songView.apply{
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = songAdapter
            addItemDecoration(SpacesItemDecoration(MARGIN))
        }
        setController()
        setSupportActionBar(findViewById(R.id.main_toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.home_shuffle_button -> musicService.isShuffling = !musicService.isShuffling
        }
        return true
    }


    override fun onStart() {
        super.onStart()
        if(playIntent == null){
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    private fun setController(){
       if (!this::controller.isInitialized) {
           controller = MusicController(this)
           controller.setPrevNextListeners({ playNext() }, { playPrev() })
           controller.setMediaPlayer(this)
           controller.setAnchorView(findViewById(R.id.home_songList_RV))
           controller.isEnabled = true
       }
    }

    private fun playNext() {
        musicService.playNext()
    }

    private fun playPrev() {
        musicService.playPrev()
    }

    // below are the functions overridden for MediaController widget

    override fun isPlaying(): Boolean {
        if (!this::musicService.isInitialized){
            return false
        }
        return musicService.isPlaying()
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        return if (isPlaying){
            musicService.getDur()
        } else 0
    }

    override fun pause() {
        musicService.pausePlayer()
        }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun seekTo(pos: Int) {
        musicService.seek(pos)
    }

    override fun getCurrentPosition(): Int {
        return if (isPlaying){
            musicService.getPos()
        } else 0
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {
        musicService.go()
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun canPause(): Boolean {
       return true
    }

    // function overriden from SongListListener
    override fun onSongChanged(songId: Int) {
        musicService.songPos = songId
        musicService.playSong()
    }

    // function overriden from MusicServiceListener
    override fun refreshUI(songPos : Int) {
       controller.show()
        songAdapter.refreshSelectedSong(songPos)
    }

    private fun handlePermissions() {
        if (!checkReadPermissions()){
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_PERM_KEY)
        }
        else{
            getSongList()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            READ_PERM_KEY ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    hasPermission = true
                    getSongList()
                }
            else{
                    finish()
                }
        }
    }

    private fun checkReadPermissions() : Boolean{
       return  (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun getSongList(){
        val musicResolver = contentResolver
       val musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf( MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST,
                                    MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM
        )
        val musicCursor = musicResolver.query(musicUri, projection, null, null, projection[2]) // last arg -  sorting by title
        if (musicCursor != null && musicCursor.moveToFirst()) {

            do {
                val id = musicCursor.getLong(musicCursor.getColumnIndex(projection[0]))
                val artist = musicCursor.getString(musicCursor.getColumnIndex(projection[1]))
                val title = musicCursor.getString(musicCursor.getColumnIndex(projection[2]))
                val album = musicCursor.getString(musicCursor.getColumnIndex(projection[3]))
                songList.add(Song(id, title, artist,album))
            } while (musicCursor.moveToNext())
        }
        musicCursor.close()
    }

    override fun onDestroy() {
        stopService(playIntent)
        super.onDestroy()
    }
}

class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left =  space
        outRect.right =  space
        outRect.bottom = space

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = space
        }
    }
}
