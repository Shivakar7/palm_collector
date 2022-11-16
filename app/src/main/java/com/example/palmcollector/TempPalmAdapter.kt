package com.example.palmcollector

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_palm.view.*

class TempPalmAdapter(
    var palmList : MutableList<Bitmap>
) : RecyclerView.Adapter<TempPalmAdapter.TempPalmViewHolder>() {

    inner class TempPalmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TempPalmViewHolder {
        return TempPalmViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_palm,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TempPalmViewHolder, position: Int){
        val model = palmList[position]

        if (holder is TempPalmAdapter.TempPalmViewHolder) {
            val myBitmap = model
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
        fun onClick(position: Int, model: Bitmap)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

}