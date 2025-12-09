package com.example.petlink.ui.ads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petlink.ui.myanimal.EditAnimalActivity
import com.example.petlink.ui.myanimal.MyAnimalDetailActivity
import com.example.petlink.R
import com.example.petlink.adapter.MyAdsAdapter
import com.example.petlink.data.model.AnimalReq
import com.example.petlink.util.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyAdsFragment : Fragment() {

    private lateinit var tabs: TabLayout
    private lateinit var recyclerActive: RecyclerView
    private lateinit var recyclerArchive: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var fab: FloatingActionButton

    private lateinit var adapterActive: MyAdsAdapter
    private lateinit var adapterArchive: MyAdsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_ads, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabs = view.findViewById(R.id.tabLayout)
        recyclerActive = view.findViewById(R.id.recyclerActive)
        recyclerArchive = view.findViewById(R.id.recyclerArchive)
        progress = view.findViewById(R.id.progress)
        emptyView = view.findViewById(R.id.emptyView)
        fab = view.findViewById(R.id.fabAdd)

        recyclerActive.layoutManager = LinearLayoutManager(requireContext())
        recyclerArchive.layoutManager = LinearLayoutManager(requireContext())

        adapterActive = MyAdsAdapter { ad ->
            val i = android.content.Intent(requireContext(), MyAnimalDetailActivity::class.java)
            i.putExtra("animal_id", ad.id.toInt())
            startActivity(i)
        }
        adapterArchive = MyAdsAdapter { ad ->
            val i = android.content.Intent(requireContext(), MyAnimalDetailActivity::class.java)
            i.putExtra("animal_id", ad.id.toInt())
            startActivity(i)
        }
        recyclerActive.adapter = adapterActive
        recyclerArchive.adapter = adapterArchive

        tabs.addTab(tabs.newTab().setText("Активные"))
        tabs.addTab(tabs.newTab().setText("Архив"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { updateVisibleList() }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        fab.setOnClickListener {
            val i = android.content.Intent(requireContext(), EditAnimalActivity::class.java)
            startActivity(i)
        }

        loadData()
        updateVisibleList()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun updateVisibleList() {
        val active = tabs.selectedTabPosition == 0
        recyclerActive.visibility = if (active) View.VISIBLE else View.GONE
        recyclerArchive.visibility = if (!active) View.VISIBLE else View.GONE
        fab.visibility = if (active) View.VISIBLE else View.GONE
    }

    private fun loadData() {
        val sp = requireContext().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "Требуется вход"
            return
        }
        progress.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerActive.visibility = View.GONE
        recyclerArchive.visibility = View.GONE

        // Активные: mine=true, status_name="Пристраивается" и/или is_available=true
        RetrofitClient.apiService.getAnimals(
            authHeader = "Token $token",
            mine = true,
            statusName = "Пристраивается",
            isAvailable = true
        ).enqueue(object: Callback<List<AnimalReq>> {
            override fun onResponse(call: Call<List<AnimalReq>>, response: Response<List<AnimalReq>>) {
                if (response.isSuccessful) {
                    adapterActive.submitList(response.body().orEmpty())
                }
                progress.visibility = View.GONE
                updateEmptyState()
                updateVisibleList()
            }
            override fun onFailure(call: Call<List<AnimalReq>>, t: Throwable) {
                progress.visibility = View.GONE
                updateEmptyState("Сеть недоступна: ${t.message}")
                updateVisibleList()
            }
        })

        // Архив: mine=true, status_name="Уже пристроен" или is_available=false
        RetrofitClient.apiService.getAnimals(
            authHeader = "Token $token",
            mine = true,
            isAvailable = false
        ).enqueue(object: Callback<List<AnimalReq>> {
            override fun onResponse(call: Call<List<AnimalReq>>, response: Response<List<AnimalReq>>) {
                if (response.isSuccessful) {
                    adapterArchive.submitList(response.body().orEmpty())
                }
                progress.visibility = View.GONE
                updateEmptyState()
                updateVisibleList()
            }
            override fun onFailure(call: Call<List<AnimalReq>>, t: Throwable) {
                progress.visibility = View.GONE
                updateEmptyState("Сеть недоступна: ${t.message}")
                updateVisibleList()
            }
        })
    }

    private fun updateEmptyState(message: String? = null) {
        val show = adapterActive.itemCount == 0 && adapterArchive.itemCount == 0
        emptyView.visibility = if (show) View.VISIBLE else View.GONE
        if (!message.isNullOrEmpty()) emptyView.text = message
    }
}
