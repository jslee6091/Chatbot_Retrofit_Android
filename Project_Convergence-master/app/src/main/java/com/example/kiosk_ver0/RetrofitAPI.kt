package com.example.kiosk_ver0

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface RetrofitAPI {
    @FormUrlEncoded
    @POST("/android")
    fun dataTransfer(
        @Field("userID") userID : String,
        @Field("state") state : String,
        @Field("quantity") quantity : String,
        @Field("menu") menu : String,
        @Field("phonenum") phonenum : String
    ) : Call<JsonObject>
}