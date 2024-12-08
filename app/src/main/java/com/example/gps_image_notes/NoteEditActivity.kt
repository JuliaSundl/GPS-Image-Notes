package com.example.gps_image_notes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileOutputStream

class NoteEditActivity : AppCompatActivity(){

    var noteId: Int = -1
    private lateinit var db: NoteDatabase
    private lateinit var noteDao: NoteDao

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2
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

        // TODO Insert Back-Button in Toolbar
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
        lateinit var noteTitle: String
        lateinit var noteMessage: String
        lateinit var noteLongitude: String
        lateinit var noteLatitude: String
        lateinit var noteImage: String

        if (noteId >=0) {
            val note = noteDao.getNoteByID(noteId)
            noteTitle = note.title.toString()
            noteMessage = note.message.toString()
            noteLongitude = note.longitude.orEmpty()
            noteLatitude = note.latitude.orEmpty()
            noteImage = note.image.orEmpty()

            // Show Data in Editfields and TextViews
            findViewById<EditText>(R.id.editTitle).setText(noteTitle)
            findViewById<EditText>(R.id.editMessage).setText(noteMessage)

            latitudeTextView = findViewById(R.id.latitudeTextView)
            longitudeTextView = findViewById(R.id.longitudeTextView)
            latitudeTextView.text = noteLatitude
            longitudeTextView.text = noteLongitude

            noteImageView = findViewById(R.id.noteImageView)
//            noteImageView.setImageResource(R.drawable.ic_placeholder)

            // Show saved Image if available
            if (noteImage.isNotEmpty()) {
//                Log.d("NoteEditActivity", "Image URI: $noteImage")
                try {
                    imageUri = Uri.parse(noteImage)
                    Toast.makeText(this, "Image URI: $imageUri", Toast.LENGTH_LONG).show()
                    val file = File(Uri.parse(noteImage).path ?: "")
                    if (file.exists()) {
//                    Toast.makeText(this, "Image URI: $imageUri", Toast.LENGTH_LONG).show()
                        noteImageView.setImageURI(imageUri)
                    }else {
                        Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
//                    Log.e("NoteEditActivity", "Error setting image URI", e)
                    Toast.makeText(this, "Fehler bei Image-Anzeige", Toast.LENGTH_SHORT).show()
                    noteImageView.setImageResource(R.drawable.ic_placeholder)
                }
            } else {
                noteImageView.setImageResource(R.drawable.ic_placeholder)
            }
        }

        val buttonAdd = findViewById<Button>(R.id.addButton)
        buttonAdd.setOnClickListener {
            val titleEditText = findViewById<EditText>(R.id.editTitle)
            val messageEditText = findViewById<EditText>(R.id.editMessage)
            val latitudeTextView = findViewById<TextView>(R.id.latitudeTextView)
            val longitudeTextView = findViewById<TextView>(R.id.longitudeTextView)

            val newTitle = titleEditText.text.toString().trim()
            val newMessage = messageEditText.text.toString().trim()
            val newLatitude = latitudeTextView.text.toString().trim()
            val newLongitude = longitudeTextView.text.toString().trim()

            // Check if the fields are empty
            if (newTitle.isEmpty() || newMessage.isEmpty()) {
                Toast.makeText(this, R.string.empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imagePath = imageUri?.toString() // Convert Image-URI to String
//            Toast.makeText(this@NoteEditActivity, "ImagePath: $imagePath", Toast.LENGTH_LONG).show()

            val mediaPlayer: MediaPlayer

            // Insert or update data in the database
            if (noteId >= 0) {
                // Update Note
                noteDao.update(Note(newTitle, newMessage, newLatitude, newLongitude, imagePath, noteId))
                Toast.makeText(this@NoteEditActivity, R.string.updated, Toast.LENGTH_LONG).show()

                // Play update sound
                mediaPlayer = MediaPlayer.create(this, R.raw.gunshot_sound)
            } else {
                // Insert Note
                noteDao.insertAll(Note(newTitle, newMessage, newLatitude, newLongitude, imagePath))
                Toast.makeText(this@NoteEditActivity, R.string.inserted, Toast.LENGTH_LONG).show()

                // Play insert sound
                mediaPlayer = MediaPlayer.create(this, R.raw.gunshot_sound)
            }

            // Start the appropriate sound
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

        // Initialize Button and add ClickListener
        val locationButton = findViewById<Button>(R.id.locationButton)
        locationButton.setOnClickListener {
            getLocation()
        }

        noteImageView = findViewById(R.id.noteImageView)
        noteImageView.setImageResource(R.drawable.ic_placeholder)
        val selectImageButton = findViewById<Button>(R.id.selectImageButton)

        // Choose Image oder take Picture
        selectImageButton.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Add Image")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto() // Camera
                    1 -> pickFromGallery() // Gallery
                }
            }
            builder.show()
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
                noteDao.delete(Note(id = noteId, title = "", message = "", image = ""))
                MediaPlayer.create(this, R.raw.gunshot_sound).start()
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
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            latitudeTextView.text = getString(R.string.permission)
            longitudeTextView.text = getString(R.string.permission)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePhoto() {
        // Intent for Opening Camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    val uri = saveImageToInternalStorage(bitmap) // Save and mount URI
                    imageUri = uri
                    noteImageView.setImageURI(uri) // Show Image
                }
                REQUEST_IMAGE_PICK -> {
                    val uri = data?.data
                    if (uri != null) {
                        imageUri = uri
                        noteImageView.setImageURI(uri) // Show Image
                    }
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val filename = "${System.currentTimeMillis()}.jpg"
//        Toast.makeText(this, "Filename: $filename", Toast.LENGTH_LONG).show()
        val file = File(filesDir, filename)
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.close()
//        val helpUri = Uri.fromFile(file)
//        Toast.makeText(this, "Uri: $helpUri", Toast.LENGTH_LONG).show()
//        return helpUri
        return Uri.fromFile(file)
    }
}