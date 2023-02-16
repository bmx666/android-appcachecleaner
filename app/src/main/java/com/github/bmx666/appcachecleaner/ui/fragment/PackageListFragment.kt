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
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.ui.view.PackageRecyclerViewAdapter

class PackageListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var onSwapAdapter: () -> Unit
    private lateinit var onRefreshAdapter: () -> Unit

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_package_list, container, false)

        val hideStats = arguments?.getString(
            Constant.Bundle.PackageFragment.KEY_CUSTOM_LIST_NAME)?.let { true } ?: false

        // Set the adapter
        if (view is RecyclerView) {
            recyclerView = view
            with(view) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context.applicationContext)
                adapter = PackageRecyclerViewAdapter(PlaceholderContent.getVisibleItems(), hideStats)
            }
            onSwapAdapter = {
                try {
                    view.swapAdapter(
                        PackageRecyclerViewAdapter(PlaceholderContent.getVisibleItems(), hideStats),
                        true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onRefreshAdapter = {
                try {
                    view.adapter?.notifyItemRangeChanged(0, PlaceholderContent.getVisibleItems().size)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return view
    }

    fun refreshAdapter() {
        onRefreshAdapter()
    }

    fun swapAdapterFilterByName(text: String) {
        PlaceholderContent.filterByName(text)
        onSwapAdapter()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun swapAdapterFilterByCacheBytes(minCacheBytes: Long) {
        PlaceholderContent.filterByCacheSize(minCacheBytes)
        onSwapAdapter()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PackageListFragment()
    }
}