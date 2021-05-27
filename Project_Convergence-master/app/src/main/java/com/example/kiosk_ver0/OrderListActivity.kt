package com.example.kiosk_ver0

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kakao.sdk.user.UserApiClient
import kotlinx.android.synthetic.main.activity_order_list.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OrderListActivity : AppCompatActivity() {

//    var items = mutableListOf<OrderListData>()
//    init {
//        items.plusAssign(OrderListData("주문 번호: 1", "메뉴: 빅맥", "조리 중"))
//    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)

        lateinit var mRetrofit : Retrofit // 사용할 Retrofit Object
        lateinit var mRetrofitPOST: RetrofitAPI // Retrofit POST object
        lateinit var mDataTransfer: Call<JsonObject> // POST Json Data

        fun setRetrofit(){
            // Retrofit 에서 가져올 url 설정 후 세팅
            mRetrofit = Retrofit
                .Builder()
                .baseUrl(getString(R.string.baseUrl))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            //POST Retrofit
            mRetrofitPOST = mRetrofit.create(RetrofitAPI::class.java)
        }

        setRetrofit()

        var mRetrofitPOSTCallback = (object : retrofit2.Callback<JsonObject>{
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.e("retrofit Failure", t.toString())
            }

            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val result = response.body()
                Log.d("retrofit Success", result.toString())

                // POST 방식으로 전송이 잘 되었는지 확인하기 위한 코드
                val dataParsed1 = Gson().fromJson(result, DataModel.UserInfo::class.java)
                val dataParsed2 = Gson().fromJson(result, DataModel.StateInfo::class.java)
                val dataParsed3 = Gson().fromJson(result, DataModel.QuantityInfo::class.java)
                val dataParsed4 = Gson().fromJson(result, DataModel.MenuInfo::class.java)

                Log.d("UserID ", dataParsed1.userID)
                Log.d("State ", dataParsed2.state)
                Log.d("Quantity ", dataParsed3.quantity)
                Log.d("Menu ", dataParsed4.menu)

            }
        })

        // 서버에서 주문목록+주문번호+상태 가져오기
        val order = intent.getSerializableExtra("order") as ArrayList<OrderListData>
        Log.i("OrderListActivity", order.toString())


        // 회원 정보와 주문 정보 서버에 보내기
        UserApiClient.instance.me { user, error ->
            var text = "${user?.id}"
            fun callRetrofitPOST(){

                // POST 방식으로 서버에 전송
                for(i in 0..order.size - 1) {
                    mDataTransfer = mRetrofitPOST.dataTransfer(
                        text, "처리중",
                        "${order.get(i).quantity.toString()}","${order.get(i).name}")

                    mDataTransfer.enqueue(mRetrofitPOSTCallback)
                }
            }

            callRetrofitPOST()
        }

        rv_order_list.adapter = OrderListAdapter(order)
        rv_order_list.layoutManager = GridLayoutManager(this,1)

        var home = findViewById<TextView>(R.id.btnHome)

        home.setOnClickListener {
            val nextIntent = Intent(this, HomeActivity::class.java)
            startActivity(nextIntent)
        }
    }

}