package io.github.leonidius20.recorder.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.ui.common.Adapter.ViewHolder
import io.github.leonidius20.recorder.R

class Adapter(
    private val context: Context,
    private val startingData: Array<String>,
) : RecyclerView.Adapter<ViewHolder>() {


    class ViewHolder(
        private val textView: TextView,
    ) : RecyclerView.ViewHolder(textView) {

        fun bindToText(text: String) {
            textView.text = text
        }

    }

    var data: Array<String> = startingData
        private set

    fun setData(data: Array<String>) {
        this.data = data
        notifyDataSetChanged() // todo: ListAdapter
    }

    override fun getItemCount() = data.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = (LayoutInflater.from(context).inflate(R.layout.view_selector_page, parent, false) as TextView)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val title = data[position]
        holder.bindToText(title)
    }

}