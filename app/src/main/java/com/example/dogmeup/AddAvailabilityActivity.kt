package com.example.dogmeup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AddAvailabilityActivity : AppCompatActivity() {

    private lateinit var btnPickDate: Button
    private lateinit var btnPickStartTime: Button
    private lateinit var btnPickEndTime: Button
    private lateinit var btnSaveAvailability: Button
    private lateinit var btnPickCities: Button
    private lateinit var tvSelectedCities: TextView
    private lateinit var btnBack: Button

    private var selectedDate: String? = null
    private var startTime: String? = null
    private var endTime: String? = null
    private var selectedCities = mutableListOf<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_availability)

        btnPickDate = findViewById(R.id.btnPickDate)
        btnPickStartTime = findViewById(R.id.btnPickStartTime)
        btnPickEndTime = findViewById(R.id.btnPickEndTime)
        btnSaveAvailability = findViewById(R.id.btnSaveAvailability)
        btnPickCities = findViewById(R.id.btnPickCities)
        tvSelectedCities = findViewById(R.id.tvSelectedCities)
        btnBack = findViewById(R.id.btnBack)

        val citiesList = loadCitiesFromJson()

        btnPickCities.setOnClickListener {
            val selectedItems = BooleanArray(citiesList.size) { false }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select up to 3 cities")

            builder.setMultiChoiceItems(citiesList.toTypedArray(), selectedItems) { dialog, which, isChecked ->
                val city = citiesList[which]
                if (isChecked) {
                    if (selectedCities.size >= 3) {
                        Toast.makeText(this, "You can select up to 3 cities only", Toast.LENGTH_SHORT).show()
                        (dialog as AlertDialog).listView.setItemChecked(which, false)
                    } else {
                        selectedCities.add(city)
                    }
                } else {
                    selectedCities.remove(city)
                }
            }

            builder.setPositiveButton("OK") { _, _ ->
                tvSelectedCities.text = selectedCities.joinToString(", ")
            }

            builder.setNegativeButton("Cancel", null)

            builder.show()
        }

        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = dateFormat.format(calendar.time)
                btnPickDate.text = "Date: $selectedDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                startTime = timeFormat.format(calendar.time)
                btnPickStartTime.text = "Start: $startTime"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        btnPickEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                endTime = timeFormat.format(calendar.time)
                btnPickEndTime.text = "End: $endTime"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        btnSaveAvailability.setOnClickListener {
            validateAndSaveAvailability()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun validateAndSaveAvailability() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null || selectedDate == null || startTime == null || endTime == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCities.isEmpty()) {
            Toast.makeText(this, "Please select at least one city", Toast.LENGTH_SHORT).show()
            return
        }

        val startDateTimeStr = "$selectedDate $startTime"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val startDateTime = LocalDateTime.parse(startDateTimeStr, formatter)

        if (startDateTime.isBefore(LocalDateTime.now())) {
            Toast.makeText(this, "Cannot set availability in the past", Toast.LENGTH_SHORT).show()
            return
        }

        checkOverlapAndSave(userId, selectedDate!!, startTime!!, endTime!!)
    }

    private fun checkOverlapAndSave(userId: String, date: String, newStart: String, newEnd: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("availability")
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val existingStart = doc.getString("startTime") ?: continue
                    val existingEnd = doc.getString("endTime") ?: continue

                    if (isTimeOverlap(newStart, newEnd, existingStart, existingEnd)) {
                        Toast.makeText(this, "Time slot overlaps with an existing availability", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }
                }

                // No overlap found – save to Firestore
                val availability = hashMapOf(
                    "date" to date,
                    "startTime" to newStart,
                    "endTime" to newEnd,
                    "cities" to selectedCities
                )

                db.collection("users")
                    .document(userId)
                    .collection("availability")
                    .add(availability)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Availability saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking availability: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun isTimeOverlap(newStart: String, newEnd: String, existingStart: String, existingEnd: String): Boolean {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val newStartTime = format.parse(newStart)
        val newEndTime = format.parse(newEnd)
        val existingStartTime = format.parse(existingStart)
        val existingEndTime = format.parse(existingEnd)

        return newStartTime.before(existingEndTime) && newEndTime.after(existingStartTime)
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
}
