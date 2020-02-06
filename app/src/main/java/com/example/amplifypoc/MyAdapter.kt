package com.example.amplifypoc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.amplify.generated.graphql.ListPetsQuery


class MyAdapter internal constructor(context: Context?) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    private var mData: List<ListPetsQuery.Item> = ArrayList()
    private val mInflater: LayoutInflater

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.recyclerview_row, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(mData[position])
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setItems(items: List<ListPetsQuery.Item>) {
        mData = items
    }

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var txt_name: TextView
        var txt_description: TextView
        fun bindData(item: ListPetsQuery.Item) {
            txt_name.text = item.name()
            txt_description.text = item.description()
        }

        init {
            txt_name = itemView.findViewById(R.id.txt_name)
            txt_description = itemView.findViewById(R.id.txt_description)
        }
    }

    init {
        mInflater = LayoutInflater.from(context)
    }
}