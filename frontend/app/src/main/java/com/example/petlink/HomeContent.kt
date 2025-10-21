package com.example.petlink

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.petlink.data.model.AnimalPhotoReq
import com.example.petlink.data.model.AnimalReq
import com.example.petlink.data.model.StatusReq
import com.example.petlink.util.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeContent {
    private var filtersInitialized = false
    private var cachedSpecies: List<com.example.petlink.data.model.SpeciesReq>? = null
    private var cachedBreeds: List<com.example.petlink.data.model.BreedReq>? = null
    fun loadAndRender(
        grid: GridLayout,
        context: Context,
        speciesId: Int? = null,
        breedId: Int? = null,
        gender: String? = null,
        ageMin: Double? = null,
        ageMax: Double? = null,
        priceMin: Double? = null,
        priceMax: Double? = null,
        isHypoallergenic: Boolean? = null,
        childFriendly: Boolean? = null,
        spaceRequirements: String? = null
    ) {
        val filtersPanel = (grid.parent as? View)?.rootView?.findViewById<LinearLayout>(R.id.filters_panel)
        val btnFilters = (grid.parent as? View)?.rootView?.findViewById<Button>(R.id.btn_filters)
        val btnApply = (grid.parent as? View)?.rootView?.findViewById<Button>(R.id.btn_apply_filters)
        val btnClear = (grid.parent as? View)?.rootView?.findViewById<Button>(R.id.btn_clear_filters)
        val spinnerSpecies = (grid.parent as? View)?.rootView?.findViewById<Spinner>(R.id.spinner_species)
        val spinnerBreed = (grid.parent as? View)?.rootView?.findViewById<Spinner>(R.id.spinner_breed)
        val spinnerGender = (grid.parent as? View)?.rootView?.findViewById<Spinner>(R.id.spinner_gender)
        val spinnerAge = (grid.parent as? View)?.rootView?.findViewById<Spinner>(R.id.spinner_age)
        val cbSter = (grid.parent as? View)?.rootView?.findViewById<android.widget.CheckBox>(R.id.cb_sterilized)
        val cbVacc = (grid.parent as? View)?.rootView?.findViewById<android.widget.CheckBox>(R.id.cb_vaccinated)
        val cbHypo = (grid.parent as? View)?.rootView?.findViewById<android.widget.CheckBox>(R.id.cb_hypo)
        val cbKids = (grid.parent as? View)?.rootView?.findViewById<android.widget.CheckBox>(R.id.cb_child_friendly)
        val spinnerSpace = (grid.parent as? View)?.rootView?.findViewById<Spinner>(R.id.spinner_space)
        val editPriceMin = (grid.parent as? View)?.rootView?.findViewById<android.widget.EditText>(R.id.edit_price_min)
        val editPriceMax = (grid.parent as? View)?.rootView?.findViewById<android.widget.EditText>(R.id.edit_price_max)

        if (!filtersInitialized) {
            spinnerGender?.adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Любой", "Мужской", "Женский")
            )
            spinnerAge?.adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Любой", "До 1 года", "1-3 года", "3+ лет")
            )
        }

        if (!filtersInitialized) {
        RetrofitClient.apiService.getSpecies().enqueue(object: Callback<List<com.example.petlink.data.model.SpeciesReq>> {
            override fun onResponse(call: Call<List<com.example.petlink.data.model.SpeciesReq>>, response: Response<List<com.example.petlink.data.model.SpeciesReq>>) {
                cachedSpecies = response.body().orEmpty()
                spinnerSpecies?.tag = cachedSpecies
                val items = mutableListOf("Любой")
                items.addAll(cachedSpecies?.map { it.name } ?: emptyList())
                spinnerSpecies?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
            }
            override fun onFailure(call: Call<List<com.example.petlink.data.model.SpeciesReq>>, t: Throwable) { }
        })

        RetrofitClient.apiService.getBreeds().enqueue(object: Callback<List<com.example.petlink.data.model.BreedReq>> {
            override fun onResponse(call: Call<List<com.example.petlink.data.model.BreedReq>>, response: Response<List<com.example.petlink.data.model.BreedReq>>) {
                cachedBreeds = response.body().orEmpty()
                spinnerBreed?.tag = cachedBreeds
                val items = mutableListOf("Любая")
                items.addAll(cachedBreeds?.map { it.name } ?: emptyList())
                spinnerBreed?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
            }
            override fun onFailure(call: Call<List<com.example.petlink.data.model.BreedReq>>, t: Throwable) { }
        })
        }

        if (!filtersInitialized) {
            spinnerSpace?.adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Любые", "Низкие", "Средние", "Высокие")
            )

            btnFilters?.setOnClickListener {
                if (filtersPanel?.visibility == View.VISIBLE) filtersPanel.visibility = View.GONE else filtersPanel?.visibility = View.VISIBLE
            }
            btnClear?.setOnClickListener {
                fetchAndRender(grid, context, null, null, null, null, null, null, null, null, null, null, null, null)
                filtersPanel?.visibility = View.GONE
            }
            filtersInitialized = true
        }

        btnApply?.setOnClickListener {
            val speciesIdx = spinnerSpecies?.selectedItemPosition ?: 0
            val breedIdx = spinnerBreed?.selectedItemPosition ?: 0
            val genderVal = when (spinnerGender?.selectedItemPosition) { 1 -> "M"; 2 -> "F"; else -> null }
            val ageBounds = when (spinnerAge?.selectedItemPosition) { 1 -> 0.0 to 1.0; 2 -> 1.0 to 3.0; 3 -> 3.0 to 100.0; else -> null }
            val priceMinInput = editPriceMin?.text?.toString()?.trim()
            val priceMaxInput = editPriceMax?.text?.toString()?.trim()
            val priceMinValue = priceMinInput?.toDoubleOrNull()
            val priceMaxValue = priceMaxInput?.toDoubleOrNull()
            val priceBounds = when {
                priceMinValue != null && priceMaxValue != null -> priceMinValue to priceMaxValue
                priceMinValue != null && priceMaxValue == null -> priceMinValue to 999_999.0
                priceMinValue == null && priceMaxValue != null -> 0.0 to priceMaxValue
                else -> null
            }

            // Кэш + маппинг выбранного имени на id
            val cachedSpecies = spinnerSpecies?.tag as? List<com.example.petlink.data.model.SpeciesReq>
            val cachedBreeds = spinnerBreed?.tag as? List<com.example.petlink.data.model.BreedReq>
            val speciesIdParam = speciesIdx.takeIf { it > 0 }?.let { idx -> cachedSpecies?.getOrNull(idx - 1)?.id }
            val breedIdParam = breedIdx.takeIf { it > 0 }?.let { idx -> cachedBreeds?.getOrNull(idx - 1)?.id }

            val spaceValue = when (spinnerSpace?.selectedItemPosition) {
                1 -> "low"
                2 -> "medium"
                3 -> "high"
                else -> null
            }

            fetchAndRender(
                grid,
                context,
                speciesIdParam,
                breedIdParam,
                genderVal,
                ageBounds?.first,
                ageBounds?.second,
                priceBounds?.first,
                priceBounds?.second,
                if (cbHypo?.isChecked == true) true else null,
                if (cbKids?.isChecked == true) true else null,
                spaceValue,
                if (cbSter?.isChecked == true) true else null,
                if (cbVacc?.isChecked == true) true else null
            )
        }

        fetchAndRender(grid, context, speciesId, breedId, gender, ageMin, ageMax, priceMin, priceMax, isHypoallergenic, childFriendly, spaceRequirements, null, null)
    }

    private fun fetchAndRender(
        grid: GridLayout,
        context: Context,
        speciesId: Int?,
        breedId: Int?,
        gender: String?,
        ageMin: Double?,
        ageMax: Double?,
        priceMin: Double?,
        priceMax: Double?,
        isHypoallergenic: Boolean?,
        childFriendly: Boolean?,
        spaceRequirements: String?,
        isSterilized: Boolean?,
        hasVaccinations: Boolean?
    ) {
        var animalsList = listOf<AnimalReq>()
        var photosList = listOf<AnimalPhotoReq>()
        var statusesList = listOf<StatusReq>()

        var statusesLoaded = false
        var animalsLoaded = false
        var photosLoaded = false

        fun tryRender() {
            if (animalsLoaded && photosLoaded && statusesLoaded) {
                renderGridWithPhotos(grid, context, animalsList, photosList, statusesList)
            }
        }

        RetrofitClient.apiService.getAnimals(
            speciesId = speciesId,
            breedId = breedId,
            gender = gender,
            ageMin = ageMin,
            ageMax = ageMax,
            priceMin = priceMin,
            priceMax = priceMax,
            isHypoallergenic = isHypoallergenic,
            childFriendly = childFriendly,
            spaceRequirements = spaceRequirements,
            isSterilized = isSterilized,
            hasVaccinations = hasVaccinations
        ).enqueue(object: Callback<List<AnimalReq>> {
            override fun onResponse(call: Call<List<AnimalReq>>, response: Response<List<AnimalReq>>) {
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                animalsList = response.body().orEmpty()
                animalsLoaded = true
                tryRender()
            }

            override fun onFailure(call: Call<List<AnimalReq>>, t: Throwable) {
                Toast.makeText(context, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        RetrofitClient.apiService.getAnimalPhotos().enqueue(object: Callback<List<AnimalPhotoReq>> {
            override fun onResponse(call2: Call<List<AnimalPhotoReq>>, response: Response<List<AnimalPhotoReq>>) {
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                photosList = response.body().orEmpty()
                photosLoaded = true
                tryRender()
            }

            override fun onFailure(call: Call<List<AnimalPhotoReq>>, t: Throwable) {
                Toast.makeText(context, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        RetrofitClient.apiService.getStatuses().enqueue(object: Callback<List<StatusReq>> {
            override fun onResponse(call3: Call<List<StatusReq>>, response: Response<List<StatusReq>>) {
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                statusesList = response.body().orEmpty()
                statusesLoaded = true
                tryRender()
            }

            override fun onFailure(call: Call<List<StatusReq>>, t: Throwable) {
                Toast.makeText(context, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderGridWithPhotos(
        grid: GridLayout,
        context: Context,
        animals: List<AnimalReq>,
        photos: List<AnimalPhotoReq>,
        statuses: List<StatusReq>
    ) {
        grid.removeAllViews()
        val inflater = LayoutInflater.from(context)

        val photosMap = photos.associateBy { it.animal_id }
        val statusMap = statuses.associateBy { it.id }

        for (animal in animals) {
            val v = inflater.inflate(R.layout.item_ad, grid, false)
            val title = v.findViewById<TextView>(R.id.ad_title)
            val price = v.findViewById<TextView>(R.id.ad_price)
            val image = v.findViewById<ImageView>(R.id.ad_image)
            val status = v.findViewById<TextView>(R.id.ad_badge)

            title.text = animal.name ?: "Без названия"
            price.text = animal.price.toString()

            val photo = photosMap[animal.id]

            if (photo?.photo_url != null && photo.photo_url.isNotEmpty()) {
                Glide.with(context).load(photo.photo_url).centerCrop().into(image)
            } else {
                Glide.with(context).load(R.drawable.cat).centerCrop().into(image)
            }
            val status_ = statusMap[animal.status]
            if (status_ != null) {
                status.text = status_.name ?: "Неизвестно"
            } else {
                status.text = "Статус не найден"
            }

            grid.addView(v)
        }
    }
}
