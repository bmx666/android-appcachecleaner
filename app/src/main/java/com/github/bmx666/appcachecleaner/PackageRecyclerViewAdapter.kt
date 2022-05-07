package com.github.bmx666.appcachecleaner

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast

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
        holder.packageNameView.text = item.name
        holder.packageLabelView.text = item.label
        holder.packageLabelView.setOnCheckedChangeListener(null)
        holder.packageLabelView.isChecked = item.checked
        holder.packageLabelView.setOnCheckedChangeListener { _, checked ->
            item.checked = checked
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentPackageBinding) :
        RecyclerView.ViewHolder(binding.root)
    {
        val packageLabelView: CheckBox = binding.packageLabel
        val packageNameView: TextView = binding.packageName

        override fun toString(): String {
            return super.toString() + " '" + packageNameView.text + "'"
        }
    }

}