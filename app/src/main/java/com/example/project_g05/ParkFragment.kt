package com.example.project_g05

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.project_g05.databinding.FragmentParkBinding
import com.example.project_g05.models.NationalPark
import com.example.project_g05.models.State
import com.example.project_g05.networking.ApiService
import com.example.project_g05.networking.RetrofitInstance
import com.google.android.gms.maps.*
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
 class ParkFragment : Fragment(),  OnMapReadyCallback {

    private var _binding: FragmentParkBinding? = null
    private lateinit var binding: FragmentParkBinding
  //  private lateinit var spinner: Spinner
  //  private lateinit var button: Button
 //   private lateinit var apiKey: String
  //  private lateinit var apiService: ApiService
    private lateinit var mMap: GoogleMap

    private lateinit var parkList: List<NationalPark>
    private lateinit var stateList: List<State>
    private var selectedState: State? = null
    private lateinit var mapView: MapView

    private val TAG = "Map_Park"

     override fun onCreateView(
         inflater: LayoutInflater,
         container: ViewGroup?,
         savedInstanceState: Bundle?
     ): View? {
         // Inflate the root view
         val rootView = inflater.inflate(R.layout.fragment_park, container, false)
         // Inflate the binding
         binding = FragmentParkBinding.inflate(inflater, container, false)

         val mapView = rootView.findViewById<MapView>(R.id.mapFragment)
         MapsInitializer.initialize(requireActivity())
         mapView.onCreate(savedInstanceState)
         mapView.getMapAsync { googleMap ->
         }

         return rootView
     }



     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val toast =
            Toast.makeText(requireActivity().applicationContext, "Screen Map", Toast.LENGTH_LONG)
        toast.show()
        Log.d(TAG, "We are in Map Screen")

        val spinner = view.findViewById<Spinner>(R.id.spinner)

        val states = State.values()
        val stateNames = states.map { it.fullName }
        // Initialize the Spinner adapter
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, stateNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set item selection listener for spinner
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedState = states[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Set up the map

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        if (mapFragment != null) {
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }else{
            Toast.makeText(requireContext(), "Map is unavailable", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Map is null")

        }

        // Find Parks button click listener
        binding.findParksButton.setOnClickListener {
            if (selectedState != null) {
                findParks(selectedState!!)
            } else {
                Toast.makeText(requireContext(), "Please choose a state", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Toast.makeText(requireContext(), "googleMap loooding", Toast.LENGTH_SHORT).show()

        // Set up marker click listener
        mMap.setOnMarkerClickListener { marker ->
            val park = parkList.find { it.fullName == marker.title }
            if (park != null) {
                // Navigate to View Park Details screen with the selected park data
                val action =
                    ParkFragmentDirections.actionParkFragmentToParkDetailsFragment(park)
                findNavController().navigate(action)

                true
            } else {
                false
            }
        }

        // Set up map click listener
        mMap.setOnMapClickListener {
            // Clear all markers
            mMap.clear()
        }
    }

    private fun findParks(state: State) {
        lifecycleScope.launch {
            try {
                val apiKey = "ooNeXJZPx1Q5JhfDWIxiRp5eBtYdlt27EPynnd8b"
                // Fetch parks data from API
                val response = RetrofitInstance.retrofitService.getUsaNationalParksbyState(state.abbreviation)
                Toast.makeText(requireContext(), "Find ${state.abbreviation }parks is working", Toast.LENGTH_SHORT).show()
                if (response.isSuccessful) {
                    val parks = response.body()?.nationalParks
                    if (parks != null) {
                        parkList = parks
                        for (park in parks) {
                            val latLng = LatLng(park.latitude, park.longitude)
                            mMap.addMarker(
                                MarkerOptions().position(latLng).title(park.fullName)
                            )
                        }
                        // Zoom map to fit all markers
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(getLatLngBounds(parks), 100)
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No parks found for selected state",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch parks data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching parks data: ${e.localizedMessage}")
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch parks data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun getLatLngBounds(parks: List<NationalPark>): LatLngBounds {
        val builder = LatLngBounds.Builder()
        for (park in parks) {
            val latLng = LatLng(park.latitude, park.longitude)
            builder.include(latLng)
        }
        return builder.build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
