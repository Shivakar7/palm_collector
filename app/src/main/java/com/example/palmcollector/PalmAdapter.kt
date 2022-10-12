package com.example.palmcollector

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_list.view.*
import java.io.File

class PalmAdapter(
    var palmList : List<File>

) : RecyclerView.Adapter<PalmAdapter.PalmViewHolder>() {

    inner class PalmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PalmViewHolder {
        return PalmViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_palm,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PalmViewHolder, position: Int){

    }

    override fun getItemCount(): Int {
        return palmList.size
    }
}