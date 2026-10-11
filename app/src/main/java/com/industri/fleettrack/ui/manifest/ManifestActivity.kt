package com.industri.fleettrack.ui.manifest

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.industri.fleettrack.data.local.AppDatabase
import com.industri.fleettrack.data.local.entity.DeliveryOrderEntity
import com.industri.fleettrack.data.remote.api.CourierApiService
import com.industri.fleettrack.data.repository.DeliveryRepository
import com.industri.fleettrack.databinding.ActivityManifestBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManifestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManifestBinding
    private lateinit var viewModel: ManifestViewModel
    private lateinit var adapter: DeliveryListAdapter
    private var allOrders: List<DeliveryOrderEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManifestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(this)
        val apiService = CourierApiService.create()
        val repository = DeliveryRepository(
            database.deliveryDao(),
            apiService
        )
        val factory = ManifestViewModel.Factory(repository)

        viewModel = ViewModelProvider(
            this,
            factory
        )[ManifestViewModel::class.java]

        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchBar()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = DeliveryListAdapter { order ->
            viewModel.markAsDelivered(order.orderId)

            Toast.makeText(
                this,
                "Paket ${order.trackingNumber} berhasil diserahkan!",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvManifest.layoutManager = LinearLayoutManager(this)
        binding.rvManifest.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun setupSearchBar() {
        binding.etSearchManifest.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterOrders(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }
        )
    }

    private fun filterOrders(query: String) {
        val filtered = if (query.isBlank()) {
            allOrders
        } else {
            allOrders.filter { order ->
                order.trackingNumber.contains(
                    query,
                    ignoreCase = true
                ) ||
                        order.recipientName.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        order.destinationAddress.contains(
                            query,
                            ignoreCase = true
                        )
            }
        }

        adapter.submitList(filtered.toMutableList())
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->

                binding.swipeRefresh.isRefreshing =
                    state is ManifestUiState.Loading

                when (state) {
                    is ManifestUiState.Loading -> {
                        binding.pbMainLoading.visibility = View.VISIBLE
                    }

                    is ManifestUiState.Success -> {
                        binding.pbMainLoading.visibility = View.GONE

                        allOrders = state.orders

                        val currentSearch =
                            binding.etSearchManifest.text.toString()

                        filterOrders(currentSearch)
                    }

                    is ManifestUiState.Error -> {
                        binding.pbMainLoading.visibility = View.GONE

                        Toast.makeText(
                            this@ManifestActivity,
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
