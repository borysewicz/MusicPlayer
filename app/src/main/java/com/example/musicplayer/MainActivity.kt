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






class MainActivity : AppCompatActivity(), SongListListener {

    private lateinit var songList : MutableList<Song>
    private lateinit var songView : RecyclerView
    private lateinit var musicService: MusicService
    private var playIntent : Intent? = null
    private var isBound = false
    private val READ_PERM_KEY = 667
    private var hasPermission = false
    private val MARGIN = 8

    val musicConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService.setSongs(songList)
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        songView = findViewById(R.id.home_songList_RV)
        songList = mutableListOf()
        handlePermissions()
        val songAdapter : SongListAdapter = SongListAdapter(songList, this)
        val viewManager = LinearLayoutManager(this)
        val rv = findViewById<RecyclerView>(R.id.home_songList_RV).apply{
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = songAdapter
        }
        rv.addItemDecoration(SpacesItemDecoration(MARGIN))
    }

    override fun onStart() {
        super.onStart()
        if(playIntent == null){
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    override fun onSongChanged(songId: Int) {
        musicService.songPos = songId
        musicService.playSong()
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
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM
        )
        val musicCursor = musicResolver.query(musicUri, projection, null, null, projection[2])
        if (musicCursor != null && musicCursor.moveToFirst()) {
            val titleColumn = musicCursor.getColumnIndex(projection[2])
            val idColumn = musicCursor.getColumnIndex(projection[0])
            val artistColumn = musicCursor.getColumnIndex(projection[1])
            val albumColumn = musicCursor.getColumnIndex(projection[3])
            do {
                val id = musicCursor.getLong(idColumn)
                val title = musicCursor.getString(titleColumn)
                val artist = musicCursor.getString(artistColumn)
                val album = musicCursor.getString(albumColumn)
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
        outRect.left = 2 * space
        outRect.right = 2 * space
        outRect.bottom = space

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = space
        }
    }
}
