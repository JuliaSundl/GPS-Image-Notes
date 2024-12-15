package com.example.gps_image_notes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class NoteEditActivity : AppCompatActivity(){

    var noteId: Int = -1
    var currentImagePath: String? = null
    private lateinit var db: NoteDatabase
    private lateinit var noteDao: NoteDao

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var weatherDescTextView: TextView
    private var showLocation: Boolean = false
    private var showWeather: Boolean = false

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2
    private var imageUri: Uri? = null // Saves URI of the Image
    private lateinit var noteImageView: ImageView

    @SuppressLint("NewApi", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Connect Main-File with Layout-File
        setContentView(R.layout.activity_note_editor)

        // Set up Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Initialize Database
        db = Room.databaseBuilder(
            applicationContext,
            NoteDatabase::class.java, "notes"
        ).allowMainThreadQueries().build()
        noteDao = db.noteDao()

        // Get ID from selected Note
        noteId = intent.getIntExtra("id", -1)
        lateinit var note: Note

        if (noteId >=0) {
            CoroutineScope(Dispatchers.IO).launch {
                note = noteDao.getNoteByID(noteId)
                note.let { runOnUiThread { showNoteDetails(it) } }
            }
        }

        val buttonAdd = findViewById<Button>(R.id.addButton)
        buttonAdd.setOnClickListener {
            val titleEditText = findViewById<EditText>(R.id.editTitle)
            val messageEditText = findViewById<EditText>(R.id.editMessage)
            val latitudeTextView = findViewById<TextView>(R.id.latitudeTextView)
            val longitudeTextView = findViewById<TextView>(R.id.longitudeTextView)
            val tempTextView = findViewById<TextView>(R.id.tempTextView)
            val weatherDescTextView = findViewById<TextView>(R.id.weatherTextView)

            val newTitle = titleEditText.text.toString().trim()
            val newMessage = messageEditText.text.toString().trim()
            val newLatitude = latitudeTextView.text.toString().trim()
            val newLongitude = longitudeTextView.text.toString().trim()
            val newTemp = tempTextView.text.toString().trim()
            val newWeatherDesc = weatherDescTextView.text.toString().trim()

            // Check if the fields are empty
            if (newTitle.isEmpty() || newMessage.isEmpty()) {
                Toast.makeText(this, R.string.empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Insert or update data in the database
            if (noteId >= 0) {
                // Update Note
                noteDao.update(Note(newTitle, newMessage, newLatitude, newLongitude, currentImagePath, newTemp, newWeatherDesc, noteId))
                Toast.makeText(this@NoteEditActivity, R.string.updated, Toast.LENGTH_LONG).show()
            } else {
                // Insert Note
                noteDao.insertAll(Note(newTitle, newMessage, newLatitude, newLongitude, currentImagePath, newTemp, newWeatherDesc))
                Toast.makeText(this@NoteEditActivity, R.string.inserted, Toast.LENGTH_LONG).show()
            }

            // Play Sound
            val mediaPlayer = MediaPlayer.create(this, R.raw.match)
            mediaPlayer.start()

            // Vibrate
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            }

            // Finish Activity
            finish()
        }

        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // TextViews for Latitude and Longitude
        latitudeTextView = findViewById(R.id.latitudeTextView)
        longitudeTextView = findViewById(R.id.longitudeTextView)

        // Initialize ShowLocationButton and add ClickListener
        val locationButton = findViewById<Button>(R.id.locationButton)
        locationButton.setOnClickListener {
            showLocation = true
            showWeather = false
            getLocation()
        }

        noteImageView = findViewById(R.id.noteImageView)
        noteImageView.setImageResource(R.drawable.ic_placeholder)
        val selectImageButton = findViewById<Button>(R.id.selectImageButton)

        // Choose Image oder take Picture
        selectImageButton.setOnClickListener {
            val photo = getString(R.string.photo)
            val gallery = getString(R.string.gallery)
            val options = arrayOf(photo,gallery)
            val builder = AlertDialog.Builder(this)

            builder.setTitle(R.string.chooseSource)
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto() // Camera
                    1 -> pickFromGallery()// Gallery
                }
            }
            builder.show()
        }

        // Initialize ShowWeatherButton and add ClickListener
        val weatherButton = findViewById<Button>(R.id.weatherButton)
        weatherButton.setOnClickListener {
            showLocation = false
            showWeather = true
            getWeatherInfo()
        }
    }

    private fun showNoteDetails(note: Note) {
        var noteTitle:String = note.title.toString()
        var noteMessage:String  = note.message.toString()
        var noteLongitude:String = note.longitude.orEmpty()
        var noteLatitude:String = note.latitude.orEmpty()
        var noteTemp:String = note.temp.orEmpty()
        var noteWeather:String = note.weather.orEmpty()

        // Show Data in Editfields and TextViews
        findViewById<EditText>(R.id.editTitle).setText(noteTitle)
        findViewById<EditText>(R.id.editMessage).setText(noteMessage)

        latitudeTextView = findViewById(R.id.latitudeTextView)
        longitudeTextView = findViewById(R.id.longitudeTextView)
        latitudeTextView.text = noteLatitude
        longitudeTextView.text = noteLongitude

        temperatureTextView = findViewById(R.id.tempTextView)
        weatherDescTextView = findViewById(R.id.weatherTextView)
        temperatureTextView.text = noteTemp
        weatherDescTextView.text = noteWeather

        noteImageView = findViewById(R.id.noteImageView)

        note.image?.let { path ->
            val file = File(path)
            currentImagePath = path
            if (file.exists()) {
                val uri = Uri.fromFile(file)
                noteImageView.setImageURI(uri)
            } else {
                noteImageView.setImageResource(R.drawable.ic_placeholder)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // TODO Delete Note from Database
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.delete){
            showDeleteDialog()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showDeleteDialog() {
        // Show Confirm-Dialog
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.confirm_delete)
        builder.setMessage(R.string.confirm_delete_message)

        // Confirm = Yes
        builder.setPositiveButton(R.string.yes){ dialog, _ ->
            // Delete Note, if ID is valid
            if (noteId >= 0) {
                noteDao.delete(Note(id = noteId, title = "", message = "", latitude = "", longitude = "", image = ""))
                MediaPlayer.create(this, R.raw.match).start()
                Toast.makeText(this@NoteEditActivity, R.string.deleted, Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@NoteEditActivity, R.string.error_deleted, Toast.LENGTH_LONG).show()
            }
        }

        // Confirm = No
        builder.setNegativeButton(R.string.no){ dialog, _ ->
            // Do nothing, close Dialog
            dialog.dismiss()
        }

        // Show Dialog
        builder.show()
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Get Permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Show Latitude and Longitude in TextViews
                    latitudeTextView.text = getString(R.string.latitude) + ": " + location.latitude.toString()
                    longitudeTextView.text = getString(R.string.longitude) + ": " + location.longitude.toString()
                } else {
                    // If no Location is available
                    latitudeTextView.text = getString(R.string.available)
                    longitudeTextView.text = getString(R.string.available)
                }
            }
            .addOnFailureListener {
                // Error handling
                latitudeTextView.text = getString(R.string.error)
                longitudeTextView.text = getString(R.string.error)
            }
    }

    // Process authorization requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            takePhoto()
        } else if (requestCode == 1 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            pickFromGallery()
        } else if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (showLocation) { getLocation() }
            if (showWeather) { getWeatherInfo() }
        } else if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (showLocation) {
                latitudeTextView.text = getString(R.string.permission)
                longitudeTextView.text = getString(R.string.permission)
            }
            if (showWeather) {
                temperatureTextView.text = getString(R.string.permission)
                weatherDescTextView.text = getString(R.string.permission)
            }
        } else {
            Toast.makeText(this, R.string.permission, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWeatherInfo() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Get Permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                fetchWeather(location.latitude, location.longitude)
            } else {
            // If no Location is available
            temperatureTextView.text = getString(R.string.available)
            weatherDescTextView.text = getString(R.string.available)
            }
        }.addOnFailureListener {
            // Error handling
            temperatureTextView.text = getString(R.string.error)
            weatherDescTextView.text = getString(R.string.error)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePhoto() {
        if (checkAndRequestPermissions()) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(this, "com.example.gps_image_notes.fileprovider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } else {
                Toast.makeText(this, R.string.error_nocamera, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickFromGallery() {
        if (checkAndRequestPermissions()) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply { currentImagePath = absolutePath }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val file = File(currentImagePath)
//                    Toast.makeText(this, "Akt.Photo: $currentImagePath", Toast.LENGTH_LONG).show()
                    if (file.exists()) {
                        imageUri = Uri.fromFile(file)
                        noteImageView.setImageURI(imageUri)
                    } else {
                        Toast.makeText(this, R.string.error_noimage, Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val uri = data?.data
                    uri?.let {
                        currentImagePath = getPathFromUri(it)
                        noteImageView.setImageURI(it)
                    }
                }
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String {
        var path = ""
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = cursor.getString(columnIndex)
            }
        }
        return path
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val listPermissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 0)
            false
        } else {
            true
        }
    }

    private fun fetchWeather(latitude: Double, longitude: Double) {
        val apiKey = "c45850e37187a2a81e574cc7db83578f" // API key for OpenWeatherMap.org
        val language = Locale.getDefault().language
        val apiLanguage = if (language in listOf("de", "en", "es", "fr")) language else "en"

        RetrofitInstance.api.getWeather(latitude, longitude, apiKey, lang = apiLanguage).enqueue(object : retrofit2.Callback<WeatherResponse> {
            override fun onResponse(call: retrofit2.Call<WeatherResponse>, response: retrofit2.Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    val temperature = weather?.main?.temp
                    val description = weather?.weather?.get(0)?.description

                    // Show Weather Infos in App
                    findViewById<TextView>(R.id.tempTextView).text = getString(R.string.temperature) + ": $temperature °C"
                    findViewById<TextView>(R.id.weatherTextView).text = getString(R.string.weatherInfo) + ": $description"
                } else {
                    Toast.makeText(this@NoteEditActivity, R.string.error_weather, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@NoteEditActivity, R.string.error_network, Toast.LENGTH_SHORT).show()
            }
        })
    }
}