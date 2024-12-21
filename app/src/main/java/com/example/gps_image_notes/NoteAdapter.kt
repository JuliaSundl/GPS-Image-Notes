package com.example.gps_image_notes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class NoteAdapter(context:Context, var notes:List<Note>):BaseAdapter() {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return notes.size
    }

    override fun getItem(position: Int): Note {
        return notes[position]
    }

    override fun getItemId(position: Int): Long {
        return notes[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_view, parent, false)

            holder = ViewHolder(
                // Find views by ID: title and message
                titleView = view.findViewById(R.id.titleView),
                messageView = view.findViewById(R.id.messageView)
            )

            // Connect holder with View
            view.tag = holder
        } else {
            // Recycle existing View
            view = convertView
            holder = view.tag as ViewHolder
        }

        val note: Note = getItem(position)
        holder.titleView.text = note.title
        holder.messageView.text = note.message

        return view
    }

    // New Class ViewHolder
    private data class ViewHolder(
        val titleView: TextView,
        val messageView: TextView
    )

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}