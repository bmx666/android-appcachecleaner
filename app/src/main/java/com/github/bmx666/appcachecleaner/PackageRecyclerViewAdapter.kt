package com.github.bmx666.appcachecleaner

import android.graphics.Color
import android.os.Build
import android.text.format.Formatter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent.PlaceholderPackage
import com.github.bmx666.appcachecleaner.databinding.FragmentPackageBinding

class PackageRecyclerViewAdapter(
    private val values: List<PlaceholderPackage>
) : RecyclerView.Adapter<PackageRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentPackageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.packageIconView.setImageDrawable(item.icon)
        holder.packageNameView.text = item.name
        holder.packageLabelView.text = item.label
        holder.packageLabelView.setOnCheckedChangeListener(null)
        holder.packageLabelView.isChecked = item.checked
        holder.packageLabelView.setOnCheckedChangeListener { _, checked ->
            item.checked = checked
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && item.stats != null) {
            val ctx = holder.cacheSizeView.context
            holder.cacheSizeView.text = ctx.getString(R.string.text_cache_size_fmt,
                Formatter.formatShortFileSize(ctx, item.stats.cacheBytes))
        } else {
            holder.cacheSizeView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentPackageBinding) :
        RecyclerView.ViewHolder(binding.root)
    {
        val packageIconView: ImageView = binding.packageIcon
        val packageLabelView: CheckBox = binding.packageLabel
        val packageNameView: TextView = binding.packageName
        val cacheSizeView: TextView = binding.cacheSize

        override fun toString(): String {
            return super.toString() + " '" + packageNameView.text + "'"
        }
    }

}