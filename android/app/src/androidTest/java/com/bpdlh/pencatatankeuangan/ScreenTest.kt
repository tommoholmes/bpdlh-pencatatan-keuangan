package com.bpdlh.pencatatankeuangan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bpdlh.pencatatankeuangan.data.PlaceSnapshot
import com.bpdlh.pencatatankeuangan.data.Record
import com.bpdlh.pencatatankeuangan.ui.detail.DetailRoot
import com.bpdlh.pencatatankeuangan.ui.editor.EditorRoot
import com.bpdlh.pencatatankeuangan.ui.records.RecordsRoot
import com.bpdlh.pencatatankeuangan.ui.settings.SettingsRoot
import com.bpdlh.pencatatankeuangan.ui.theme.PencatatanTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menjalankan layar sungguhan dengan database perangkat, tanpa login. Membuktikan keadaan kosong,
 * keadaan berisi, validasi editor, dan tombol yang benar-benar bekerja.
 */
@RunWith(AndroidJUnit4::class)
class ScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val container: AppContainer
        get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PencatatanApp).container

    @Before
    fun reset() = runBlocking { container.records.wipeLocal() }

    private fun active(): List<Record> = runBlocking { container.records.observeActive().first() }

    @Test
    fun recordsShowsEmptyStateAndItsAction() {
        var addClicks = 0
        compose.setContent {
            PencatatanTheme {
                RecordsRoot(container, onOpenRecord = {}, onAddRecord = { addClicks++ }, onOpenSettings = {})
            }
        }

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Belum ada catatan").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Belum ada catatan").assertIsDisplayed()
        compose.onNodeWithText("Catat pengeluaran").performClick()
        assert(addClicks == 1) { "Aksi catat pengeluaran tidak memanggil callback" }
    }

    @Test
    fun savedRecordAppearsInListAndOpens() {
        val id = runBlocking {
            container.records.save(null, "Kopi susu", 18_000, null, PlaceSnapshot(null, null, null))
        }
        var opened: String? = null
        compose.setContent {
            PencatatanTheme {
                RecordsRoot(container, onOpenRecord = { opened = it }, onAddRecord = {}, onOpenSettings = {})
            }
        }

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kopi susu").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Kopi susu").assertIsDisplayed()
        compose.onNodeWithText("Rp 18.000").assertIsDisplayed()
        compose.onNodeWithText("1 catatan · Rp 18.000").assertIsDisplayed()
        compose.onNodeWithText("menunggu dikirim").assertIsDisplayed()

        compose.onNodeWithText("Kopi susu").performClick()
        assert(opened == id) { "Baris tidak membuka catatan yang benar" }
    }

    @Test
    fun editorRejectsEmptyFieldsThenSaves() {
        compose.setContent {
            PencatatanTheme {
                EditorRoot(container, recordId = null, onBack = {}, onOpenSettings = {})
            }
        }

        compose.onNodeWithText("Simpan").performClick()
        compose.onNodeWithText("Nama belum diisi.").assertIsDisplayed()
        compose.onNodeWithText("Harga belum diisi.").assertIsDisplayed()

        compose.onNodeWithText("Nama").performTextInput("Bakso")
        compose.onNodeWithText("Harga (Rp)").performTextInput("25000")
        compose.onNodeWithText("Simpan").performClick()

        compose.waitUntil(timeoutMillis = 10_000) { active().isNotEmpty() }
        val saved = active().single()
        assert(saved.name == "Bakso" && saved.price == 25_000L) { "Catatan tidak tersimpan dengan benar" }
    }

    @Test
    fun detailShowsSavedFields() {
        val id = runBlocking {
            container.records.save(null, "Parkir", 5_000, null, PlaceSnapshot(-7.7956, 110.3695, 12.0))
        }
        compose.setContent {
            PencatatanTheme {
                DetailRoot(container, recordId = id, onBack = {}, onEdit = {})
            }
        }

        compose.onNodeWithText("Parkir").assertIsDisplayed()
        compose.onNodeWithText("Rp 5.000").assertIsDisplayed()
        compose.onNodeWithText("-7.79560, 110.36950").assertIsDisplayed()
        compose.onNodeWithText("Buka di aplikasi peta").assertIsDisplayed()
    }

    @Test
    fun settingsShowsAccountSyncAndPrivacy() {
        compose.setContent {
            PencatatanTheme {
                SettingsRoot(container, onBack = {}, onSignedOut = {})
            }
        }

        compose.onNodeWithText("Simpan lokasi otomatis").assertIsDisplayed()
        compose.onNodeWithText("Kirim laporan masalah").performClick()
        compose.onNodeWithText("Hapus akun").assertIsDisplayed()
    }

    @Test
    fun recordsShowsConnectionState() {
        compose.setContent {
            PencatatanTheme {
                RecordsRoot(container, onOpenRecord = {}, onAddRecord = {}, onOpenSettings = {})
            }
        }

        val indicator = hasText("Online") or hasText("Offline")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(indicator).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
