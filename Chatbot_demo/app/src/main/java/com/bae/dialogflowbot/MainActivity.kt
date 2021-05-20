package com.bae.dialogflowbot


import android.Manifest
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bae.dialogflowbot.adapters.ChatAdapter
import com.bae.dialogflowbot.helpers.SendMessageInBg
import com.bae.dialogflowbot.interfaces.BotReply
import com.bae.dialogflowbot.models.Message
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.*
import com.google.common.collect.Lists
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class MainActivity : AppCompatActivity(),OnInitListener, BotReply {
    var messageList: MutableList<Message> = ArrayList()
    val chatAdapter = ChatAdapter(messageList, this)

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val REQUEST_CODE = 1
    val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

    //dialogFlow
    private var sessionsClient: SessionsClient? = null
    private var sessionName: SessionName? = null
    private val uuid = UUID.randomUUID().toString()
    private val TAG = "mainactivity"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chatView.setAdapter(chatAdapter)

        tts = TextToSpeech(this, this)
        // permission 확인
        if (Build.VERSION.SDK_INT >= 23)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO), REQUEST_CODE)



        startSTT()

        btnSend.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val message = editMessage.getText().toString()
                if (!message.isEmpty()) {
                    messageList.add(Message(message, false))
                    editMessage.setText("")
                    sendMessageToBot(message)
                    Objects.requireNonNull(chatView.getAdapter()).notifyDataSetChanged()
                    Objects.requireNonNull(chatView.getLayoutManager())
                            ?.scrollToPosition(messageList.size - 1)
                } else {
                    Toast.makeText(this@MainActivity, "Please enter text!", Toast.LENGTH_SHORT).show()
                }
            }
        })
        setUpBot()

    }

    private fun setUpBot() {
        try {
            val stream = this.resources.openRawResource(R.raw.order)
            val credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"))
            val projectId = (credentials as ServiceAccountCredentials).projectId
            val settingsBuilder = SessionsSettings.newBuilder()
            val sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            sessionName = SessionName.of(projectId, uuid)
            Log.d(TAG, "projectId : $projectId")
        } catch (e: Exception) {
            Log.d(TAG, "setUpBot: " + e.message)
        }
    }

    private fun sendMessageToBot(message: String) {
        val input = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("ko")).build()
        //Log.d(TAG, "inputQuery : $input")
        SendMessageInBg(this, sessionName!!, sessionsClient!!, input).execute()
    }

    override fun callback(returnResponse: DetectIntentResponse?) {
        //Log.d(TAG, "returnresponce : $returnResponse")
        if (returnResponse != null) {
            val botReply = returnResponse.queryResult.fulfillmentText
            if (!botReply.isEmpty()) {
                messageList.add(Message(botReply, true))
                chatAdapter!!.notifyDataSetChanged()
                Objects.requireNonNull(chatView!!.layoutManager)!!.scrollToPosition(messageList.size - 1)
                speakOut(botReply)

            } else {
                Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "failed to connect!", Toast.LENGTH_SHORT).show()
        }
    }


    //TTS부분
    private fun speakOut(text: CharSequence) {
        tts!!.setPitch(0.6.toFloat())
        tts!!.setSpeechRate(2.0.toFloat())
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1")

    }
    public override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()

    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.KOREA)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported")
            } else {
            }
            tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    Log.i("TextToSpeech", "On Start")
                }

                override fun onDone(utteranceId: String) {
                    Log.i("TextToSpeech", "On Done")
                    GlobalScope.launch(Main){
                        startSTT()
                    }

                }

                override fun onError(utteranceId: String) {
                    Log.i("TextToSpeech", "On Error")
                }
            })


        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }


    // RecognitionListener 사용한 예제
    private  fun startSTT() {


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
            startListening(speechRecognizerIntent)
            Log.d("ff","startlistening")
        }

    }

    private val recognitionListener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) = Toast.makeText(this@MainActivity, "음성인식 시작", Toast.LENGTH_SHORT).show()

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {

            val message: String
            when (error) {
                SpeechRecognizer.ERROR_AUDIO -> message = "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> message = "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> message = "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> message = "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> message = "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> message = "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> message = "RecognitionService busy"
                SpeechRecognizer.ERROR_SERVER -> message = "Error from server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> message = "No speech input"
                else -> message = "Didn't understand, please try again."
            }
            if(error == SpeechRecognizer.ERROR_NO_MATCH || error ==SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                speechRecognizer?.startListening(speechRecognizerIntent)
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()


        }

        override fun onResults(results: Bundle) {
            var str = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!![0]
            editMessage.setText(str)
            btnSend.callOnClick()
        }
    }



// 2. onActivityResult를 사용한 STT
//    inner class VoiceTask : AsyncTask<String?, Int?, String?>() {
//        var str: String? = null
//        protected override fun doInBackground(vararg params: String?): String? {
//            // TODO Auto-generated method stub
//            try {
//                voice
//            } catch (e: Exception) {
//                // TODO: handle exception
//            }
//            return str
//        }
//
//        override fun onPostExecute(result: String?) {
//            try {
//            } catch (e: Exception) {
//                Log.d("onActivityResult", "getImageURL exception")
//            }
//        }
//
//
//    }
//
//    private val voice: Unit
//        private get() {
//            val intent = Intent()
//            intent.action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//            val language = "ko-KR"
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
//            startActivityForResult(intent, 2)
//        }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        // TODO Auto-generated method stub
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.e("ff",""+resultCode)
//        if (resultCode == RESULT_OK) {
//            val results = data
//                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
//            val str = results?.get(0)
//            Toast.makeText(baseContext, str, Toast.LENGTH_SHORT).show()
//
//            editMessage.setText(str)
//            btnSend.callOnClick()
//        }
//    }



}



