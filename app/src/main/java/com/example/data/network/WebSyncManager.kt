package com.example.data.network

import android.util.Log
import com.example.data.ArsipPenjualanEntity
import com.example.data.MenuItemEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSyncManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchDaftarMenu(baseUrl: String): List<MenuItemEntity>? = withContext(Dispatchers.IO) {
        try {
            val url = if (baseUrl.contains("?")) "$baseUrl&action=getDaftarMenu" else "$baseUrl?action=getDaftarMenu"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                
                // Parse JSON array
                val jsonArray = JSONArray(bodyString)
                val list = mutableListOf<MenuItemEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val nama = obj.getString("nama")
                    val harga = obj.optDouble("harga", 0.0)
                    list.add(MenuItemEntity(nama = nama, harga = harga))
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("WebSyncManager", "Error fetching menu", e)
            return@withContext null
        }
    }

    suspend fun fetchArsipSebulan(baseUrl: String): List<ArsipPenjualanEntity>? = withContext(Dispatchers.IO) {
        try {
            val url = if (baseUrl.contains("?")) "$baseUrl&action=getArsipSebulan" else "$baseUrl?action=getArsipSebulan"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                
                val jsonArray = JSONArray(bodyString)
                val list = mutableListOf<ArsipPenjualanEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ArsipPenjualanEntity(
                            tanggal = obj.optString("tanggal", ""),
                            nota = obj.optString("nota", ""),
                            pembeli = obj.optString("pembeli", ""),
                            menu = obj.optString("menu", ""),
                            harga = obj.optDouble("harga", 0.0),
                            qty = obj.optInt("qty", 0),
                            total = obj.optDouble("total", 0.0),
                            isSynced = true
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("WebSyncManager", "Error fetching arsip", e)
            return@withContext null
        }
    }

    suspend fun uploadTransactions(baseUrl: String, dataPesanan: List<ArsipPenjualanEntity>, nota: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
            payload.put("action", "simpanTransaksi")
            payload.put("nomorNotaDariClient", nota)

            val pesananArray = JSONArray()
            dataPesanan.forEach { item ->
                val pObj = JSONObject()
                pObj.put("pembeli", item.pembeli)
                pObj.put("nama", item.menu)
                pObj.put("harga", item.harga)
                pObj.put("qty", item.qty)
                pObj.put("total", item.total)
                pesananArray.put(pObj)
            }
            payload.put("dataPesanan", pesananArray)

            val requestBody = payload.toString().toRequestBody(mediaTypeJson)
            val request = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val bodyString = response.body?.string() ?: return@withContext false
                val resObj = JSONObject(bodyString)
                return@withContext resObj.optString("status") == "Sukses"
            }
        } catch (e: Exception) {
            Log.e("WebSyncManager", "Error uploading transactions", e)
            return@withContext false
        }
    }
}
