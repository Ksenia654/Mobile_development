package com.example.calculator

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.MainActivity.Calculation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val recyclerView = findViewById<RecyclerView>(R.id.historyList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadHistory { items ->
            recyclerView.adapter = HistoryAdapter(items).apply {
                notifyDataSetChanged()
            }
        }
    }

    private fun loadHistory(callback: (List<MainActivity.Calculation>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("calculations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    val items = result.map {
                        it.toObject(Calculation::class.java)
                    }
                    callback(items)
                }
                .addOnFailureListener { e ->
                    Log.e("HISTORY", "Ошибка загрузки: ${e.message}")
                    callback(emptyList())
                }
        } ?: callback(emptyList())

    }
}