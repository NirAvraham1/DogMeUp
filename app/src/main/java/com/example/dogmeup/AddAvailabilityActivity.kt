package com.example.dogmeup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddAvailabilityActivity : AppCompatActivity() {

    private lateinit var btnPickDate: Button
    private lateinit var btnPickStartTime: Button
    private lateinit var btnPickEndTime: Button
    private lateinit var btnSaveAvailability: Button
    private lateinit var etCities: MultiAutoCompleteTextView
    private lateinit var btnBack: Button

    private var selectedDate: String? = null
    private var startTime: String? = null
    private var endTime: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_availability)


        btnPickDate = findViewById(R.id.btnPickDate)
        btnPickStartTime = findViewById(R.id.btnPickStartTime)
        btnPickEndTime = findViewById(R.id.btnPickEndTime)
        btnSaveAvailability = findViewById(R.id.btnSaveAvailability)
        etCities = findViewById(R.id.etCities)
        btnBack = findViewById(R.id.btnBack)

        val citiesList = listOf(
            "Tel Aviv", "Ramat Gan", "Giv'atayim", "Holon", "Bat Yam",
            "Jerusalem", "Haifa", "Netanya", "Herzliya", "Petah Tikva",
            "Kfar Saba", "Ra'anana", "Eilat", "Be'er Sheva", "Ashdod",
            "Ashkelon", "Rishon LeZion", "Rehovot", "Modiin", "Yavne",
            "Lod", "Ramla", "Nazareth", "Tiberias", "Acre",
            "Kiryat Ono", "Kiryat Gat", "Kiryat Yam", "Kiryat Motzkin", "Nahariya",
            "Zikhron Ya'akov", "Or Yehuda", "Yehud", "Beit Shemesh", "Ma'ale Adumim",
            "Arad", "Dimona", "Safed", "Hadera", "Carmiel",
            "Migdal HaEmek", "Nesher", "Tamra", "Sakhnin", "Umm al-Fahm"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, citiesList)
        etCities.setAdapter(adapter)
        etCities.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        // בוחר תאריך
        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = dateFormat.format(calendar.time)
                btnPickDate.text = "Date: $selectedDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // בוחר שעת התחלה
        btnPickStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                startTime = timeFormat.format(calendar.time)
                btnPickStartTime.text = "Start: $startTime"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // בוחר שעת סיום
        btnPickEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                endTime = timeFormat.format(calendar.time)
                btnPickEndTime.text = "End: $endTime"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // שומר את הנתונים
        btnSaveAvailability.setOnClickListener {
            saveAvailabilityToFirestore()
        }

        // סגירת המסך וחזרה אחורה
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun saveAvailabilityToFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null || selectedDate == null || startTime == null || endTime == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // שליפת ערים מהשדה
        val rawCities = etCities.text.toString()
        val selectedCities = rawCities.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (selectedCities.size > 3) {
            Toast.makeText(this, "Please enter up to 3 cities only", Toast.LENGTH_SHORT).show()
            return
        }

        // בניית האובייקט לשמירה
        val availability = hashMapOf(
            "date" to selectedDate,
            "startTime" to startTime,
            "endTime" to endTime,
            "cities" to selectedCities
        )

        FirebaseFirestore.getInstance()
            .collection("users")
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

}
