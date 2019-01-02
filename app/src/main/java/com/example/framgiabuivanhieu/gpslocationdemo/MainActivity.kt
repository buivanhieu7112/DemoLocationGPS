package com.example.framgiabuivanhieu.gpslocationdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), LocationListener {

  private lateinit var mLocationManager: LocationManager
  private var mHasGps = false
  private var mHasNetWork = false
  private var mLocationGPS: Location? = null
  private var mLocationNetWork: Location? = null


  private var mPermissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
      android.Manifest.permission.ACCESS_COARSE_LOCATION)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    disableView()
    initCheckPermission()
  }

  private fun initCheckPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (checkPermission(mPermissions)) {
        enableView()
      } else {
        requestPermissions(mPermissions, PERMISSION_REQUEST)
      }
    } else {
      enableView()
    }
  }

  private fun enableView() {
    buttonGetLocation.isEnabled = true
    buttonGetLocation.alpha = 1F
    buttonStopGetLocation.isEnabled = true
    buttonStopGetLocation.alpha = 1F
    buttonShowOnMap.setOnClickListener { showMap() }
    buttonGetLocation.setOnClickListener { getLocation() }
    buttonStopGetLocation.setOnClickListener { stopGetLocation() }
    Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
  }

  private fun showMap() {
    if(mLocationGPS!= null) {
      intent = Intent(this@MainActivity,MapsActivity::class.java)
      intent.putExtra("LATITUDE",mLocationGPS!!.latitude)
      intent.putExtra("LONGITUDE",mLocationGPS!!.longitude)
          startActivity(intent)
    }
  }

  private fun stopGetLocation() {
    mLocationManager.removeUpdates(this)
    Toast.makeText(this, "Has Been Stopped", Toast.LENGTH_SHORT).show()
  }

  private fun disableView() {
    buttonGetLocation.isEnabled = false
    buttonGetLocation.alpha = 0.5F
    buttonStopGetLocation.isEnabled = false
    buttonStopGetLocation.alpha = 0.5F

  }

  @SuppressLint("MissingPermission")
  private fun getLocation() {
    mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    mHasGps = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    mHasNetWork = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    if (mHasGps || mHasNetWork) {
      if (mHasGps) {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES, this)

        val localGpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (localGpsLocation != null) {
          mLocationGPS = localGpsLocation
        }
      }
      if (mHasNetWork) {
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
            MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this)

        val localNetWorkLocation = mLocationManager.getLastKnownLocation(
            LocationManager.NETWORK_PROVIDER)
        if (localNetWorkLocation != null) {
          mLocationNetWork = localNetWorkLocation
        }
      }


      if (mLocationGPS != null && mLocationNetWork != null) {
        if (mLocationGPS!!.accuracy > mLocationNetWork!!.accuracy) {
          textViewResult.append("\nNetWork at Fist")
          textViewResult.append("\nLatitude: " + mLocationNetWork!!.latitude)
          textViewResult.append("\nLongitude: " + mLocationNetWork!!.longitude)
        } else {
          textViewResult.append("\nGPS at Fist")
          textViewResult.append("\nLatitude: " + mLocationGPS!!.latitude)
          textViewResult.append("\nLongitude: " + mLocationGPS!!.longitude)

        }
      }
    } else {
      startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
  }

  override fun onLocationChanged(location: Location?) {
    if (mHasGps) {
      Log.d("GET_GPS_LOCATION", "hasGPS")
      if (location != null) {
        mLocationGPS = location
        textViewResult.append("\nGPSChanged")
        textViewResult.append("\nLatitude: " + mLocationGPS!!.latitude)
        textViewResult.append("\nLongitude: " + mLocationGPS!!.longitude)

        textViewResult.append("\nTime: " + Calendar.getInstance().time)
      }
    }
    if (mHasNetWork) {
      Log.d("GET_LOCATION", "hasNetWork")
      if (location != null) {
        mLocationNetWork = location
        textViewResult.append("\nNetWorkChanged")
        textViewResult.append("\nLatitude: " + mLocationNetWork!!.latitude)
        textViewResult.append("\nLongitude: " + mLocationNetWork!!.longitude)
      }
    }

  }

  override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
  }

  override fun onProviderEnabled(provider: String?) {
  }

  override fun onProviderDisabled(provider: String?) {
  }


  private fun checkPermission(mPermissions: Array<String>): Boolean {
    var allSuccess = true
    for (i in mPermissions.indices) {
      if (checkCallingOrSelfPermission(mPermissions[i]) == PackageManager.PERMISSION_DENIED) {
        allSuccess = false
      }
    }
    return allSuccess
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
      grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSION_REQUEST) {
      var allSuccess = true
      for (i in permissions.indices) {
        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
          allSuccess = false
          val requestAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(
              permissions[i])
          if (requestAgain) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(this, "Go to settings and enable the permission",
                Toast.LENGTH_SHORT).show()
          }
        }
      }
      if (allSuccess)
        enableView()

    }
  }

  companion object {
    private const val PERMISSION_REQUEST = 10
    // The minimum distance to change Updates in meters
    private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 0F
    // The minimum time between updates in milliseconds
    private const val MIN_TIME_BW_UPDATES: Long = 1000 * 5 * 1
  }
}
