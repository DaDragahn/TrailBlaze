package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import dam.a42363.trailblaze.databinding.FragmentFullInfoBinding

class FullInfoFragment : Fragment() {

    private val ICON_GEOJSON_SOURSE_ID = "icon-source-id"
    private val ICON_GEOJSON_Layer_ID = "icon-layer-id"

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var locationComponent: LocationComponent
    private lateinit var permissionsManager: PermissionsManager

    lateinit var navController: NavController
    private var _binding: FragmentFullInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var cardView: CardView

    private lateinit var db: FirebaseFirestore


    @SuppressLint("UseCompatLoadingForColorStateLists")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_access_token)) }
        _binding = FragmentFullInfoBinding.inflate(inflater, container, false)

        cardView = binding.cardViewInfo

        binding.iniciarBtn.backgroundTintList =
            view?.resources?.getColorStateList(R.color.trailGreen)


        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        cardView.setBackgroundResource(R.drawable.cardview_info)

        db = FirebaseFirestore.getInstance()

        val feature = arguments?.get("feature")

        db.collection("locations").document("$feature").get().addOnCompleteListener {
            if (it.isSuccessful) {
                val document = it.result
                val route = document?.getString("route")


                binding.nome.text = document?.getString("nome")
                binding.localidade.text = document?.getString("localidade")
                binding.dificuldade.text =
                    "Dificuldade: ${document?.getString("dificuldade")}"
                binding.modalidade.text = document?.getString("modalidade")
                binding.autor.text = document?.getString("autor");
                binding.descricao.text = document?.getString("descricao")

                binding.iniciarBtn.setOnClickListener {
                    val bundle = bundleOf("route" to route)
                    navController.navigate(
                        R.id.action_fullInfoFragment_to_navigationFragment,
                        bundle
                    )
                }

                (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
                cardView.visibility = View.VISIBLE
            }
        }

    }

}