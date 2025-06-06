package com.example.entityadmin

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.example.entityadmin.network.Session
import com.example.entityadmin.viewmodel.SessionViewModel

@AndroidEntryPoint
class SessionListActivity : AppCompatActivity() {

    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_list)

        val recycler = findViewById<RecyclerView>(R.id.recyclerSessions)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = SessionAdapter()
        recycler.adapter = adapter

        lifecycleScope.launch {
            viewModel.loadSessions()
            adapter.sessions = viewModel.sessions
            adapter.notifyDataSetChanged()
        }
    }
}
