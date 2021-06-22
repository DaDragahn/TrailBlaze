package dam.a42363.trailblaze

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    var bottomNavigationView: BottomNavigationView? = null
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide() //it hides actions bar

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.explorarFragment,
                R.id.guardadosFragment,
                R.id.contribuirFragment,
                R.id.perfilFragment
            )
        )

        bottomNavigationView?.setupWithNavController(navController)
    }
}