package com.example.myapplication
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.R.*
import com.example.myapplication.R.id.*
import com.example.myapplication.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import java.sql.Time
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import kotlin.collections.HashMap


class MapsActivity :AppCompatActivity(),
    OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var textCurrentLocation: TextView
    private lateinit var textTravelDistance : TextView
    private var isJourneyStarted = false
    private var totalDistance = 0.0
    private var previousUpdateTime = 0L
    private var previousLocation: Location? = null
    private var PERMISSIONCODE = 111
    private var destinationMarker: Marker? = null
    private var originMarker: Marker? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationsRequest: LocationRequest
    private val journeyLocations: MutableList<Location> = mutableListOf()
    private var currentLocationMarker: Marker? = null
    private var endLocationLat  :Double = 0.0
    private var endLocationLng  :Double = 0.0
    private val APIKEY = "AIzaSyBtydB5hJ7sw4uFbMQOINK9N-5SCObh524"
    private lateinit var auth: FirebaseAuth
    private var hasRestarted = false
    private lateinit var textCurrentSpeed : TextView
    private lateinit var textCreditLeft : TextView
    private lateinit var textOrigin : TextView
    private lateinit var notifyExitBtn : FloatingActionButton
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var journeyStartedTime : HashMap<String,Any> ?= null
    private var journeyEndedTime : HashMap<String,Any> ?= null
    private var usersData : DataSnapshot ?= null
    private var currentWalletBalance : Int?=null
    private var finalCost : Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) {
        }

        auth = Firebase.auth
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        textCurrentLocation =  findViewById(textViewUserCurrentLocation)
        textTravelDistance = findViewById(textViewUserDistantTravel)
        textCurrentSpeed = findViewById(textViewUserCurrentSpeed)
        textOrigin = findViewById(textViewUserOriginLocation)
        textCreditLeft = findViewById(textViewUserCreditLeft)
        notifyExitBtn = findViewById(fabUserNotifyExit)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        Places.initialize(applicationContext, APIKEY)

        notifyExitBtn.setOnClickListener {
            stopJourney()
        }

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val sriLankaLatLng = LatLng(7.8731, 80.7718)
        val zoomLevel = 8.0f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLankaLatLng, zoomLevel))
        getUserData()
    }


    private fun startJourney(){
        if (checkPermission()) {
            if (isLocationEnabled()) {
                isJourneyStarted = true
                totalDistance = 0.0
                previousUpdateTime = 0L
                previousLocation = null
                journeyLocations.clear()
                getNewLocation()
                val currentTime = Calendar.getInstance()
                val hours = currentTime.get(Calendar.HOUR_OF_DAY) // 24-hour format
                val minutes = currentTime.get(Calendar.MINUTE)
                journeyStartedTime  = hashMapOf(
                    "hours" to hours,
                    "minutes" to minutes
                )
            } else {
                Toast.makeText(this, "Please turn on the Location Service", Toast.LENGTH_LONG).show()
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNewLocation() {
        Toast.makeText(this, "Waiting for the location", Toast.LENGTH_LONG).show()
        locationsRequest = LocationRequest()
        locationsRequest.priority =
            LocationRequest.PRIORITY_HIGH_ACCURACY
        locationsRequest.interval = 3000
        locationsRequest.fastestInterval = 2000
        locationsRequest.numUpdates = 200
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

        fusedLocationProviderClient.requestLocationUpdates(
            locationsRequest, locationCallback, Looper.myLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        @SuppressLint("SuspiciousIndentation")
        override fun onLocationResult(p0: LocationResult) {
            val currentLocation = p0.lastLocation
            if (currentLocation != null) {
                val cityName: String = getCityName(currentLocation.latitude, currentLocation.longitude)

                textCurrentLocation.text = "Area name: $cityName"

                    if (isJourneyStarted && journeyLocations.isNotEmpty()) {

                        val previousLocation = journeyLocations.last()
                        val distance = previousLocation.distanceTo(currentLocation)
                        val currentTime = Calendar.getInstance()

                        currentLocationMarker?.remove()

                        currentLocationMarker = mMap.addMarker(MarkerOptions()
                            .position(LatLng(currentLocation.latitude,currentLocation.longitude))
                            .title("Current Location"))

                        if (distance > 1000) {
                            totalDistance += distance
                            textTravelDistance.text = totalDistance.toString()
                        }

                        if (startTime == 0L) {
                            startTime = currentTime.timeInMillis
                        } else {
                            endTime = currentTime.timeInMillis
                            val elapsedTimeInSeconds =
                                (endTime - startTime) / 1000.0 // Convert to seconds
                            val speedMps =
                                distance / elapsedTimeInSeconds // Speed in meters per second (m/s)
                            val speedKmph =
                                (speedMps * 3.6).toFloat()  // Convert to kilometers per hour (km/h)

                            val decimalFormat = DecimalFormat("#.#")

                            val lastResult =  decimalFormat.format(speedKmph).toFloat()


                            if (speedKmph > 0){
                                textCurrentSpeed.text = "$lastResult km/h"
                            }

                            startTime = endTime
                        }
                    }else{
                        mMap.addMarker(
                        MarkerOptions().position(LatLng(currentLocation.latitude,currentLocation.longitude))
                            .title("Origin Point")
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                journeyLocations.add(currentLocation)
            }
        }
    }

    private fun getCityName(lat:Double,long:Double):String{
        var cityName ="Not Found"
        val geoCoder = Geocoder(this, Locale.getDefault())
        val address = geoCoder.getFromLocation(lat,long,1)
        if (address != null) {
            cityName = address[0]?.locality.toString()
        }
        return  cityName
    }

    private fun stopJourney() {
        isJourneyStarted = false
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        val lastLocation = journeyLocations.last()
        val currentTime = Calendar.getInstance()
        val hours = currentTime.get(Calendar.HOUR_OF_DAY) // 24-hour format
        val minutes = currentTime.get(Calendar.MINUTE)

        // Calculate total distance traveled
        if (journeyLocations.size >= 2) {
            for (i in 1 until journeyLocations.size) {
                val previousLocation = journeyLocations[i - 1]
                val currentLocation = journeyLocations[i]
                val distance = previousLocation.distanceTo(currentLocation) // in meters
                totalDistance += distance
            }
        }

        journeyEndedTime  = hashMapOf(
            "hours" to hours,
            "minutes" to minutes
        )

        textCurrentLocation.text = "Distance Traveled: ${String.format("%.2f", totalDistance/1000)} km"

        mMap.addMarker(
            MarkerOptions().position(LatLng(lastLocation.latitude,lastLocation.longitude))
                .title("Origin Point")
                .icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        calculateCost()
      //journeyStopDBHandler()
    }

    private fun calculateCost() {
        if (totalDistance != null && totalDistance > 0) {

            val toatalDistance = 120
            val disInKm = totalDistance / 1000
            finalCost = 20

            finalCost += if (disInKm < 5) {
                40
            } else if (disInKm < 10) {
                90
            } else if (disInKm < 20) {
                120
            } else if (disInKm < 50) {
                140
            } else if (disInKm < 100) {
                200
            } else {
                (Integer.parseInt(totalDistance.toString())/ 1000) * 3
            }
            textOrigin.text = totalDistance.toString()
        }
    }

    private fun journeyStopDBHandler() {
        val userID = auth.currentUser!!.uid
        val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(userID)

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val updates = hashMapOf(
                        "busID" to "",
                        "status" to "idle",
                        "walletBalance" to ""
                    )
                    userReference.updateChildren(updates as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@MapsActivity,
                                "Bus Registered Successfully",
                                Toast.LENGTH_LONG
                            ).show()

                        }.addOnFailureListener {
                            Toast.makeText(
                                this@MapsActivity,
                                "Error Occurred ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                } else {
                    Toast.makeText(
                        this@MapsActivity,
                        "User data not Found",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@MapsActivity,
                    "Error Occurred ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }
        })

    }

    private fun getUserData() {
        val userID = auth.currentUser!!.uid
        val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(userID)

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    startJourney()
                    usersData = dataSnapshot
                    currentWalletBalance = Integer.parseInt(dataSnapshot.child("walletBalance").value.toString())
                }else{
                    Toast.makeText(this@MapsActivity, "Error Occurred. Please try Again.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MapsActivity, PassengerHomeActivity::class.java)
                        startActivity(intent)
                        finish()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MapsActivity, "Error Occurred ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

















    private fun routeHandler() {

        val startLocation = journeyLocations.first()
        Log.d("Map", "START LOC LAT ${startLocation.latitude}----------------------------------------------------------")
        Log.d("Map", "START LOC LNG ${startLocation.latitude}----------------------------------------------------------")
        Log.d("Map", "END LOC LAT ${endLocationLat}----------------------------------------------------------")
        Log.d("Map", "EMD LOC LNG ${endLocationLng}----------------------------------------------------------")

        val context = GeoApiContext.Builder()
            .apiKey(APIKEY)
            .queryRateLimit(3)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val directionsResult: DirectionsResult = DirectionsApi.newRequest(context)
            .mode(TravelMode.DRIVING) // Choose travel mode
            .origin("${startLocation.latitude},${startLocation.longitude}")
            .destination("$endLocationLat,$endLocationLng")
            .await()

        val route = directionsResult.routes[0].overviewPolyline.decodePath()

        if (route != null){
            // Convert DirectionsLatLng to Google Maps LatLng
            val googleMapsLatLngList = route.map { directionsLatLng ->
                LatLng(directionsLatLng.lat, directionsLatLng.lng)
            }

            // Draw the route on the map
            val polylineOptions = PolylineOptions().addAll(googleMapsLatLngList)
            mMap.addPolyline(polylineOptions)
        }else{
            Log.d("debug", "---------------------------------List is Empty----------------------------")
        }

    }




































    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestPermissions() {

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), PERMISSIONCODE
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            (LocationManager.NETWORK_PROVIDER)
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONCODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (!hasRestarted) {
              //  restartActivity();
                hasRestarted = true
            }

            Log.d("debug", "----------------------------------location Permissions granted----------------------------------")
        }
    }

}

//
//        endLocationFragment =
//            supportFragmentManager.findFragmentById(id.endLocationFragment) as AutocompleteSupportFragment
//
//        endLocationFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
//            .setHint("Select the Journey Destination")
//            .setCountry("LK")
//
//        endLocationFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//
//            override fun onPlaceSelected(place: Place) {
//                endLocationLat = place.latLng!!.latitude
//                endLocationLng = place.latLng!!.longitude
//
//                addMarker(LatLng(endLocationLat,endLocationLng), "Destination")
//
//                mMap.addMarker(
//                    MarkerOptions().position(LatLng(endLocationLat,endLocationLng))
//                        .title("End Location")
//                )
//
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(place.latLng!!.latitude,place.latLng!!.longitude)))
//              //  routeHandler()
//            }
//
//            override fun onError(status: Status) {
//                Log.i(TAG, "An error occurred: $status---------------------------------------------------------------------------------------------------------------")
//            }
//        })
//
//
//
//        conBtn.setOnClickListener {
//            if (!isJourneyStarted) {
//                startJourney()
//
//            } else {
//                stopJourney()
//            }
//        }
//
//        logOutBtn.setOnClickListener {
//            Firebase.auth.signOut()
//            Toast.makeText(this, "Logging Out", Toast.LENGTH_SHORT).show()
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            this.finish()
//        }



//private fun showCurrentLocation() {
//    if (!isstartmarkerset) {
//        if (checkPermission()) {
//            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
//                if (location != null) {
//                    val currentLatLng = LatLng(location.latitude, location.longitude)
//                    if (currentLocationMarker == null) {
//                        addMarker(currentLatLng, "Current Location")
//                        journeyLocations.add(location)
//                        val cityName: String = getCityName(location.latitude,location.longitude)
//                        textOrigin.text = cityName
//                    } else {
//                        currentLocationMarker?.position = currentLatLng
//                        addMarker(currentLatLng, "Current Location")
//                    }
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 8.0f))
//                    isstartmarkerset = true
//                } else {
//                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
//                    requestPermissions()
//                }
//            }
//        } else {
//            requestPermissions()
//        }
//    }
//}

//private fun addMarker(latLng: LatLng, type: String) {
//    // Remove existing markers if they exist
//    if (type == "Destination") {
//        destinationMarker?.remove()
//        destinationMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Destination"))
//    } else if (type == "Origin") {
//        originMarker?.remove()
//        originMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Origin"))
//    }
//}
