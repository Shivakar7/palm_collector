package com.example.palmcollector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_list.view.*

class SubjectAdapter(
    var subjects : MutableList<Subject>
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        return SubjectViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_list,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val model = subjects[position]

        if (holder is SubjectViewHolder) {
            holder.itemView.tvSubjectID.text = model.subjectID
            holder.itemView.tvLeftCount.text = model.leftList.size.toString()
            holder.itemView.tvRightCount.text = model.rightList.size.toString()

            holder.itemView.setOnClickListener {

                if (onClickListener != null) {
                    onClickListener!!.onClick(position, model)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return subjects.size
    }

    interface OnClickListener {
        fun onClick(position: Int, model: Subject)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
}