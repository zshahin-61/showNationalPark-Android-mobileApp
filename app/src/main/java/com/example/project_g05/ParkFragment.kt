package com.example.project_g05

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.project_g05.databinding.FragmentParkBinding
import com.example.project_g05.models.NationalPark
import com.example.project_g05.models.State
import com.example.project_g05.networking.ApiService
import com.example.project_g05.networking.RetrofitInstance
import kotlinx.coroutines.launch
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class ParkFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentParkBinding? = null
    private lateinit var binding: FragmentParkBinding
    private lateinit var spinner: Spinner
    private lateinit var button: Button
    private lateinit var apiKey: String
    private lateinit var apiService: ApiService
    private lateinit var mMap: GoogleMap

    private lateinit var parkList: List<NationalPark>
    private lateinit var stateList: List<State>
    private var selectedState: State? = null

    private val TAG = "Map_Park"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentParkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toast =
            Toast.makeText(requireActivity().applicationContext, "Screen Map", Toast.LENGTH_LONG)
        toast.show()
        Log.d(TAG, "We are in Map Screen")

        spinner = view.findViewById(R.id.spinner)
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


        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        if (mapFragment != null) {
            mapFragment.getMapAsync { googleMap ->
                mMap = googleMap
                googleMap.setPadding(0, 100, 0, 0)
                googleMap.uiSettings.isZoomControlsEnabled = true

            }
        } else {
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
               apiKey ="ooNeXJZPx1Q5JhfDWIxiRp5eBtYdlt27EPynnd8b"
                apiService = RetrofitInstance.retrofitService

                val response = apiService.getUsaNationalParksbyState(state.abbreviation)
              //  Toast.makeText(requireContext(), "find the ${response} is loading", Toast.LENGTH_SHORT).show()

                if (response.isSuccessful) {
                       Toast.makeText(requireContext(), "find the ${state.abbreviation} is sussfeul", Toast.LENGTH_SHORT).show()
                    Log.d(TAG,"${response}")

                    val parks = response.body()?.data
                    Log.d(TAG,"${parks}")
                    if (parks != null) {
                        parkList = parks

                        // Add markers on map for each national park
                        for (park in parkList) {
                            val latLng = LatLng(park.latitude, park.longitude)
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(park.fullName)
                            )
                        }

                        // Set map camera position to fit all markers
                        val builder = LatLngBounds.Builder()
                        for (park in parkList) {
                            val latLng = LatLng(park.latitude, park.longitude)
                            builder.include(latLng)
                        }
                        val bounds = builder.build()

                        val padding =16
                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                        mMap.animateCamera(cameraUpdate)
                    } else {
                        Toast.makeText(requireContext(), "No parks found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to get parks data", Toast.LENGTH_SHORT)
                        .show()
                    Log.e(TAG, "Failed to get parks data: ${response.code()}")
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error: ${e.message}", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

