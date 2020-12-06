package com.tyagiabhinav.dialogflowchat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.tyagiabhinav.dialogflowchat.utility.AudioZ;
import com.tyagiabhinav.dialogflowchat.utility.MainActivity2;
import com.tyagiabhinav.dialogflowchat.utility.SelectDeviceActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import ai.api.AIServiceContext;
import ai.api.android.AIDataService;
import ai.api.model.AIRequest;

// Android client for older V1 --- recommend not to use this

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, RecognitionListener {

    public static final Integer RecordAudioRequestCode = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int USER = 10001;
    private static final int BOT = 10002;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    TextToSpeech textToSpeech;
    String languagePref = "en-US";
    boolean isStart = false;
    private String uuid = UUID.randomUUID().toString();
    private LinearLayout chatLayout;
    private EditText queryEditText;
    // Android client for older V1 --- recommend not to use this
    private AIRequest aiRequest;
    private AIDataService aiDataService;
    private AIServiceContext customAIServiceContext;
    // Java V2
    private SessionsClient sessionsClient;
    private SessionName session;
    private ImageView sendBtn;
    private SpeechRecognizer speechRecognizer;
    private Button micBtn;
    private Intent speach_intent;
    private boolean firstLaunch = true;
    private String deviceName = null;
    private String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AudioZ.unmuteAudio(this);


        /*--------TTS-----------*/

        //Initializations part


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.forLanguageTag(languagePref));
        initIntent();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
        }

        final ScrollView scrollview = findViewById(R.id.chatScrollView);
        scrollview.post(() -> scrollview.fullScroll(ScrollView.FOCUS_DOWN));

        chatLayout = findViewById(R.id.chatLayout);
        micBtn = (Button) findViewById(R.id.startBtn);
        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(this::sendMessage);
        micBtn.setOnClickListener(this::recognizeAudio);

        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent);// Activity is started with requestCode 2
            }
        });

        /*------------Bot for Bluetooth--------------*/


        final ImageView buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new MainActivity.CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }

           /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handleMessage: " + msg.toString());
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                Log.d(TAG, "handleMessage: Connected");
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                micBtn.setVisibility(View.VISIBLE);
                                queryEditText.setHint(getString(R.string.tap_to_speak));
                                break;
                            case -1:
                                toolbar.setSubtitle(getString(R.string.faild_bluetooth_con));
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                micBtn.setVisibility(View.GONE);
                                Log.d(TAG, "handleMessage: Failed to connect");
                                queryEditText.setHint(getString(R.string.bluetooth_connections_hint));
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        Log.d(TAG, "handleMessageDDD: " + arduinoMsg);
                        switch (arduinoMsg.toLowerCase()) {
//                            case "led is turned on":
//                                imageView.setImageResource(R.drawable.light_on);
//                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
//                                break;
//                            case "led is turned off":
//                                imageView.setImageResource(R.drawable.light_off);
//                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
//                                break;
                        }
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });


        findViewById(R.id.title_of_app).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
            }
        });


        queryEditText = findViewById(R.id.queryEditText);
        queryEditText.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        sendMessage(sendBtn);
                        return true;
                    default:
                        break;
                }
            }
            return false;
        });


        // Java V2
        initV2Chatbot();

    }

    private void initIntent() {
        speach_intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speach_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languagePref);
        speach_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languagePref);
        speach_intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languagePref);
        speach_intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 30000);
    }

    private void recognizeAudio(View view) {
        if (isStart == false) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(this);
            speechRecognizer.startListening(speach_intent);
            micBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            micBtn.setText(getString(R.string.stop));
            isStart = true;

            if (textToSpeech != null) {
                textToSpeech = new TextToSpeech(this, this);
                textToSpeech.setLanguage(Locale.forLanguageTag(languagePref));
                initIntent();
            }


        } else {
            micBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            speechRecognizer.stopListening();
            micBtn.setText(getString(R.string.start));
            isStart = false;

            queryEditText.setHint(getString(R.string.tap_to_speak));

            speechRecognizer.destroy();

            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
        }

    }

//    private boolean recognizeAudio(View view, MotionEvent motionEvent) {
//        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//            speechRecognizer.stopListening();
//            if (micButton.isPlaying()) micButton.stop();
//        }
//        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//            micButton.play();
//            speechRecognizer.startListening(speach_intent);
//        }
//        return false;
//    }


    private void initV2Chatbot() {
        try {
            InputStream stream = getResources().openRawResource(R.raw.test_agent_credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            session = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(View view) {
        String msg = queryEditText.getText().toString();
        if (msg.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, getString(R.string.enter_your_query), Toast.LENGTH_LONG).show();
        } else {
            showTextView(msg, USER);
            queryEditText.setText("");
            QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en-US")).build();
            new RequestJavaV2Task(MainActivity.this, session, sessionsClient, queryInput).execute();

        }
    }


    public void callbackV2(DetectIntentResponse response) {
        if (response != null) {
            // process aiResponse here
            String botReply = response.getQueryResult().getFulfillmentText();
            Log.d(TAG, "V2 Bot Reply: " + botReply);

            if (TextUtils.isEmpty(botReply) || botReply == null) {
                botReply = getString(R.string.i_didnot_find_anything);
                texttoSpeak(botReply);
                showTextView(botReply, BOT);
            } else {
                texttoSpeak(botReply);
                showTextView(botReply, BOT);
            }

            queryEditText.setHint(getString(R.string.tap_to_speak));

            try {
                String intent_action = response.getQueryResult().getIntent().getDisplayName();
                intentAction(intent_action);
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            Log.d(TAG, "Bot Reply: Null");
            showTextView("There was some communication issue. Please Try again!", BOT);
        }
    }


    private void intentAction(String intent_action) {
        if (intent_action == null || TextUtils.isEmpty(intent_action)) {
            return;
        }


        switch (intent_action) {
            case "turn_left":
                connectedThread.write("A");
                break;
            case "turn_right":
                connectedThread.write("B");
                break;
            case "hands_up":
                connectedThread.write("C");
                break;
            case "hands_down":
                connectedThread.write("D");
                break;
            case "trun_head_right":
                connectedThread.write("E");
                break;
            case "trun_head_left":
                connectedThread.write("F");
                break;
            case "please_dance":
                connectedThread.write("G");
                break;

        }
    }

    private void showTextView(String message, int type) {
        FrameLayout layout;
        switch (type) {
            case USER:
                layout = getUserLayout();
                break;
            case BOT:
                layout = getBotLayout();
                break;
            default:
                layout = getBotLayout();
                break;
        }
        layout.setFocusableInTouchMode(true);
        chatLayout.addView(layout); // move focus to text view to automatically make it scroll up if softfocus
        TextView tv = layout.findViewById(R.id.chatMsg);
        tv.setText(message);
        layout.requestFocus();
        queryEditText.requestFocus(); // change focus back to edit text to continue typing
    }

    FrameLayout getUserLayout() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        return (FrameLayout) inflater.inflate(R.layout.user_msg_layout, null);
    }

    FrameLayout getBotLayout() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        return (FrameLayout) inflater.inflate(R.layout.bot_msg_layout, null);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        AudioZ.muteAudio(this);
        try {
            speechRecognizer.destroy();

            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
            super.onDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.forLanguageTag(languagePref));

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("error", "This Language is not supported");
            } else {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                String template = getString(R.string.bot_response);
//                showTextView(template, BOT);
                // texttoSpeak(template);
            }

            ttsInitialized();


        } else {
            Log.e("error", "Failed to Initialize");
        }
    }

    private void ttsInitialized() {

        // *** set UtteranceProgressListener AFTER tts is initialized ***
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                micBtn.setEnabled(false);
            }

            @Override
            // this method will always called from a background thread.
            public void onDone(String utteranceId) {

                try {
                    Thread.sleep(3000L);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            speechRecognizer.startListening(speach_intent);
                            micBtn.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                            micBtn.setText(R.string.stop);
                            isStart = true;
                            micBtn.setEnabled(true);
                            queryEditText.setHint(getString(R.string.listing));
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        micBtn.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                        speechRecognizer.stopListening();
                        micBtn.setText(getString(R.string.start));
                        isStart = false;
                        micBtn.setEnabled(true);
                    }
                });

            }
        });


    }


    private void texttoSpeak(String text) {
        if ("".equals(text)) {
            text = getString(R.string.i_didnot_find_anything);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MyUniqueUtteranceId");
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.d(TAG, "onReadyForSpeech: ");
    }

    @Override
    public void onBeginningOfSpeech() {
        queryEditText.setText("");
        queryEditText.setHint(getString(R.string.listing));
        Log.d(TAG, "onBeginningOfSpeech: ");
        isStart = true;
    }

    @Override
    public void onRmsChanged(float v) {
    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {
        isStart = false;


    }

    @Override
    public void onError(int i) {
        speechRecognizer.startListening(speach_intent);
        micBtn.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
        micBtn.setText(getString(R.string.stop));
        isStart = true;
        micBtn.setEnabled(true);
        queryEditText.setHint(getString(R.string.listing));


    }

    @Override
    public void onResults(Bundle bundle) {
        ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        // queryEditText.setText(data.get(0));
        queryEditText.setHint(getString(R.string.answer_prosessing));

        micBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        micBtn.setText(getString(R.string.start));


        showTextView(data.get(0), USER);
        QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(data.get(0)).setLanguageCode("en-US")).build();
        new RequestJavaV2Task(MainActivity.this, session, sessionsClient, queryInput).execute();

    }

    @Override
    public void onPartialResults(Bundle bundle) {
        Log.d(TAG, "onPartialResults: ");
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
        Log.d(TAG, "onEvent: ");
    }





    /*-------------Ardiuno Code-------------*/

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null) {
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new MainActivity.ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n') {
                        readMessage = new String(buffer, 0, bytes);
                        Log.e("Arduino Message", readMessage);
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error", "Unable to send message", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


}
