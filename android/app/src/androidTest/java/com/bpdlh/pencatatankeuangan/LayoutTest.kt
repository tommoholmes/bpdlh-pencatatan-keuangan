package com.bpdlh.pencatatankeuangan

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bpdlh.pencatatankeuangan.data.PlaceSnapshot
import com.bpdlh.pencatatankeuangan.ui.detail.DetailRoot
import com.bpdlh.pencatatankeuangan.ui.editor.EditorRoot
import com.bpdlh.pencatatankeuangan.ui.login.LoginRoot
import com.bpdlh.pencatatankeuangan.ui.records.RecordsRoot
import com.bpdlh.pencatatankeuangan.ui.settings.SettingsRoot
import com.bpdlh.pencatatankeuangan.ui.theme.PencatatanTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Semua konten harus berada di dalam lebar layar pada lebar berapa pun. Ini menangkap
 * teks yang meluber dan elemen yang terpotong, yang tidak terlihat di pratinjau lebar.
 */
@RunWith(AndroidJUnit4::class)
class LayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private val container: AppContainer
        get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PencatatanApp).container

    private fun assertNothingExceedsScreen(label: String) {
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val nodes = compose.onRoot().fetchSemanticsNode()
        val all = collect(nodes)
        val offenders = all.filter { it.boundsInRoot.left < -1f || it.boundsInRoot.right > screen.width + 1f }
        assertTrue(
            "$label: ${offenders.size} node keluar dari lebar layar ${screen.width}: " +
                offenders.take(3).joinToString { it.config.toString().take(80) },
            offenders.isEmpty(),
        )
    }

    private fun collect(node: androidx.compose.ui.semantics.SemanticsNode): List<androidx.compose.ui.semantics.SemanticsNode> =
        listOf(node) + node.children.flatMap { collect(it) }

    @Before
    fun seed() {
        runBlocking {
            container.records.wipeLocal()
            container.records.save(
                null,
                "Kopi susu karamel ukuran besar dengan es",
                18_000,
                null,
                PlaceSnapshot(null, null, null),
            )
            container.records.save(null, "Bensin", 45_000, null, PlaceSnapshot(-7.7956, 110.3695, 12.0))
        }
    }

    @Test
    fun loginFitsScreen() {
        compose.setContent { PencatatanTheme { LoginRoot(container) } }
        compose.waitForIdle()
        assertNothingExceedsScreen("login")
    }

    @Test
    fun recordsFitsScreen() {
        compose.setContent {
            PencatatanTheme {
                RecordsRoot(container, onOpenRecord = {}, onAddRecord = {}, onOpenSettings = {})
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Bensin").fetchSemanticsNodes().isNotEmpty()
        }
        assertNothingExceedsScreen("records")
    }

    @Test
    fun editorFitsScreen() {
        compose.setContent {
            PencatatanTheme { EditorRoot(container, recordId = null, onBack = {}, onOpenSettings = {}) }
        }
        compose.waitForIdle()
        assertNothingExceedsScreen("editor")
    }

    @Test
    fun detailFitsScreen() {
        val id = runBlocking { container.records.observeActive().first().first { it.name == "Bensin" }.id }
        compose.setContent {
            PencatatanTheme { DetailRoot(container, recordId = id, onBack = {}, onEdit = {}) }
        }
        compose.waitForIdle()
        assertNothingExceedsScreen("detail")
    }

    @Test
    fun settingsFitsScreenAndReachesLastAction() {
        compose.setContent {
            PencatatanTheme { SettingsRoot(container, onBack = {}, onSignedOut = {}) }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Hapus akun").performScrollTo()
        assertNothingExceedsScreen("settings")
    }
}
