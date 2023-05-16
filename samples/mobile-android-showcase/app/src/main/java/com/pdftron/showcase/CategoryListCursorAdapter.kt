package com.pdftron.showcase

import android.content.Context
import android.database.Cursor
import androidx.cursoradapter.widget.CursorAdapter
import androidx.appcompat.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class CategoryListCursorAdapter(val context: Context, cursor: Cursor, val main: MainActivity, val searchView: SearchView) : androidx.cursoradapter.widget.CursorAdapter(context, cursor, true) {

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(R.layout.item_category_title, parent, false)

    }

    private fun getString(cursor: Cursor, key: String): String {
        return cursor.getString(cursor.getColumnIndexOrThrow(key))
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val text = view.findViewById(R.id.nameLabel) as TextView
        val id = cursor.getLong(cursor.getColumnIndex("_id"))
        text.text = getString(cursor, "name")
        view.setOnClickListener {
            main.updateSearchCategory(id)
        }
    }
}