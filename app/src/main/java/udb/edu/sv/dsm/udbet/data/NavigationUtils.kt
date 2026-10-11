package udb.edu.sv.dsm.udbet.data

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import udb.edu.sv.dsm.udbet.MainActivity

const val EXTRA_SELECTED_TAB = "extra_selected_tab"

fun AppCompatActivity.setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
    bottomNavigationView.setOnItemSelectedListener { item ->
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_SELECTED_TAB, item.itemId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
        true
    }
}