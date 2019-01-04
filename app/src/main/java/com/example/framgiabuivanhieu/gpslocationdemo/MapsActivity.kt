package com.example.framgiabuivanhieu.gpslocationdemo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.example.framgiabuivanhieu.gpslocationdemo.R.string.google_maps_key
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap
import kotlin.collections.ArrayList

@Suppress("NAME_SHADOWING")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

  private lateinit var mMap: GoogleMap
  private lateinit var mListPoints: ArrayList<LatLng>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager
        .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
    mListPoints = ArrayList()
  }

  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  @SuppressLint("MissingPermission")
  override fun onMapReady(googleMap: GoogleMap) {
    val latitude = intent.extras!!.get("LATITUDE") as Double
    val longitude = intent.extras!!.get("LONGITUDE") as Double
    val location = intent.extras!!.get("LOCATION") as String

    mMap = googleMap
    // Add a marker in Sydney and move the camera
    val origin = LatLng(latitude, longitude)
    mMap.addMarker(MarkerOptions().position(origin).title(location))
    mMap.moveCamera(CameraUpdateFactory.newLatLng(origin))
    mMap.uiSettings.isZoomControlsEnabled = true
    mMap.isMyLocationEnabled = true


    mMap.setOnMapClickListener { latLng: LatLng? ->
      //Reset marker when already 2
      if (mListPoints.size == 2) {
        mListPoints.clear()
        mMap.clear()
      }
      // Save first point select
      mListPoints.add(latLng!!)
      //Create marker
      var makerOptions = MarkerOptions()
      makerOptions.position(latLng)

      if (mListPoints.size == 1) {
        //Add first marker to the map
        makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
      } else {
        //Add second marker to the map
        makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
      }
      mMap.addMarker(makerOptions)

      if (mListPoints.size == 2) {
        //Create the URL to get request from first maker to second maker
        var url: String = getRequestUrl(mListPoints[0], mListPoints[1])
        TaskRequestDirection().execute(url)
      }
    }

  }

  private fun getRequestUrl(origin: LatLng, dest: LatLng): String {
    //Value of origin
    var origin = "origin=" + origin.latitude + "." + origin.longitude
    // Value of destination
    var destination: String = "destination=" + dest.latitude + "." + dest.longitude
    // Set value enable the sensor
    var sensor = "sensor=false"
    //Mode for find direction
    var mode = "mode=driving"
    //Key api
    var key = "client="+google_maps_key.toString()
    //Build the full param
    var param = "$origin&$destination&$sensor&$mode&$key"
    //Out put format
    var output = "json"
    //Create url to request
    var url = "http://maps.googleapis.com/maps/api/directions/$output?$param"
    return url
  }

  private fun requestDirection(reqUrl: String): String {
    var responseString = ""
    var inputStream: InputStream? = null
    var httpURLConnection: HttpURLConnection? = null
    try {
      var url = URL(reqUrl)
      httpURLConnection = url.openConnection() as HttpURLConnection?
      httpURLConnection!!.connect()

      //Get response result
      inputStream = httpURLConnection.inputStream
      var inputStreamReader = InputStreamReader(inputStream)
      var bufferedReader = BufferedReader(inputStreamReader)
      var stringBuffer = StringBuffer()
      var line: String
      while (bufferedReader.readLine() != null) {
        line = bufferedReader.readLine()
        stringBuffer.append(line)
      }
      responseString = stringBuffer.toString()
      bufferedReader.close()
      inputStreamReader.close()
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      inputStream?.close()
      httpURLConnection?.disconnect()
    }
    return responseString
  }

  inner class TaskRequestDirection : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg params: String?): String {
      var responseString = ""
      try {
        responseString = requestDirection(params[0]!!)

      } catch (e: Exception) {
        e.printStackTrace()
      }
      return responseString
    }

    override fun onPostExecute(result: String?) {
      super.onPostExecute(result)
      //Parse json here
      TaskParser().execute(result)
    }

  }

  inner class TaskParser : AsyncTask<String, Void, List<List<HashMap<String, String>>>>() {
    override fun doInBackground(vararg params: String?): List<List<HashMap<String, String>>> {
      var jsonObjects: JSONObject
      var routes: List<List<HashMap<String, String>>>? = null

      try {
        jsonObjects = JSONObject(params[0])
        var dataParser = DataParser()
        routes = dataParser.parse(jsonObjects)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      return routes!!
    }

    override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
      //Get list route and display it into the map
      var points: ArrayList<LatLng>
      var polylineOptions: PolylineOptions? = null
      for (path: List<HashMap<String, String>> in result!!) {
        points = ArrayList()
        polylineOptions = PolylineOptions()
        for (point: HashMap<String, String> in path) {
          var latitude: Double = point["lat"]!!.toDouble()
          var longitude: Double = point["lng"]!!.toDouble()
          points.add(LatLng(latitude, longitude))
        }
        polylineOptions.addAll(points)
        polylineOptions.width(15F)
        polylineOptions.color(Color.BLACK)
        polylineOptions.geodesic(true)
      }
      if (polylineOptions != null) {
        mMap.addPolyline(polylineOptions)
      } else {
        Toast.makeText(applicationContext, "Direction not found", Toast.LENGTH_SHORT).show()
      }
    }
  }
}
