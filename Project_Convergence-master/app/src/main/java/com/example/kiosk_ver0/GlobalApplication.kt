package com.example.kiosk_ver0

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Native App Key
        KakaoSdk.init(this, "8a051aace2d2d21d294403565a44e77b")
    }
}