package com.example.palmcollector

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_palm.view.*

class PalmAdapter(
    var palmList : MutableList<SubjectMetaData>

) : RecyclerView.Adapter<PalmAdapter.PalmViewHolder>() {

    inner class PalmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var onClickListener: OnClickListener? = null

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
        val model = palmList[position]

        if (holder is PalmAdapter.PalmViewHolder) {
            val myBitmap = BitmapFactory.decodeFile(model.Image.getAbsolutePath())
            holder.itemView.ivPalmImage.setImageBitmap(myBitmap)

            holder.itemView.setOnClickListener {

                if (onClickListener != null) {
                    onClickListener!!.onClick(position, model)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return palmList.size
    }

    interface OnClickListener {
        fun onClick(position: Int, model: SubjectMetaData)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

}

