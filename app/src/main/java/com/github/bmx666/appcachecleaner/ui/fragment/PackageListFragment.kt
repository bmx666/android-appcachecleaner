package com.github.bmx666.appcachecleaner.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.ui.view.PackageRecyclerViewAdapter

class PackageListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var onUpdateAdapter: () -> Unit

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_package_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            recyclerView = view
            with(view) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context.applicationContext)
                adapter = PackageRecyclerViewAdapter(PlaceholderContent.getItems())
            }
            onUpdateAdapter = {
                try {
                    view.swapAdapter(
                        PackageRecyclerViewAdapter(PlaceholderContent.getItems()),
                        true)
                } catch (e: Exception) {}
            }
        }
        return view
    }

    fun updateAdapter(text: String) {
        if (text.trim().isEmpty()) {
            PlaceholderContent.getItems().forEach { it.ignore = false }
        } else {
            PlaceholderContent.getItems().forEach { it.ignore = true }
            PlaceholderContent.getItems()
                .filter { it.label.lowercase().contains(text.lowercase()) }
                .forEach { it.ignore = false }
        }
        PlaceholderContent.sortByLabel()

        onUpdateAdapter()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PackageListFragment()
    }
}