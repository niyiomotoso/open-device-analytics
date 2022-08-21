package com.analytics.device.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.analytics.device.R
import com.analytics.device.models.AppDetails


class TotalTimeUsedAdapter(private val mList: List<AppDetails>):
    RecyclerView.Adapter<TotalTimeUsedAdapter.ViewHolder>()
{
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.total_time_used_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return mList[position].id
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Populate the data into the template view using the data object
        val usageStats: AppDetails = mList[position]
        holder.app_name_tv.text = usageStats.appName
        holder.usage_perc_tv.text = String.format("%d%%", usageStats.statPercentage)
        holder.icon_img.setImageDrawable(usageStats.appIcon)
        holder.progressBar.progress = usageStats.statPercentage
        holder.usage_duration_tv.text = usageStats.totalTimeInForeground
    }

    // Holds the views for adding it to image and text
    class ViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
        // Lookup view for data population
        val app_name_tv = convertView.findViewById<TextView>(R.id.app_name_tv)
        val usage_duration_tv = convertView.findViewById<TextView>(R.id.usage_duration_tv)
        val usage_perc_tv = convertView.findViewById<TextView>(R.id.usage_perc_tv)
        val icon_img = convertView.findViewById<ImageView>(R.id.icon_img)
        val progressBar = convertView.findViewById<ProgressBar>(R.id.progressBar)
    }
}