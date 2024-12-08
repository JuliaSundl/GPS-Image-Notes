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
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class NoteEditActivity : AppCompatActivity(){

    var noteId: Int = -1
    var currentPhotoPath: String? = null
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
            Toast.makeText(this, "ImagePath from DB: $noteImage", Toast.LENGTH_LONG).show()

            // Show saved Image if available
            if (noteImage.isNotEmpty()) {
                try {
                    // Wenn der gespeicherte Bildpfad nicht leer ist, versuche ihn in eine Uri zu konvertieren
                    imageUri = Uri.parse(noteImage)
                    // Prüfen, ob die Datei existiert
                    val file = File(imageUri?.path ?: "")
                    if (file.exists()) {
                        // Wenn die Datei existiert, setze die URI im ImageView
                        noteImageView.setImageURI(imageUri)
                    } else {
                        // Wenn die Datei nicht gefunden wurde, zeige eine Fehlernachricht an
                        Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
                        noteImageView.setImageResource(R.drawable.ic_placeholder) // Platzhalterbild
                    }
                } catch (e: Exception) {
                    // Fehler beim Laden des Bildes
                    Toast.makeText(this, "Fehler bei der Bildanzeige", Toast.LENGTH_SHORT).show()
                    noteImageView.setImageResource(R.drawable.ic_placeholder) // Platzhalterbild
                }
            } else {
                // Wenn kein Bildpfad vorhanden ist, zeige das Platzhalterbild an
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

//            Toast.makeText(this@NoteEditActivity, "ImagePath: $imagePath", Toast.LENGTH_LONG).show()

            val mediaPlayer: MediaPlayer

            // Insert or update data in the database
            if (noteId >= 0) {
                // Update Note
                noteDao.update(Note(newTitle, newMessage, newLatitude, newLongitude, currentPhotoPath, noteId))
                Toast.makeText(this@NoteEditActivity, R.string.updated, Toast.LENGTH_LONG).show()

                // Play update sound
                mediaPlayer = MediaPlayer.create(this, R.raw.gunshot_sound)
            } else {
                // Insert Note
                noteDao.insertAll(Note(newTitle, newMessage, newLatitude, newLongitude, currentPhotoPath))
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
                noteDao.delete(Note(id = noteId, title = "", message = "", latitude = "", longitude = "", image = ""))
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
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile() // Diese Methode erstellt die Bilddatei
            } catch (ex: IOException) {
                Log.e("NoteEditActivity", "Error occurred while creating the file", ex)
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(this, "com.example.gps_image_notes.fileprovider", it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Log.e("NoteEditActivity", "No activity found to handle the intent")
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    /**
     * Creates an image file to store the captured image.
     * @return The created image file.
     * @throws IOException If an error occurs while creating the file.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply { currentPhotoPath = absolutePath }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val file = File(currentPhotoPath)
                    if (file.exists()) {
                        imageUri = Uri.fromFile(file)
                        noteImageView.setImageURI(imageUri) // Das Bild anzeigen
                    } else {
                        Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val uri = data?.data
                    uri?.let {
                        currentPhotoPath = getPathFromUri(it)
                        noteImageView.setImageURI(it)
                    }
                }
            }
        }
    }

    /**
     * Retrieves the file path from a URI.
     * @param uri The URI to retrieve the path from.
     * @return The file path.
     */
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

//    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
//        val filename = "${System.currentTimeMillis()}.jpg"
////        Toast.makeText(this, "Filename: $filename", Toast.LENGTH_LONG).show()
//        val file = File(filesDir, filename)
//        val fos = FileOutputStream(file)
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
//        fos.close()
////        val helpUri = Uri.fromFile(file)
////        Toast.makeText(this, "Uri: $helpUri", Toast.LENGTH_LONG).show()
////        return helpUri
//        return Uri.fromFile(file)
//    }

    /**
     * Checks and requests necessary permissions for camera and storage access.
     * @return True if permissions are already granted, false otherwise.
     */
    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val listPermissionsNeeded = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        return if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 0)
            false
        } else {
            true
        }
    }

}