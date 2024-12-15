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

    private val REQUEST_TAKE_PHOTO = 101
    private val REQUEST_PICK_FROM_GALLERY = 102
    private val REQUEST_LOCATION_WEATHER = 100
    private var imageUri: Uri? = null // Saves URI of the Image
    private lateinit var noteImageView: ImageView

    @SuppressLint("NewApi")
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
            vibrate()

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

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        if (vibrator != null && vibrator.hasVibrator()) { // Check if the device supports vibration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android O and higher
                val vibrationEffect = VibrationEffect.createOneShot(
                    500, // Duration in milliseconds
                    VibrationEffect.DEFAULT_AMPLITUDE // Default vibration strength
                )
                vibrator.vibrate(vibrationEffect)
            } else {
                // For devices below Android O
                @Suppress("DEPRECATION")
                vibrator.vibrate(500) // Duration in milliseconds
            }
        } else {
            Toast.makeText(this, "Vibration not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNoteDetails(note: Note) {
        val noteTitle:String = note.title.toString()
        val noteMessage:String  = note.message.toString()
        val noteLongitude:String = note.longitude.orEmpty()
        val noteLatitude:String = note.latitude.orEmpty()
        val noteTemp:String = note.temp.orEmpty()
        val noteWeather:String = note.weather.orEmpty()

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.delete){
            showDeleteDialog()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showDeleteDialog() {
        // Show Confirm-Dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.confirm_delete)
        builder.setMessage(R.string.confirm_delete_message)

        // Confirm = Yes
        builder.setPositiveButton(R.string.yes){ dialog, _ ->
            // Delete Note, if ID is valid
            if (noteId >= 0) {
                noteDao.delete(Note(id = noteId, title = "", message = "", latitude = "", longitude = "", image = ""))
                MediaPlayer.create(this, R.raw.match).start()
                // Vibrate
                vibrate()
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
        if (grantResults.isEmpty() || grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
            Toast.makeText(this, R.string.permission, Toast.LENGTH_SHORT).show()
            return
        }

        when (requestCode) {
            REQUEST_TAKE_PHOTO -> {
                takePhoto() // Permission granted for taking a photo
            }
            REQUEST_PICK_FROM_GALLERY -> {
                pickFromGallery() // Permission granted for picking an image
            }
            REQUEST_LOCATION_WEATHER -> {
                if (showLocation) {
                    getLocation() // Permission granted for location
                } else if (showWeather) {
                    getWeatherInfo() // Permission granted for weather
                }
            }
            else -> {
                Toast.makeText(this, R.string.permission, Toast.LENGTH_SHORT).show()
            }
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

    private fun takePhoto() {
        if (checkAndRequestPermissions(REQUEST_TAKE_PHOTO)) {
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
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            } else {
                Toast.makeText(this, R.string.error_nocamera, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickFromGallery() {
        if (checkAndRequestPermissions(REQUEST_PICK_FROM_GALLERY)) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_PICK_FROM_GALLERY)
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
                REQUEST_TAKE_PHOTO -> {
                    val file = File(currentImagePath)
                    if (file.exists()) {
                        imageUri = Uri.fromFile(file)
                        noteImageView.setImageURI(imageUri)
                    } else {
                        Toast.makeText(this, R.string.error_noimage, Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_PICK_FROM_GALLERY -> {
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

    private fun checkAndRequestPermissions(requestCode: Int): Boolean {
        val permissions = mutableListOf<String>()

        // Add permissions based on the request
        when (requestCode) {
            REQUEST_TAKE_PHOTO -> {
                permissions.add(Manifest.permission.CAMERA)
            }
            REQUEST_PICK_FROM_GALLERY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            REQUEST_LOCATION_WEATHER -> {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        // Check whether permissions have already been granted
        val listPermissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (listPermissionsNeeded.isNotEmpty()) {
            // Request missing permissions
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), requestCode)
            false
        } else {
            // All permissions have already been granted
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