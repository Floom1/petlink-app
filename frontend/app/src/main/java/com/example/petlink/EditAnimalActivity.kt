package com.example.petlink

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petlink.adapter.ExistingPhotoAdapter
import com.example.petlink.adapter.LocalPhotoAdapter
import com.example.petlink.data.model.*
import com.example.petlink.util.RetrofitClient
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class EditAnimalActivity : AppCompatActivity() {

    private var animalId: Int? = null

    private lateinit var progress: ProgressBar
    private lateinit var btnCancel: Button
    private lateinit var btnDeleteTop: Button
    private lateinit var btnAddPhotos: Button
    private lateinit var btnSave: Button

    private lateinit var recyclerLocal: RecyclerView
    private lateinit var recyclerExisting: RecyclerView
    private lateinit var localAdapter: LocalPhotoAdapter
    private lateinit var existingAdapter: ExistingPhotoAdapter

    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerBreed: Spinner
    private lateinit var spinnerSpace: Spinner

    private lateinit var etName: EditText
    private lateinit var etAge: EditText
    private lateinit var etColor: EditText
    private lateinit var etDescription: EditText
    private lateinit var etPrice: EditText
    private lateinit var cbFree: CheckBox

    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton

    private lateinit var cbSterilized: CheckBox
    private lateinit var cbVaccinations: CheckBox
    private lateinit var cbHypoallergenic: CheckBox
    private lateinit var cbChildFriendly: CheckBox

    private var speciesList: List<SpeciesReq> = emptyList()
    private var breedList: List<BreedReq> = emptyList()

    private val localUris = mutableListOf<Uri>()
    private var existingPhotos = mutableListOf<AnimalPhotoReq>()

    private lateinit var pickImagesLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_animal)

        animalId = intent.getIntExtra("animal_id", -1).takeIf { it != -1 }

        initViews()
        wireUi()

        loadDictionaries {
            if (animalId != null) loadExisting() // edit mode
        }
    }

    private fun showSaveError(response: Response<AnimalSimpleResponse>) {
        val msg = try {
            val raw = response.errorBody()?.string()
            if (raw.isNullOrEmpty()) null else {
                val j = JSONObject(raw)
                when {
                    j.has("detail") -> j.optString("detail")
                    j.has("gender") -> j.getJSONArray("gender").optString(0)
                    j.has("color") -> j.getJSONArray("color").optString(0)
                    j.has("status") -> j.getJSONArray("status").optString(0)
                    else -> raw
                }
            }
        } catch (e: Exception) { null }
        Toast.makeText(this, msg ?: "Ошибка сохранения", Toast.LENGTH_SHORT).show()
    }

    private fun initViews() {
        progress = findViewById(R.id.progress)
        btnCancel = findViewById(R.id.btnCancel)
        btnDeleteTop = findViewById(R.id.btnDeleteTop)
        btnAddPhotos = findViewById(R.id.btnAddPhotos)
        btnSave = findViewById(R.id.btnSave)

        recyclerLocal = findViewById(R.id.recyclerLocalPhotos)
        recyclerExisting = findViewById(R.id.recyclerExistingPhotos)
        recyclerLocal.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerExisting.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        localAdapter = LocalPhotoAdapter { pos -> localUris.removeAt(pos); localAdapter.submit(localUris) }
        existingAdapter = ExistingPhotoAdapter { photo -> deleteExistingPhoto(photo) }
        recyclerLocal.adapter = localAdapter
        recyclerExisting.adapter = existingAdapter

        spinnerSpecies = findViewById(R.id.spinnerSpecies)
        spinnerBreed = findViewById(R.id.spinnerBreed)
        spinnerSpace = findViewById(R.id.spinnerSpace)

        etName = findViewById(R.id.etName)
        etAge = findViewById(R.id.etAge)
        etColor = findViewById(R.id.etColor)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        cbFree = findViewById(R.id.cbFree)

        rgGender = findViewById(R.id.rgGender)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)

        cbSterilized = findViewById(R.id.cbSterilized)
        cbVaccinations = findViewById(R.id.cbVaccinations)
        cbHypoallergenic = findViewById(R.id.cbHypoallergenic)
        cbChildFriendly = findViewById(R.id.cbChildFriendly)

        pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                localUris.addAll(uris)
                localAdapter.submit(localUris)
            }
        }
    }

    private fun wireUi() {
        btnCancel.setOnClickListener { finish() }
        btnAddPhotos.setOnClickListener { pickImagesLauncher.launch("image/*") }
        cbFree.setOnCheckedChangeListener { _, isChecked -> etPrice.visibility = if (isChecked) View.GONE else View.VISIBLE }
        btnSave.setOnClickListener { onSave() }
        btnDeleteTop.setOnClickListener { confirmDelete() }

        val spaceValues = listOf("low", "medium", "high")
        val spaceLabels = listOf("Низкие", "Средние", "Высокие")
        spinnerSpace.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spaceLabels)
        spinnerSpace.setSelection(1)

        spinnerSpecies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = speciesList.getOrNull(position)
                val filtered = breedList.filter { it.species == selected?.id }
                spinnerBreed.adapter = ArrayAdapter(this@EditAnimalActivity, android.R.layout.simple_spinner_dropdown_item, filtered.map { it.name })
                spinnerBreed.isEnabled = selected != null
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadDictionaries(done: () -> Unit) {
        RetrofitClient.apiService.getSpecies().enqueue(object: Callback<List<SpeciesReq>> {
            override fun onResponse(call: Call<List<SpeciesReq>>, response: Response<List<SpeciesReq>>) {
                speciesList = response.body().orEmpty()
                spinnerSpecies.adapter = ArrayAdapter(this@EditAnimalActivity, android.R.layout.simple_spinner_dropdown_item, speciesList.map { it.name })
                RetrofitClient.apiService.getBreeds().enqueue(object: Callback<List<BreedReq>> {
                    override fun onResponse(call2: Call<List<BreedReq>>, resp: Response<List<BreedReq>>) {
                        breedList = resp.body().orEmpty()
                        done()
                    }
                    override fun onFailure(call2: Call<List<BreedReq>>, t: Throwable) { done() }
                })
            }
            override fun onFailure(call: Call<List<SpeciesReq>>, t: Throwable) { spinnerSpecies.adapter = ArrayAdapter(this@EditAnimalActivity, android.R.layout.simple_spinner_dropdown_item, emptyList<String>()); done() }
        })
    }

    private fun loadExisting() {
        val id = animalId ?: return
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", null) ?: return
        progress.visibility = View.VISIBLE
        RetrofitClient.apiService.getAnimalDetail(id).enqueue(object: Callback<AnimalSimpleResponse> {
            override fun onResponse(call: Call<AnimalSimpleResponse>, response: Response<AnimalSimpleResponse>) {
                if (response.isSuccessful) {
                    val a = response.body()
                    if (a != null) fillForm(a)
                    // load photos
                    RetrofitClient.apiService.getAnimalPhotos(a?.id).enqueue(object: Callback<List<AnimalPhotoReq>> {
                        override fun onResponse(call2: Call<List<AnimalPhotoReq>>, resp: Response<List<AnimalPhotoReq>>) {
                            if (resp.isSuccessful) {
                                existingPhotos = resp.body()?.toMutableList() ?: mutableListOf()
                                existingAdapter.submit(existingPhotos)
                            }
                            progress.visibility = View.GONE
                            updateDeleteVisibility(a)
                        }
                        override fun onFailure(call2: Call<List<AnimalPhotoReq>>, t: Throwable) { progress.visibility = View.GONE; updateDeleteVisibility(a) }
                    })
                } else {
                    progress.visibility = View.GONE
                }
            }
            override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) { progress.visibility = View.GONE }
        })
    }

    private fun updateDeleteVisibility(a: AnimalSimpleResponse?) {
        if (a == null) return
        // Only show delete if active (available)
        btnDeleteTop.visibility = View.VISIBLE
    }

    private fun fillForm(a: AnimalSimpleResponse) {
        etName.setText(a.name ?: "")
        etAge.setText(a.age?.toString() ?: "")
        when (a.gender) { "M" -> rgGender.check(rbMale.id); "F" -> rgGender.check(rbFemale.id) }
        etColor.setText(a.color ?: "")
        etDescription.setText(a.description ?: "")
        cbSterilized.isChecked = a.is_sterilized == true
        cbVaccinations.isChecked = a.has_vaccinations == true
        cbHypoallergenic.isChecked = a.is_hypoallergenic == true
        cbChildFriendly.isChecked = a.child_friendly == true
        // space
        val idx = when (a.space_requirements) { "low" -> 0; "high" -> 2; else -> 1 }
        spinnerSpace.setSelection(idx)
        if (a.price == null || a.price == 0.0) { cbFree.isChecked = true; etPrice.visibility = View.GONE } else { etPrice.setText(a.price.toString()) }
        // species/breed selection by IDs
        val breed = a.breed
        val speciesId = breedList.firstOrNull { it.id == breed }?.species
        val spIndex = speciesList.indexOfFirst { it.id == speciesId }
        if (spIndex >= 0) spinnerSpecies.setSelection(spIndex)
        val filtered = breedList.filter { it.species == speciesId }
        spinnerBreed.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filtered.map { it.name })
        val brIndex = filtered.indexOfFirst { it.id == breed }
        if (brIndex >= 0) spinnerBreed.setSelection(brIndex)
    }

    private fun onSave() {
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        if (token.isNullOrEmpty()) { Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show(); return }

        val ageVal = etAge.text?.toString()?.trim()?.toDoubleOrNull()
        if (ageVal != null && (ageVal < 0 || ageVal > 30)) { etAge.error = "Возраст 0–30"; etAge.requestFocus(); return }

        val selectedSpecies = speciesList.getOrNull(spinnerSpecies.selectedItemPosition)
        val filteredBreeds = breedList.filter { it.species == selectedSpecies?.id }
        val selectedBreed = filteredBreeds.getOrNull(spinnerBreed.selectedItemPosition)

        // Обязательные поля: вид, цвет
        if (selectedSpecies == null) {
            Toast.makeText(this, "Выберите вид животного", Toast.LENGTH_SHORT).show()
            spinnerSpecies.requestFocus()
            return
        }

        val colorText = etColor.text?.toString()?.trim() ?: ""
        if (colorText.isEmpty()) {
            etColor.error = "Введите цвет"
            etColor.requestFocus()
            return
        }

        val gender = if (rgGender.checkedRadioButtonId == rbFemale.id) "F" else "M"
        val price = if (cbFree.isChecked) null else etPrice.text?.toString()?.trim()?.toDoubleOrNull()

        val fields = mutableMapOf<String, Any?>()
        fields["name"] = etName.text?.toString()?.trim()
        fields["breed"] = selectedBreed?.id
        fields["age"] = ageVal
        fields["gender"] = gender
        fields["color"] = colorText
        fields["description"] = etDescription.text?.toString()?.trim()
        fields["price"] = price
        fields["is_sterilized"] = cbSterilized.isChecked
        fields["has_vaccinations"] = cbVaccinations.isChecked
        fields["is_hypoallergenic"] = cbHypoallergenic.isChecked
        fields["child_friendly"] = cbChildFriendly.isChecked
        fields["space_requirements"] = when (spinnerSpace.selectedItemPosition) { 0 -> "low"; 2 -> "high"; else -> "medium" }

        val id = animalId
        if (id == null) {
            if (localUris.isEmpty()) { Toast.makeText(this, "Добавьте хотя бы одно фото", Toast.LENGTH_SHORT).show(); return }
            progress.visibility = View.VISIBLE
            RetrofitClient.apiService.createAnimal("Token $token", fields).enqueue(object: Callback<AnimalSimpleResponse> {
                override fun onResponse(call: Call<AnimalSimpleResponse>, response: Response<AnimalSimpleResponse>) {
                    if (response.isSuccessful) {
                        val newAnimal = response.body()!!
                        uploadAndLinkPhotos(token, newAnimal.id.toInt())
                    } else {
                        progress.visibility = View.GONE
                        showSaveError(response)
                    }
                }
                override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) { progress.visibility = View.GONE; Toast.makeText(this@EditAnimalActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show() }
            })
        } else {
            progress.visibility = View.VISIBLE
            RetrofitClient.apiService.updateAnimal("Token $token", id, fields).enqueue(object: Callback<AnimalSimpleResponse> {
                override fun onResponse(call: Call<AnimalSimpleResponse>, response: Response<AnimalSimpleResponse>) {
                    if (response.isSuccessful) {
                        if (localUris.isNotEmpty()) uploadAndLinkPhotos(token, id) else { progress.visibility = View.GONE; finish() }
                    } else {
                        progress.visibility = View.GONE
                        showSaveError(response)
                    }
                }
                override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) { progress.visibility = View.GONE; Toast.makeText(this@EditAnimalActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show() }
            })
        }
    }

    private fun uploadAndLinkPhotos(token: String, animalId: Int) {
        if (localUris.isEmpty()) { progress.visibility = View.GONE; finish(); return }
        var completed = 0
        localUris.forEachIndexed { index, uri ->
            val temp = uriToTempFile(uri) ?: run { nextPhoto(++completed); return@forEachIndexed }
            val part = MultipartBody.Part.createFormData("image", temp.name, temp.asRequestBody("image/*".toMediaTypeOrNull()))
            RetrofitClient.apiService.uploadProfileImage("Token $token", part).enqueue(object: Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    val url = response.body()?.url
                    if (!url.isNullOrEmpty()) {
                        val body = AnimalPhotoCreate(animal_id_write = animalId, photo_url = url, is_main = index == 0, order = index)
                        RetrofitClient.apiService.createAnimalPhoto("Token $token", body).enqueue(object: Callback<AnimalPhotoReq> {
                            override fun onResponse(call2: Call<AnimalPhotoReq>, resp: Response<AnimalPhotoReq>) { nextPhoto(++completed) }
                            override fun onFailure(call2: Call<AnimalPhotoReq>, t: Throwable) { nextPhoto(++completed) }
                        })
                    } else nextPhoto(++completed)
                }
                override fun onFailure(call: Call<UploadResponse>, t: Throwable) { nextPhoto(++completed) }
            })
        }
    }

    private fun nextPhoto(done: Int) { if (done >= localUris.size) { progress.visibility = View.GONE; finish() } }

    private fun uriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { out ->
                inputStream.use { inp ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (inp.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }
            tempFile
        } catch (e: Exception) { null }
    }

    private fun deleteExistingPhoto(photo: AnimalPhotoReq) {
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", null) ?: return
        RetrofitClient.apiService.deleteAnimalPhoto("Token $token", photo.id).enqueue(object: Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                existingPhotos.removeAll { it.id == photo.id }
                existingAdapter.submit(existingPhotos)
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun confirmDelete() {
        if (animalId == null) return
        AlertDialog.Builder(this)
            .setMessage("Вы уверены, что хотите удалить это объявление? Все заявки на это животное будут отклонены.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ -> performDelete() }
            .show()
    }

    private fun performDelete() {
        val id = animalId ?: return
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", null) ?: return
        RetrofitClient.apiService.deleteAnimal("Token $token", id).enqueue(object: Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) { finish() }
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }
}
