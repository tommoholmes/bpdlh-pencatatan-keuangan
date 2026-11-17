package com.bpdlh.pencatatankeuangan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bpdlh.pencatatankeuangan.data.SyncScheduler
import com.bpdlh.pencatatankeuangan.ui.PencatatanNav
import com.bpdlh.pencatatankeuangan.ui.theme.PencatatanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PencatatanTheme {
                PencatatanNav((application as PencatatanApp).container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Pemicu sinkronisasi: aplikasi dibuka.
        if ((application as PencatatanApp).container.auth.currentUser != null) {
            SyncScheduler.requestNow(this)
        }
    }
}
