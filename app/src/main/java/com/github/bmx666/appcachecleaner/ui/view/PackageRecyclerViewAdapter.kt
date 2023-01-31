package com.github.bmx666.appcachecleaner.ui.view

import android.os.Build
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.databinding.FragmentPackageBinding
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper

class PackageRecyclerViewAdapter(
    private val values: List<PlaceholderContent.PlaceholderPackage>
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
        if (item.ignore) {
            holder.packageLayout.visibility = View.GONE
            return
        }
        holder.packageLayout.visibility = View.VISIBLE
        holder.packageNameView.text = item.name
        holder.packageLabelView.text = item.label
        holder.packageLabelView.setOnCheckedChangeListener(null)
        holder.packageLabelView.isChecked = item.checked
        holder.packageLabelView.setOnCheckedChangeListener { _, checked ->
            item.checked = checked
        }

        GlideApp.with(holder.packageIconView.context)
            .load(
                PackageManagerHelper.getApplicationIcon(
                    holder.packageIconView.context,
                    item.pkgInfo
                )
            )
            .into(holder.packageIconView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && item.stats != null) {
            val ctx = holder.cacheSizeView.context
            holder.cacheSizeView.text = ctx.getString(
                R.string.text_cache_size_fmt,
                Formatter.formatShortFileSize(ctx, item.stats!!.cacheBytes)
            )
        } else {
            holder.cacheSizeView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = values.filter { !it.ignore }.size

    inner class ViewHolder(binding: FragmentPackageBinding) :
        RecyclerView.ViewHolder(binding.root)
    {
        val packageLayout: LinearLayout = binding.packageLayout
        val packageIconView: ImageView = binding.packageIcon
        val packageLabelView: CheckBox = binding.packageLabel
        val packageNameView: TextView = binding.packageName
        val cacheSizeView: TextView = binding.cacheSize

        override fun toString(): String {
            return super.toString() + " '" + packageNameView.text + "'"
        }
    }

}