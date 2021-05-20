package com.bae.dialogflowbot.interfaces

import com.google.cloud.dialogflow.v2.DetectIntentResponse

interface BotReply {
    fun callback(returnResponse: DetectIntentResponse?)
}