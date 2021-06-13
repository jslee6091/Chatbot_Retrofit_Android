package com.example.kiosk_ver0

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import com.kakao.sdk.user.UserApiClient
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        UserApiClient.instance.me { user, error ->
            var text = "회원번호: ${user?.id}"
            Toast.makeText(this,text,Toast.LENGTH_LONG).show()
        }

        kakao_logout_button.setOnClickListener {
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    Toast.makeText(this, "로그아웃 실패 $error", Toast.LENGTH_SHORT).show()
                }else {
                    Toast.makeText(this, "로그아웃 성공", Toast.LENGTH_SHORT).show()
                }
                val intent = Intent(this, MainActivity::class.java)
                // Logout 이후 뒤로가기 누를 시 로그인 된 상태인 이전 화면으로 돌아가는 것을 방지
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }

        btnOrder.setOnClickListener {
            val nextIntent = Intent(this, MenuActivity::class.java)
            startActivity(nextIntent)
        }

        btnVoiceOrder.setOnClickListener {
            val nextIntent = Intent(this, VoiceOrderActivity::class.java)
            startActivity(nextIntent)
        }

        btnOrderList.setOnClickListener {
            val nextIntent = Intent(this, OrderListActivity::class.java)
            startActivity(nextIntent)
        }
    }
}