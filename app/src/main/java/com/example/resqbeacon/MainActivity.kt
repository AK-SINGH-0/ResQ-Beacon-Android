package com.example.resqbeacon

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.resqbeacon.database.AppDatabase
import com.example.resqbeacon.databinding.ActivityMainBinding
import com.example.resqbeacon.network.IncidentLog
import com.example.resqbeacon.network.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager

    // --- Feature Variables ---
    private var sirenPlayer: MediaPlayer? = null
    private var fakeCallPlayer: MediaPlayer? = null
    private var isSirenPlaying = false
    private var sirenJob: Job? = null

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var isStrobeActive = false
    private var strobeJob: Job? = null

    // --- Auto-Stop Variable (Battery Saver) ---
    private var autoStopJob: Job? = null
    // 3 Minutes = 180,000ms.
    // Logic: Long enough to attract help, short enough to save battery for calls.
    private val EMERGENCY_DURATION_MS = 180_000L

    // --- Sensor Variables ---
    private var acceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAlertTime = 0L

    // --- Shake Detection Variables ---
    private var shakeTimestamp: Long = 0
    private var shakeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initHardware()
        checkPermissions()
        setupClickListeners()
    }

    private fun initHardware() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) { }
    }

    private fun setupClickListeners() {
        binding.btnPanic.setOnClickListener { confirmEmergency("PANIC BUTTON") }
        binding.btnSetupContacts.setOnClickListener { startActivity(Intent(this, ContactsActivity::class.java)) }
        binding.btnSiren.setOnClickListener { toggleSiren() }
        binding.btnStrobe.setOnClickListener { toggleStrobe() }
        binding.btnFakeCall.setOnClickListener { triggerFakeCall() }
        binding.btnHospitals.setOnClickListener { openSafePlacesMap() }
        binding.btnImSafe.setOnClickListener { sendSafetyUpdate() }
        binding.btnHelpline.setOnClickListener { startActivity(Intent(this, HelplineActivity::class.java)) }
        binding.btnTips.setOnClickListener { startActivity(Intent(this, GuideActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    // --- SENSOR LOGIC ---
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            val gForce = currentAcceleration / SensorManager.GRAVITY_EARTH
            val sharedPref = getSharedPreferences("ResQSettings", Context.MODE_PRIVATE)
            val isSensorEnabled = sharedPref.getBoolean("FALL_DETECTION_ENABLED", true)

            if (isSensorEnabled) {
                val currentTime = System.currentTimeMillis()

                // A. FALL DETECTION (High Impact > 50)
                if (acceleration > 50) {
                    if (currentTime - lastAlertTime > 10000) {
                        lastAlertTime = currentTime
                        Toast.makeText(this, "HIGH IMPACT DETECTED! AUTO-SENDING SOS...", Toast.LENGTH_LONG).show()
                        executeEmergencyProtocol("FALL DETECTED (Force: ${"%.1f".format(acceleration)})")
                    }
                }

                // B. SHAKE DETECTION (Violent > 4.5g)
                if (gForce > 4.5F) {
                    if (shakeTimestamp + 500 < currentTime) {
                        shakeCount = 0
                    }
                    shakeTimestamp = currentTime
                    shakeCount++

                    if (shakeCount >= 5) {
                        shakeCount = 0
                        if (currentTime - lastAlertTime > 10000) {
                            lastAlertTime = currentTime
                            Toast.makeText(this, "PANIC SHAKE DETECTED! SENDING SOS...", Toast.LENGTH_LONG).show()
                            executeEmergencyProtocol("SHAKE TRIGGER")
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    // --- DETERRENT FEATURES ---
    private fun toggleStrobe() {
        if (cameraId == null) {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show()
            return
        }
        isStrobeActive = !isStrobeActive
        if (isStrobeActive) {
            binding.btnStrobe.setCardBackgroundColor(Color.RED)
            binding.tvStrobeLabel.text = "ACTIVE!"
            strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                var state = true
                while (isActive && isStrobeActive) {
                    try {
                        cameraManager?.setTorchMode(cameraId!!, state)
                        state = !state
                        delay(50)
                    } catch (e: Exception) { isStrobeActive = false }
                }
                try { cameraManager?.setTorchMode(cameraId!!, false) } catch (e: Exception) {}
            }
        } else {
            stopStrobe()
        }
    }

    private fun stopStrobe() {
        isStrobeActive = false
        strobeJob?.cancel()
        binding.btnStrobe.setCardBackgroundColor(Color.parseColor("#3949AB"))
        binding.tvStrobeLabel.text = "STROBE"
        try { cameraId?.let { cameraManager?.setTorchMode(it, false) } } catch (e: Exception) {}
    }

    private fun toggleSiren() {
        if (isSirenPlaying) {
            stopSiren()
        } else {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            sirenPlayer = MediaPlayer.create(this, alertUri)?.apply {
                isLooping = true
                setVolume(1.0f, 1.0f)
                start()
            }
            isSirenPlaying = true
            binding.tvSirenLabel.text = "LOUD!"
            sirenJob = lifecycleScope.launch {
                var red = true
                while (isActive && isSirenPlaying) {
                    if (red) binding.btnSiren.setCardBackgroundColor(Color.RED)
                    else binding.btnSiren.setCardBackgroundColor(Color.WHITE)
                    red = !red
                    delay(200)
                }
            }
        }
    }

    private fun stopSiren() {
        sirenPlayer?.stop()
        sirenPlayer?.release()
        sirenPlayer = null
        isSirenPlaying = false
        sirenJob?.cancel()
        binding.btnSiren.setCardBackgroundColor(Color.parseColor("#FF9800"))
        binding.tvSirenLabel.text = "SIREN"
    }

    // --- STOP ALL (BATTERY SAVER) ---
    private fun stopAllAlerts() {
        if (isSirenPlaying) stopSiren()
        if (isStrobeActive) stopStrobe()
    }

    private fun triggerFakeCall() {
        if (fakeCallPlayer?.isPlaying == true) {
            stopFakeCall()
            return
        }
        Toast.makeText(this, "Fake call in 5 seconds...", Toast.LENGTH_SHORT).show()
        binding.btnFakeCall.setCardBackgroundColor(Color.DKGRAY)
        binding.tvFakeCallLabel.text = "WAIT..."
        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() { startFakeRing() }
        }.start()
    }

    private fun startFakeRing() {
        val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        fakeCallPlayer = MediaPlayer.create(this, ringUri).apply {
            isLooping = true
            setVolume(1.0f, 1.0f)
            start()
        }
        AlertDialog.Builder(this)
            .setTitle("Incoming Call")
            .setMessage("Unknown Number")
            .setPositiveButton("ANSWER") { _, _ -> stopFakeCall() }
            .setNegativeButton("DECLINE") { _, _ -> stopFakeCall() }
            .setCancelable(false)
            .show()
        binding.btnFakeCall.setCardBackgroundColor(Color.GREEN)
        binding.tvFakeCallLabel.text = "RINGING"
    }

    private fun stopFakeCall() {
        fakeCallPlayer?.stop()
        fakeCallPlayer?.release()
        fakeCallPlayer = null
        binding.btnFakeCall.setCardBackgroundColor(Color.parseColor("#546E7A"))
        binding.tvFakeCallLabel.text = "FAKE CALL"
    }

    private fun openSafePlacesMap() {
        val gmmIntentUri = Uri.parse("geo:0,0?q=hospital OR police station")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/hospital+OR+police")))
        }
    }

    private fun sendSafetyUpdate() {
        AlertDialog.Builder(this)
            .setTitle("Mark as Safe?")
            .setMessage("Send 'I AM SAFE' message to all contacts?")
            .setPositiveButton("SEND") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val contacts = AppDatabase.getDatabase(applicationContext).contactDao().getContactsListSnapshot()
                    val smsManager = SmsManager.getDefault()
                    contacts.forEach {
                        try {
                            smsManager.sendTextMessage(it.phone, null, "STATUS UPDATE: I am now SAFE. Thank you.", null, null)
                        } catch (e: Exception) {}
                    }
                    withContext(Dispatchers.Main) {
                        stopAllAlerts() // Also stop alerts if user marks safe
                        Toast.makeText(applicationContext, "Safety update sent.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // --- CORE EMERGENCY PROTOCOL ---
    private fun confirmEmergency(type: String) {
        AlertDialog.Builder(this)
            .setTitle("EMERGENCY DETECTED!")
            .setMessage("Sending alert in 5 seconds...\nType: $type")
            .setPositiveButton("SEND NOW") { _, _ -> executeEmergencyProtocol(type) }
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun executeEmergencyProtocol(type: String) {
        // 1. Start Deterrents
        if (!isSirenPlaying) toggleSiren()
        if (!isStrobeActive) toggleStrobe()

        // 2. Schedule Auto-Stop (Battery Saver)
        autoStopJob?.cancel()
        autoStopJob = lifecycleScope.launch {
            delay(EMERGENCY_DURATION_MS) // Wait 3 minutes
            stopAllAlerts()
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Alerts stopped to save battery.", Toast.LENGTH_LONG).show()
            }
        }

        Toast.makeText(this, "Initiating Alert Protocol...", Toast.LENGTH_SHORT).show()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    sendAlerts(location ?: Location("").apply { latitude=0.0; longitude=0.0 }, type)
                }
                .addOnFailureListener {
                    sendAlerts(Location("").apply { latitude=0.0; longitude=0.0 }, type)
                }
        } else {
            checkPermissions()
        }
    }

    private fun sendAlerts(location: Location, type: String) {
        val battery = getBatteryPercentage()
        val speedKmh = (location.speed * 3.6).toInt()
        val mapLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"

        val msgBody = "SOS! $type! Ankush is in danger!\nBat: $battery% | Spd: ${speedKmh}km/h\nLoc: $mapLink"

        lifecycleScope.launch(Dispatchers.IO) {
            val contacts = AppDatabase.getDatabase(applicationContext).contactDao().getContactsListSnapshot()
            if (contacts.isEmpty()) {
                withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "NO CONTACTS SAVED!", Toast.LENGTH_LONG).show() }
                return@launch
            }

            val smsManager = SmsManager.getDefault()
            contacts.forEach {
                try { smsManager.sendTextMessage(it.phone, null, msgBody, null, null) } catch (e: Exception){}
            }

            try { RetrofitClient.api.logIncident(IncidentLog(location.latitude, location.longitude, type)) } catch (e: Exception) {}

            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "SOS SENT TO ${contacts.size} CONTACTS", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getBatteryPercentage(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun checkPermissions() {
        val required = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (!required.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, required, 101)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            stopAllAlerts()
            fakeCallPlayer?.release()
            sensorManager.unregisterListener(this)
        }
    }
}