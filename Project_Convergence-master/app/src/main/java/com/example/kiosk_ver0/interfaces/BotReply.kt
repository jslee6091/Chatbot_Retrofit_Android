package com.example.kiosk_ver0.interfaces

import com.google.cloud.dialogflow.v2.DetectIntentResponse

interface BotReply {
    fun callback(returnResponse: DetectIntentResponse?)
}