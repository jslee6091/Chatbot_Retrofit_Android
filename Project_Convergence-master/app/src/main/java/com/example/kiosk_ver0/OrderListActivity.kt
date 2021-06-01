package com.example.kiosk_ver0

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.JsonObject
import com.kakao.sdk.user.UserApiClient
import kotlinx.android.synthetic.main.activity_order_list.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import org.apache.commons.lang3.ObjectUtils

class OrderListActivity : AppCompatActivity() {

//    var items = mutableListOf<OrderListData>()
//    init {
//        items.plusAssign(OrderListData("주문 번호: 1", "메뉴: 빅맥", "조리 중"))
//    }

    private val SET_PERMISSION = 101

    fun SendInfo(phonenumber : String){
        lateinit var mRetrofit : Retrofit // 사용할 Retrofit Object
        lateinit var mRetrofitPOST: RetrofitAPI // Retrofit POST object
        lateinit var mDataTransfer: Call<JsonObject> // POST Json Data


        // 서버에서 주문목록+주문번호+상태 가져오기
        val order = intent.getSerializableExtra("order") as ArrayList<OrderListData>

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
            }
        })

        Log.i("OrderListActivity", order.toString())

        // 회원 정보와 주문 정보 서버에 보내기
        UserApiClient.instance.me { user, error ->
            var text = "${user?.id}"

            fun callRetrofitPOST(){

                // POST 방식으로 서버에 전송
                for(i in 0..order.size - 1) {
                    mDataTransfer = mRetrofitPOST.dataTransfer(
                        text, "처리중","${order.get(i).quantity.toString()}",
                        "${order.get(i).name}", phonenumber)

                    mDataTransfer.enqueue(mRetrofitPOSTCallback)
                }
            }

            callRetrofitPOST()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == SET_PERMISSION){ // 권한 요청 응답이 왔을 때 실행
            // 권한을 허용한 경우 - 이때 웹으로 데이터 전송
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "권한 허가 받았습니다.", Toast.LENGTH_LONG).show()

                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                var telephone = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                var phonenum = telephone.line1Number

                SendInfo(phonenum)
            }
            // 권한을 거절한 경우
            else{
                // 권한 요청 거절 종류 확인
                var is_permitted = ActivityCompat.shouldShowRequestPermissionRationale(this@OrderListActivity, Manifest.permission.READ_PHONE_STATE)

                if (is_permitted){ // 거부를 선택한 경우
                    Toast.makeText(this, "권한 요청을 승인해야합니다.", Toast.LENGTH_LONG).show()

                    // 1초 뒤 다시 권한 요청
                    var handler = Handler()
                    handler.postDelayed({
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), SET_PERMISSION)
                    },1000)

                }
                else{ // 거부 및 다시 묻지 않기를 선택한 경우 - 수동으로 설정
                    Toast.makeText(this, "권한 요청을 수동으로 승인 후 다시 주문 해야합니다.\n설정 -> 앱 -> 앱 정보 -> 앱 선택 -> 전화 권한 허용", Toast.LENGTH_LONG).show()

                    // 1초 뒤 결제 페이지로 이동
                    var handler = Handler()
                    handler.postDelayed({
                        var intent = Intent(this,CartListActivity::class.java)
                        startActivity(intent)
                        finish()

                    },1000)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)

        // 서버에서 주문목록+주문번호+상태 가져오기
        val order = intent.getSerializableExtra("order") as ArrayList<OrderListData>


        // telephone number 얻는 권한 없을 때 - 허가를 받아야 함
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this@OrderListActivity, arrayOf(Manifest.permission.READ_PHONE_STATE), SET_PERMISSION)
        }
        else{ //telephone number 얻는 권한 있을 때 - 웹으로 데이터 전송
            var telephone = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            var phonenum = telephone.line1Number

            SendInfo(phonenum)
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