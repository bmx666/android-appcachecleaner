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
import com.github.bmx666.appcachecleaner.ui.activity.AppCacheCleanerActivity
import com.github.bmx666.appcachecleaner.ui.view.PackageRecyclerViewAdapter


class PackageListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var onSwapAdapter: (List<PlaceholderContent.PlaceholderPackage>) -> Unit
    private lateinit var onRefreshAdapter: (List<PlaceholderContent.PlaceholderPackage>) -> Unit

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_package_list, container, false)

        val hideStats = arguments?.getBoolean(
            Constant.Bundle.PackageFragment.KEY_HIDE_STATS) ?: false

        val onChecked: (() -> Unit)? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                {
                    (activity as AppCacheCleanerActivity?)?.showTotalCacheSizeOfCheckedPackages()
                }
            } else {
                null
            }

        // Set the adapter
        if (view is RecyclerView) {
            recyclerView = view
            with(view) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context.applicationContext)
                addOverlayJob(
                    suspendCallback = {
                        PlaceholderContent.Current.getVisible()
                    },
                    postUiCallback = { pkgList ->
                        adapter = PackageRecyclerViewAdapter(pkgList, hideStats, onChecked)
                    },
                )
            }

            onSwapAdapter = { pkgList ->
                view.swapAdapter(
                    PackageRecyclerViewAdapter(pkgList, hideStats, onChecked),
                    true)
            }

            onRefreshAdapter = { pkgList ->
                view.adapter?.notifyItemRangeChanged(0, pkgList.size)
            }
        }
        return view
    }

    fun refreshAdapter() {
        addOverlayJob(
            suspendCallback = {
                PlaceholderContent.Current.getVisible()
            },
            postUiCallback = onRefreshAdapter
        )
    }

    fun swapAdapterFilterByName(text: String) {
        addOverlayJob(
            suspendCallback = {
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getFilteredByName(text))
                PlaceholderContent.Current.getVisible()
            },
            postUiCallback = onSwapAdapter
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun swapAdapterFilterByCacheBytes(minCacheBytes: Long) {
        addOverlayJob(
            suspendCallback = {
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getFilteredByCacheSize(minCacheBytes)
                )
                PlaceholderContent.Current.getVisible()
            },
            postUiCallback = onSwapAdapter
        )
    }

    private fun addOverlayJob(
        suspendCallback: suspend () -> List<PlaceholderContent.PlaceholderPackage>,
        postUiCallback: ((List<PlaceholderContent.PlaceholderPackage>) -> Unit)? = null
    ) {
        (activity as AppCacheCleanerActivity?)
            ?.addOverlayJob(suspendCallback, postUiCallback)
    }

    companion object {
        @JvmStatic
        fun newInstance() = PackageListFragment()
    }
}