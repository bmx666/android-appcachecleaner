package com.github.bmx666.appcachecleaner.data

import android.content.pm.PackageInfo
import android.os.Build
import com.github.bmx666.appcachecleaner.fake.FakePackageSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

// Robolectric only to mint real PackageInfo handles; all asserted logic is pure (filter /
// sort / selection / post-clean write-back) and runs on virtual time.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class PackageRepositoryTest {

    private val source = FakePackageSource()
    private fun repo() = PackageRepository(source)
    private fun pkg(name: String) = PackageInfo().apply { packageName = name }

    @Test
    fun `sort by label keeps checked first then alphabetical`() = runTest {
        val repo = repo()
        repo.add(pkg("com.b"), "Bravo", Locale.US, 100)
        repo.add(pkg("com.a"), "Alpha", Locale.US, 200)
        repo.add(pkg("com.c"), "Charlie", Locale.US, 300)
        repo.setChecked("com.c", true)
        repo.applySortByLabel()

        assertEquals(
            listOf("Charlie", "Alpha", "Bravo"),
            repo.visiblePackages.value.map { it.label },
        )
    }

    @Test
    fun `checked total cache bytes sums only checked`() = runTest {
        val repo = repo()
        repo.add(pkg("com.a"), "A", Locale.US, 200)
        repo.add(pkg("com.b"), "B", Locale.US, 50)
        repo.applySortByLabel()

        repo.setChecked("com.a", true)
        assertEquals(setOf("com.a"), repo.checked.value)
        assertEquals(200L, repo.checkedTotalCacheBytes.value)

        repo.setChecked("com.b", true)
        assertEquals(250L, repo.checkedTotalCacheBytes.value)
    }

    @Test
    fun `filter by name matches case-insensitive substring`() = runTest {
        val repo = repo()
        repo.add(pkg("com.a"), "Telegram", Locale.US, 0)
        repo.add(pkg("com.b"), "Chrome", Locale.US, 0)
        repo.applyFilterByName("gram")

        assertEquals(listOf("Telegram"), repo.visiblePackages.value.map { it.label })
    }

    @Test
    fun `filter by cache size keeps only above threshold sorted desc`() = runTest {
        val repo = repo()
        repo.add(pkg("com.small"), "Small", Locale.US, 100)
        repo.add(pkg("com.big"), "Big", Locale.US, 5000)
        repo.add(pkg("com.mid"), "Mid", Locale.US, 1000)
        repo.applyFilterByCacheSize(500)

        assertEquals(
            listOf("com.big", "com.mid"),
            repo.visiblePackages.value.map { it.name },
        )
    }

    @Test
    fun `reset hides all and clears selection`() = runTest {
        val repo = repo()
        repo.add(pkg("com.a"), "A", Locale.US, 10)
        repo.setChecked("com.a", true)
        repo.applySortByLabel()

        repo.reset()
        repo.applySortByLabel()

        assertEquals(emptyList<String>(), repo.visiblePackages.value.map { it.name })
        assertEquals(emptySet<String>(), repo.checked.value)
    }

    @Test
    fun `refresh after clean returns freed bytes and updates warm cache`() = runTest {
        val repo = repo()
        repo.add(pkg("com.a"), "A", Locale.US, 500)
        repo.applySortByLabel()
        repo.setChecked("com.a", true)

        // Simulate the post-clean size drop the source will report.
        source.cacheBytes["com.a"] = 50

        val cleaned = repo.refreshStatsAfterCacheClean()

        assertEquals(450L, cleaned)
        assertEquals(50L, repo.visiblePackages.value.first().cacheBytes)
    }
}
