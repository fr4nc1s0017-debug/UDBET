package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import udb.edu.sv.dsm.udbet.data.EXTRA_SELECTED_TAB
import udb.edu.sv.dsm.udbet.fragment.BusquedaFragment
import udb.edu.sv.dsm.udbet.fragment.DeportesFragment
import udb.edu.sv.dsm.udbet.fragment.HistorialFragment
import udb.edu.sv.dsm.udbet.fragment.HomeFragment
import udb.edu.sv.dsm.udbet.fragment.PlaceholderFragment

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: Fragment
    private lateinit var searchFragment: Fragment
    private lateinit var sportsFragment: Fragment
    private lateinit var casinoFragment: Fragment
    private lateinit var historyFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            searchFragment = BusquedaFragment()
            sportsFragment = DeportesFragment()
            casinoFragment = PlaceholderFragment.newInstance("Casino", android.R.drawable.ic_menu_gallery)
            historyFragment = HistorialFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, historyFragment, TAG_HISTORY).hide(historyFragment)
                .add(R.id.fragmentContainer, casinoFragment, TAG_CASINO).hide(casinoFragment)
                .add(R.id.fragmentContainer, sportsFragment, TAG_SPORTS).hide(sportsFragment)
                .add(R.id.fragmentContainer, searchFragment, TAG_SEARCH).hide(searchFragment)
                .add(R.id.fragmentContainer, homeFragment, TAG_HOME)
                .commit()

            activeFragment = homeFragment
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME)!!
            searchFragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH)!!
            sportsFragment = supportFragmentManager.findFragmentByTag(TAG_SPORTS)!!
            casinoFragment = supportFragmentManager.findFragmentByTag(TAG_CASINO)!!
            historyFragment = supportFragmentManager.findFragmentByTag(TAG_HISTORY)!!

            activeFragment = listOf(homeFragment, searchFragment, sportsFragment, casinoFragment, historyFragment)
                .first { !it.isHidden }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val selected: Fragment? = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_search -> searchFragment
                R.id.nav_sports -> sportsFragment
                R.id.nav_casino -> casinoFragment
                R.id.nav_history -> historyFragment
                else -> null
            }

            if (selected != null && selected !== activeFragment) {
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(selected)
                    .commit()
                activeFragment = selected
            }

            selected != null
        }

        aplicarPestanaSolicitada(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        aplicarPestanaSolicitada(intent)
    }

    private fun aplicarPestanaSolicitada(intent: Intent) {
        val tabId = intent.getIntExtra(EXTRA_SELECTED_TAB, -1)
        if (tabId != -1) {
            findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = tabId
        }
    }

    fun navigateToTab(itemId: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = itemId
    }

    companion object {
        private const val TAG_HOME = "inicio"
        private const val TAG_SEARCH = "busqueda"
        private const val TAG_SPORTS = "deportes"
        private const val TAG_CASINO = "casino"
        private const val TAG_HISTORY = "historial"
    }
}