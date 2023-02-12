package com.github.bmx666.appcachecleaner.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.ui.view.PackageRecyclerViewAdapter

class PackageListFragment(private val hideStats: Boolean) : Fragment() {

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
                adapter = PackageRecyclerViewAdapter(PlaceholderContent.getVisibleItems(), hideStats)
            }
            onUpdateAdapter = {
                try {
                    view.swapAdapter(
                        PackageRecyclerViewAdapter(PlaceholderContent.getVisibleItems(), hideStats),
                        true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return view
    }

    fun updateAdapterFilterByName(text: String) {
        PlaceholderContent.filterByName(text)
        onUpdateAdapter()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateAdapterFilterByMinCacheBytes(minCacheBytes: Long) {
        PlaceholderContent.filterByCacheSize(minCacheBytes)
        onUpdateAdapter()
    }

    companion object {
        @JvmStatic
        fun newInstance(hideStats: Boolean) = PackageListFragment(hideStats)
    }
}