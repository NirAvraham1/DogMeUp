package com.example.dogmeup

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class FindSitterActivity : AppCompatActivity() {

    private lateinit var layoutSitterList: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: FirebaseFirestore
    private lateinit var tvEmptyMessage: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var spinnerCity: Spinner
    private lateinit var btnApplyCity: Button
    private lateinit var etMaxPrice: EditText

    private var userCity: String? = null
    private var locationAlreadyUsed = false
    private var selectedDate: String? = null
    private var maxPrice: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_sitter)

        layoutSitterList = findViewById(R.id.lvSitters)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        spinnerCity = findViewById(R.id.spinnerCity)
        btnApplyCity = findViewById(R.id.btnApplyCity)
        etMaxPrice = findViewById(R.id.etMaxPrice)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = FirebaseFirestore.getInstance()

        val citiesList = listOf("Select city...") + loadCitiesFromJson()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, citiesList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = spinnerAdapter

        spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) return
                userCity = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnApplyCity.setOnClickListener {
            val priceText = etMaxPrice.text.toString().trim()
            maxPrice = priceText.toIntOrNull()
            loadSitters()
        }

        btnSelectDate.setOnClickListener { showDatePicker() }
        getCurrentLocation()

    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                if (!locationAlreadyUsed) {
                    locationAlreadyUsed = true
                    val location = result.lastLocation
                    if (location != null) {
                        getCityFromLocation(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(applicationContext, "Location unavailable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun getCityFromLocation(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(this, Locale.ENGLISH)
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                userCity = addresses[0].locality
                Toast.makeText(this, "You are in $userCity", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Could not detect your city", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Geocoder failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, y, m, d ->
            val date = String.format("%04d-%02d-%02d", y, m + 1, d)
            selectedDate = date
            btnSelectDate.text = "Selected: $date"
        }, year, month, day)

        dialog.show()
    }

    private fun loadSitters() {
        val city = userCity
        val date = selectedDate

        layoutSitterList.removeAllViews()
        tvEmptyMessage.visibility = View.GONE

        if (city.isNullOrBlank()) {
            tvEmptyMessage.text = "Please select a city."
            tvEmptyMessage.visibility = View.VISIBLE
            return
        }
        if (date.isNullOrBlank()) {
            tvEmptyMessage.text = "Please select a date."
            tvEmptyMessage.visibility = View.VISIBLE
            return
        }

        tvEmptyMessage.text = "No sitters available in your area."
        db.collection("users")
            .whereEqualTo("isSitter", true)
            .get()
            .addOnSuccessListener { sitterDocs ->
                if (sitterDocs.isEmpty) {
                    tvEmptyMessage.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                var totalSitters = sitterDocs.size()
                var sittersWithAvailability = 0

                for (sitterDoc in sitterDocs) {
                    val sitterId = sitterDoc.id
                    val fullName = sitterDoc.getString("fullName") ?: "Dog Sitter"
                    val bio = sitterDoc.getString("bio") ?: ""
                    val rate = sitterDoc.getLong("rate")?.toInt() ?: 0
                    val photoUrl = sitterDoc.getString("profilePhotoUrl") ?: ""

                    // סינון לפי מחיר מקסימלי
                    if (maxPrice != null && rate > maxPrice!!) {
                        totalSitters--
                        if (totalSitters == 0 && sittersWithAvailability == 0) {
                            tvEmptyMessage.visibility = View.VISIBLE
                        }
                        continue
                    }

                    db.collection("users")
                        .document(sitterId)
                        .collection("availability")
                        .whereEqualTo("date", date)
                        .whereArrayContains("cities", city)
                        .get()
                        .addOnSuccessListener { availabilityDocs ->
                            for (doc in availabilityDocs) {
                                val startTime = doc.getString("startTime") ?: ""
                                val endTime = doc.getString("endTime") ?: ""

                                // סינון זמני עבר
                                if (!isSlotInFuture(date, startTime)) {
                                    continue
                                }

                                val availabilityText = "$date $startTime–$endTime"

                                val view = layoutInflater.inflate(R.layout.item_sitter_card, null)
                                val tvName = view.findViewById<TextView>(R.id.tvSitterName)
                                val tvDetails = view.findViewById<TextView>(R.id.tvSitterDetails)
                                val ivPhoto = view.findViewById<ImageView>(R.id.ivSitterPhoto)

                                tvName.text = fullName
                                tvDetails.text = "Available: $availabilityText\nRate: ₪$rate/hour"
                                Glide.with(this).load(photoUrl).into(ivPhoto)

                                view.setOnClickListener {
                                    val intent = Intent(this, SitterDetailsActivity::class.java).apply {
                                        putExtra("fullName", fullName)
                                        putExtra("bio", bio)
                                        putExtra("rate", rate)
                                        putExtra("photoUrl", photoUrl)
                                        putExtra("availability", availabilityText)
                                        putExtra("sitterId", sitterId)
                                        putExtra("availabilityId", doc.id)
                                    }
                                    startActivity(intent)
                                }

                                layoutSitterList.addView(view)
                                sittersWithAvailability++
                            }

                            totalSitters--
                            if (totalSitters == 0 && sittersWithAvailability == 0) {
                                tvEmptyMessage.visibility = View.VISIBLE
                            }
                        }
                        .addOnFailureListener {
                            totalSitters--
                            if (totalSitters == 0 && sittersWithAvailability == 0) {
                                tvEmptyMessage.visibility = View.VISIBLE
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading sitters", Toast.LENGTH_SHORT).show()
                tvEmptyMessage.visibility = View.VISIBLE
            }
    }

    private fun isSlotInFuture(dateStr: String, startTimeStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val slotStart = sdf.parse("$dateStr $startTimeStr")
            slotStart != null && slotStart.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun loadCitiesFromJson(): List<String> {
        return try {
            val inputStream = assets.open("cities.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load cities", Toast.LENGTH_SHORT).show()
            emptyList()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        }
    }
}
