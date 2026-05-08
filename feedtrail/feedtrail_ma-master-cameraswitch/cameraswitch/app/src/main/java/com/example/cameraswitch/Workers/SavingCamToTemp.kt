package com.example.cameraswitch.Workers

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cameraswitch.UtilsAndCons.ImageToTempUtil
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationAvailability
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.intellij.lang.annotations.Language
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SavingCamToTemp(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
    val metadataJson = inputData.getString("metadata") ?: "{}"
    val typepic= inputData.getString("camera_type") ?: ""
    private var metadata = JSONObject(metadataJson).toMap().toMutableMap()
    var locationDataDone:Boolean=false
    var metadataDone :Boolean=false
    val imagePath = inputData.getString("image_path") ?: null

    override suspend fun doWork(): Result {
       // val imagePath = inputData.getString("image_path") ?: return Result.failure()

        return try {
          //  val bitmap = BitmapFactory.decodeFile(imagePath)
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(imagePath)
            }


            if (bitmap != null) {
                // Save the image to the temp folder initially

             //   val tempFile = ImageToTempUtil.saveImageToTempFolder(applicationContext, bitmap, metadata, typepic)
             //   val tempFile = ImageToTempUtil.saveImageToTempFolder(applicationContext, bitmap, typepic)

             //   val tempFile = saveCamPicture(applicationContext,bitmap,typepic)
                val tempFile = withContext(Dispatchers.IO) {
                    saveCamPicture(applicationContext, bitmap, typepic)
                }
                val location = getAccurateLocation()
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    metadata["location"] = "Lat: $latitude, Lon: $longitude"
                    fetchAddressFromLocation(location) { address ->
                        if (address != null) {
                            metadata["address"] = address
                            if (imagePath != null) {
                                saveJsonToFile(address,imagePath)
                            }
                        }
                        fetchOSMData(latitude, longitude)
                        Log.d("Metadata", "Updated Metadata: $metadata")
                    }
                }
                Log.d("Metadata", "the metadata sent : ${metadata.toString()}")
                Log.d("SaveImageWorkerIm", "Image saved to temp folder: ${tempFile?.absolutePath}, ${metadata.toString()}")
                if (tempFile != null) {
                 //   Log.d("SaveImageWorkerIm", "Image saved to temp folder: ${tempFile.absolutePath}, ${metadata.toString()}")

                    // Attempt to fetch location data
                    //val location = getAccurateLocation()
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                      //  metadata["location"] = "Lat: $latitude, Lon: $longitude"

                        fetchAddressFromLocation(location) { address ->
                            if (address != null) {
                                metadata["address"] = address
                            }
                            fetchOSMData(latitude, longitude)
                            Log.d("Metadata", "Updated Metadata: $metadata")
                        }
                    }

                    Result.success()
                } else {
                    Log.e("SaveImageWorkerIm", "Failed to save image to temp folder")
                    Result.failure()
                }
           } else {
                Log.e("SaveImageWorkerIm", "Failed to decode image: $imagePath")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("SaveImageWorkerIm", "Error saving image to temp folder", e)
            Result.failure()
        }
    }
  /*  private suspend fun getAccurateLocation(): Location? {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }

       return suspendCoroutine { continuation ->
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    continuation.resume(locationResult.lastLocation)
                    fusedLocationClient.removeLocationUpdates(this) // Stop further updates
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (!locationAvailability.isLocationAvailable) {
                        continuation.resume(null)
                    }
                }
            }

            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@suspendCoroutine
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }*/
      private fun saveJsonToFile(jsonData: String, imagePath: String): File? {
          return try {
              var sessionManager = SessionManager(applicationContext)

              val sessionCounter = sessionManager.getSessionCounter()
              // Construct the directory for the JSON file
              val sessionDir = File(applicationContext.filesDir, "temp_images/session_$sessionCounter")
              if (!sessionDir.exists()) sessionDir.mkdirs()

              // Construct the JSON file path with the same base name as the image
              val imageFile = File(imagePath)
              val jsonFile = File(sessionDir, imageFile.nameWithoutExtension + ".json")

              // Write JSON data to the file
              jsonFile.writeText(jsonData)

              Log.d("SaveImageWorker", "JSON saved successfully to: ${jsonFile.absolutePath}")
              jsonFile
          } catch (e: Exception) {
              Log.e("SaveImageWorker", "Failed to save JSON file", e)
              null
          }
      }


    private suspend fun getAccurateLocation(): Location? {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }

        val locationResultDeferred = CompletableDeferred<Location?>()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Complete the deferred with the last known location
                locationResultDeferred.complete(locationResult.lastLocation)
                if (imagePath != null) {
                    saveJsonToFile(locationResult.lastLocation.toString(), imagePath)
                }
                fusedLocationClient.removeLocationUpdates(this) // Stop further updates
            }

        /*    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    // If location is unavailable, complete with null#
                    locationResultDeferred.complete(null)
                    fusedLocationClient.removeLocationUpdates(this)
                   // locationResultDeferred.complete(null)
                }
            }*/
        }

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            locationResultDeferred.complete(null)

        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Wait for the result and return it
        return locationResultDeferred.await()
    }
 /* private suspend fun getAccurateLocation(): Location? {
      var locationReceived = false  // Track if the location has been received
      return suspendCoroutine { continuation ->
          val locationRequest = LocationRequest.create().apply {
              priority = LocationRequest.PRIORITY_HIGH_ACCURACY
              interval = 0
              fastestInterval = 0
              numUpdates = 1
          }

          val locationCallback = object : LocationCallback() {
              override fun onLocationResult(locationResult: LocationResult) {
                  if (!locationReceived) {
                      // Ensure continuation is resumed only once
                      locationReceived = true
                   //   continuation.resume(locationResult.lastLocation)
                      if (imagePath != null) {
                          saveJsonToFile(locationResult.lastLocation.toString(), imagePath)
                      }
                      // Stop further location updates after receiving a valid location
                      fusedLocationClient.removeLocationUpdates(this)
                  }
              }

              override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                  if (!locationAvailability.isLocationAvailable && !locationReceived) {
                      // If no location is available, resume with null, but only once
                      locationReceived = true
                      continuation.resume(null)

                      // Remove location updates as no location is available
                      fusedLocationClient.removeLocationUpdates(this)
                  }
              }
          }

          // Check permissions before requesting location updates
          if (ActivityCompat.checkSelfPermission(
                  applicationContext,
                  Manifest.permission.ACCESS_FINE_LOCATION
              ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                  applicationContext,
                  Manifest.permission.ACCESS_COARSE_LOCATION
              ) != PackageManager.PERMISSION_GRANTED
          ) {
              locationReceived = true  // Mark as received to prevent multiple resuming
              continuation.resume(null) // Resume with null if permissions are missing
              return@suspendCoroutine
           //   return
          }

          // Request location updates
          fusedLocationClient.requestLocationUpdates(
              locationRequest,
              locationCallback,
              Looper.getMainLooper()
          )

          // Timeout to prevent indefinite waiting
          GlobalScope.launch(Dispatchers.IO) {
              delay(5000)  // Timeout after 5 seconds
              if (!locationReceived) {
                  locationReceived = true
                 // continuation.resume(null) // Timeout reached, resume with null
                  fusedLocationClient.removeLocationUpdates(locationCallback) // Clean up
              }
          }
      }

  }*/




    private fun fetchOSMData(latitude: Double, longitude: Double) {
        val smallBoxSize = 0.0001  // Represents roughly +/-10 meters depending on the location
        val minLat = latitude - smallBoxSize
        val maxLat = latitude + smallBoxSize
        val minLon = longitude - smallBoxSize
        val maxLon = longitude + smallBoxSize

        val query = """
             [out:json];
             (
                node["amenity"](bbox:$minLat,$minLon,$maxLat,$maxLon);
                way["amenity"](bbox:$minLat,$minLon,$maxLat,$maxLon);
                relation["amenity"](bbox:$minLat,$minLon,$maxLat,$maxLon);
               
             );
             out body;
             >;
             out skel qt;
          """.trimIndent()

        val encodedQuery = Uri.encode(query)
        val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

        OkHttpClient().newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Failed to fetch OSM data", e)

            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("HTTP", "Failed to fetch OSM data: ${response.message} URL was: $url")

                        return
                    }
                    val responseData = it.body?.string() ?: ""
                    val completeData = "OSM Data: $responseData"
                    Log.d("Metadata", "Updated OSM")

                 /*   metadata = mutableMapOf<String, String>().apply {
                        put("OSMData", responseData.toString()) // Example of dynamic address
                    }*/
                    findNearestAmenity(responseData, latitude, longitude)
                }
            }
        })
    }
    private fun findNearestAmenity(data: String, myLat: Double, myLon: Double) {
        val json = JSONObject(data)
        val elements = json.getJSONArray("elements")
        var nearestNode: JSONObject? = null
        var minDistance = Double.MAX_VALUE

        for (i in 0 until elements.length()) {
            val node = elements.getJSONObject(i)
            if (node.has("lat") && node.has("lon")) {
                val lat = node.getDouble("lat")
                val lon = node.getDouble("lon")
                val distance = calculateDistance(myLat, myLon, lat, lon)
                // val distance = sqrt((lat - myLat).pow(2) + (lon - myLon).pow(2))
                if (distance < minDistance) {
                    minDistance = distance
                    nearestNode = node
                }
            }
        }

        nearestNode?.let {
        /*    val amenity = if (it.has("amenity")) it.getString("amenity") else "No amenity available"
            val highway = if (it.has("highway")) it.getString("highway") else "No highway available"
            val nearestAmenityInfo = "Nearest Amenity: $amenity, Location: ${it.getDouble("lat")}, ${it.getDouble("lon")}"


            (metadata as MutableMap<String, String>)["nearestAmenity"] = amenity
            (metadata as MutableMap<String, String>)["nearestHighway"] = highway
            (metadata as MutableMap<String, String>)["nearestNode"] = nearestNode.toString()*/
          //  (metadata as MutableMap<String, String>)["nearestNodeCoordinates"] = "Lat: ${it.getDouble("lat")}, Lon: ${it.getDouble("lon")}"

           /* val metadata = mapOf(
                "nearestAmenity" to amenity.toString(),
                "nearestHighway" to highway.toString(),
                "nearestlat" to  it.getDouble("lat").toString(),
                "nearestNode" to nearestNode

               // "nearestlon" to it.getDouble("lon").toString()
            )*/
            val highway = if (it.has("highway")) it.getString("highway") else "No highway available"
          //  val amenity = it.optString("amenity", "Unknown")
            val amenity = it.optString("tags", "No amenity found")
            val building = it.optString("building", "No building")
            val landUse = it.optString("landuse", "No land use info")
            val waterway = it.optString("waterway", "No waterway info")
            val leisure = it.optString("leisure", "No sports facility")


            metadata["nearestAmenity"] = amenity
            metadata["nearesHighway"] = highway
            metadata["nearestBuilding"] = building
            metadata["nearestlandUse"] = landUse
            metadata["nearestWaterway"] = waterway
            metadata["neareestleisure"] = leisure


         //   metadata["nearestNode"] = "Lat: ${it.getDouble("lat")}, Lon: ${it.getDouble("lon")}"
            Log.d("Metadata", "Nearest Amenity: $amenity, Highway: $highway, Building : $building, landuse: $landUse, waterway: $waterway, leisure; $leisure")
            if (imagePath != null) {
                saveJsonToFile(metadata.toString(), imagePath)
            }
                metadataDone=true
       //     Log.d("Metadata", "in findnearestamnisty ; $metadata")
        } ?: run {
            Log.d("Metadata", "No nearest amenity found or no nodes with valid coordinates")
            metadataDone=true

        }
       // val tempFile = ImageToTempUtil.saveImageToTempFolder(applicationContext, bitmap, metadata, typepic)
        //Log.d("SaveImageWorkerIm", "Image saved to temp folder: ${tempFile!!.absolutePath}, ${metadata.toString()}")

    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0  // Radius of the earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c * 1000  // Distance in meters
    }
     fun fetchAddressFromLocation(location: Location, onAddressFetched: (String?) -> Unit) {


        val latitude = location.latitude
        val longitude = location.longitude
        val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude"

        // OkHttpClient and Request creation
        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Failed to fetch address data", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData)
                    val address = jsonObject.getJSONObject("address").getString("road")
                    onAddressFetched(address)
                    if (imagePath != null) {
                        saveJsonToFile(address, imagePath)
                    }
                   fetchOSMData(latitude,longitude)
                }
            }
        })

    }
    private fun JSONObject.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        keys().forEach { key -> map[key] = getString(key) }
        return map
    }



    private suspend fun saveCamPicture(
        context: Context,
        bitmap: android.graphics.Bitmap,
        typepic: String
    ): File? {
         withContext(Dispatchers.IO) {
            val sessionCounter = try {
                SessionManager(context).getSessionCounter()
            } catch (e: Exception) {
                Log.e("SaveImageWorker", "Failed to get session counter", e)
                0 // Fallback session counter
            }

            val baseDir = File(context.filesDir, "temp_images/session_$sessionCounter")
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                Log.e(
                    "SaveImageWorker",
                    "Failed to create session directory: ${baseDir.absolutePath}"
                )
                return@withContext null
            }

            //   val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val currentTime = System.currentTimeMillis()
            //   val formattedDate = dateFormat.format(Date(currentTime))
            val formattedTime = timeFormat.format(Date(currentTime))

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val formattedDateTime = dateFormat.format(Date())
            val pId = SharedPreferencesUtils.getParticipantId(context)

            val fileName = "${typepic}_p(${pId})_{$sessionCounter}_${formattedDateTime}"


            val tempFile = File(baseDir, fileName)
             return@withContext try {
                 tempFile.outputStream().use { fos ->
                     //  stream.toByteArray().size / 1024 > maxSizeInKB
                     if (typepic == "back") {
                         bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 5, fos)

                     } else {
                         bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 13, fos)

                     }
                 }
                 Log.d("SaveImageWorker", "Image successfully saved to: ${tempFile.absolutePath}")
                 tempFile
             } catch (e: Exception) {
                 Log.e(
                     "SaveImageWorker",
                     "Error saving screenshot to file: ${tempFile.absolutePath}",
                     e
                 )
                 null
             }
        }
        return null
    }
}

