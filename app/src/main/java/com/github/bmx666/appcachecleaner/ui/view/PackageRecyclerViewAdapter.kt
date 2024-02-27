package com.github.bmx666.appcachecleaner.ui.view

import android.content.pm.PackageInfo
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.databinding.FragmentPackageBinding
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.util.ActivityHelper
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.toFormattedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.util.unit.DataSize


class PackageRecyclerViewAdapter(
    private val values: List<PlaceholderContent.PlaceholderPackage>,
    private val hideStats: Boolean
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
        holder.packageLayout.visibility = View.VISIBLE
        holder.packageNameView.text = item.name
        holder.packageLabelView.text = item.label
        holder.packageLabelView.setOnCheckedChangeListener(null)
        holder.packageLabelView.isChecked = item.checked
        holder.packageLabelView.setOnCheckedChangeListener { _, checked ->
            item.checked = checked
        }

        holder.loadIcon(item.pkgInfo)

        holder.packageIconView.setOnClickListener {
            Toast.makeText(holder.packageIconView.context,
                R.string.toast_package_list_item_long_click,
                Toast.LENGTH_SHORT).show()
        }
        holder.packageIconView.setOnLongClickListener {
            ActivityHelper.startApplicationDetailsActivity(
                holder.packageIconView.context,
                holder.packageNameView.text.toString()
            )
            true
        }

        val showStats = item.stats != null && !hideStats
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && showStats) {
            val ctx = holder.cacheSizeView.context
            holder.cacheSizeView.text = ctx.getString(
                R.string.text_cache_size_fmt,
                DataSize.ofBytes(item.stats!!.cacheBytes).toFormattedString(ctx)
            )
        } else {
            holder.cacheSizeView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = values.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    inner class ViewHolder(binding: FragmentPackageBinding) :
        RecyclerView.ViewHolder(binding.root)
    {
        val packageLayout: LinearLayout = binding.packageLayout
        val packageIconView: ImageView = binding.packageIcon
        val packageLabelView: CheckBox = binding.packageLabel
        val packageNameView: TextView = binding.packageName
        val cacheSizeView: TextView = binding.cacheSize

        private var loadIconJob: Job? = null

        fun loadIcon(pkgInfo: PackageInfo) {
            loadIconJob?.cancel()
            loadIconJob =
                CoroutineScope(Dispatchers.IO).launch {
                    val icon =
                        PackageManagerHelper.getApplicationIcon(
                            packageIconView.context,
                            pkgInfo
                        )

                    withContext(Dispatchers.Main) {
                        Glide.with(packageIconView.context)
                            .load(icon)
                            .into(packageIconView)
                    }
                }
        }

        fun clear() {
            loadIconJob?.cancel()
        }

        override fun toString(): String {
            return super.toString() + " '" + packageNameView.text + "'"
        }
    }

}