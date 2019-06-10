package com.example.musicplayer

import android.graphics.Color
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.musicplayer.model.Song

class SongListAdapter(private  val songs: List<Song>, private val songListListener: SongListListener) : RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    private var lastSelected = -1

   inner class SongViewHolder(val cardView: CardView,val titleText:TextView, val artistText:TextView,
                         val albumText : TextView) : RecyclerView.ViewHolder(cardView){


    }

    override fun onCreateViewHolder(parent: ViewGroup, pos: Int): SongListAdapter.SongViewHolder {
        val cardView : CardView =LayoutInflater.from(parent.context)
            .inflate(R.layout.songlist_element,parent,false) as CardView
        val titleText = cardView.findViewById<TextView>(R.id.songlist_element_title)
        val albumText = cardView.findViewById<TextView>(R.id.songlist_element_album)
        val artistText = cardView.findViewById<TextView>(R.id.songlist_element_author)
        return SongViewHolder(cardView,titleText,artistText,albumText)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: SongViewHolder, pos: Int) {
        holder.titleText.text = songs[pos].title
        holder.albumText.text = songs[pos].album
        holder.artistText.text = songs[pos].artist
        holder.cardView.setOnClickListener{
            songListListener.onSongChanged(pos)
            val oldPos = lastSelected
            lastSelected = pos
            notifyItemChanged(oldPos)
            notifyItemChanged(lastSelected)
        }
        holder.itemView.setBackgroundColor(if (lastSelected == pos) Color.GREEN else Color.WHITE)
    }


}