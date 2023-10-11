package com.example.myapplication.adaptors

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.dataclasses.UserTripRecord


class UserTripAdaptor (private val userTripRecord:MutableList<UserTripRecord>): RecyclerView.Adapter<UserTripAdaptor.ViewHolder>() {

    private val colors = arrayOf("#E1BEE7","#D1C4E9", "#C5CAE9", "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB", "#C8E6C9")
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val originText: TextView = itemView.findViewById(R.id.originTextViewProfile)
        val destinationTest : TextView = itemView.findViewById(R.id.destinationTextViewProfile)
        val busIDText : TextView = itemView.findViewById(R.id.busIDTextViewProfile)
        val costText: TextView = itemView.findViewById(R.id.costTextViewProfile)
        val endTimeText: TextView = itemView.findViewById(R.id.endTimeTextViewProfile)
        val startTimeText: TextView = itemView.findViewById(R.id.startTimeTextViewProfile)
        val distanceText : TextView = itemView.findViewById(R.id.distanceTextViewProfile)
        val recordLinearLayout : LinearLayout =itemView.findViewById(R.id.recordLinearLayout)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.a_user_trip_record,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val color = colors[position % colors.size]
        val record = userTripRecord[position]


        if (record.origin == null){
            holder.originText.text= "No Data"
        }else{
            holder.originText.text = record.origin
        }


        if (record.destination == null){
            holder.destinationTest.text = "No Data"
        }else{
            holder.destinationTest.text = record.destination
        }

        holder.busIDText.text = record.busID
        holder.costText.text = record.cost
        holder.endTimeText.text = record.endTime?.values.toString()
        holder.distanceText.text = record.distance
        holder.startTimeText.text = record.startTime?.values.toString()
        holder.recordLinearLayout.setBackgroundColor(Color.parseColor(color))
    }


    override fun getItemCount(): Int {
        return userTripRecord.size
    }
}