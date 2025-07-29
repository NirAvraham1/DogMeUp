package com.example.dogmeup

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var btnBack: Button
    private val ordersList = mutableListOf<Order>()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var isSitter = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        // מציאת רכיבים
        recyclerView = findViewById(R.id.recyclerViewOrders)
        spinner = findViewById(R.id.spinnerFilter)
        btnBack = findViewById(R.id.btnBack)

        // חזרה אחורה
        btnBack.setOnClickListener {
            finish()
        }

        // הגדרת RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        ordersAdapter = OrdersAdapter(emptyList())
        recyclerView.adapter = ordersAdapter

        // הגדרת Spinner
        val filterOptions = listOf("All", "Upcoming", "Completed")
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            filterOptions
        )

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedFilter = filterOptions[position]
                filterAndShowOrders(selectedFilter)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadOrders()
    }

    private fun loadOrders() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            isSitter = doc.getBoolean("isSitter") ?: false
            val queryField = if (isSitter) "sitterId" else "clientId"

            db.collection("bookings")
                .whereEqualTo(queryField, userId)
                .get()
                .addOnSuccessListener { result ->
                    ordersList.clear()
                    val now = Date()
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                    for (doc in result) {
                        var order = doc.toObject(Order::class.java)

                        // בדיקה אם תוקפו עבר ועדכון סטטוס
                        val fullTimeStr = "${order.date} ${order.endTime}"
                        val orderEndTime = try {
                            sdf.parse(fullTimeStr)
                        } catch (e: Exception) {
                            null
                        }

                        if (order.status == "upcoming" && orderEndTime != null && orderEndTime.before(now)) {
                            db.collection("bookings").document(doc.id)
                                .update("status", "completed")
                                .addOnSuccessListener {
                                    Log.d("MyOrders", "Order ${doc.id} marked as completed")
                                }

                            order.status = "completed"
                        }

                        ordersList.add(order)
                    }

                    val selected = spinner.selectedItem?.toString() ?: "All"
                    filterAndShowOrders(selected)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show()
                    Log.e("MyOrders", "Error loading orders", it)
                }
        }
    }

    private fun filterAndShowOrders(filter: String) {
        val filtered = when (filter) {
            "Upcoming" -> ordersList.filter { it.status == "upcoming" }
            "Completed" -> ordersList.filter { it.status == "completed" }
            else -> ordersList
        }

        ordersAdapter = OrdersAdapter(filtered)
        recyclerView.adapter = ordersAdapter
    }
}
