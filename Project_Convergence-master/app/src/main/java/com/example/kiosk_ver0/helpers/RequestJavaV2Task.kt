package com.example.kiosk_ver0.helpers

import android.content.ContentValues
import android.os.AsyncTask
import android.util.Log
import com.example.kiosk_ver0.interfaces.BotReply
import com.google.cloud.dialogflow.v2.*

class RequestJavaV2Task(private val mInterface: BotReply, private val session: SessionName, private val sessionsClient: SessionsClient,
                        private val queryInput: QueryInput) : AsyncTask<Void?, Void?, DetectIntentResponse?>() {
    protected override  fun doInBackground(vararg params: Void?): DetectIntentResponse? {
        try {
            val detectIntentRequest = DetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .build()
            return sessionsClient.detectIntent(detectIntentRequest)
        } catch (e: Exception) {
            Log.d(ContentValues.TAG, "doInBackground: " + e.message)
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(response: DetectIntentResponse?) {
        mInterface.callback(response)
    }


}