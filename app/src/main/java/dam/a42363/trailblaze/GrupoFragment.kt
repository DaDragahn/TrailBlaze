package dam.a42363.trailblaze

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.net.toUri
import androidx.navigation.NavController
import dam.a42363.trailblaze.databinding.FragmentGrupoBinding
import dam.a42363.trailblaze.databinding.FragmentImageShareBinding

class GrupoFragment : Fragment() {

    private lateinit var navController: NavController

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var fullImage: AppCompatImageView

    var _binding: FragmentGrupoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentGrupoBinding.inflate(inflater, container, false)


        toolbar = binding.toolbar
        toolbar.inflateMenu(R.menu.invite_menu)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.invite -> {


                    true
                }
                R.id.leave -> {


                    true
                }

                R.id.editarFoto -> {


                    true
                }

                else -> false
            }
        }
        return binding.root
    }
}