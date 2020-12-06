package com.tyagiabhinav.dialogflowchat;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import ai.api.DefaultAIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

public class TestActivity extends AppCompatActivity {


    private SessionsClient sessionsClient;
    private String uuid = UUID.randomUUID().toString();
    private SessionName session;

    private static final String TAG = "TestActivityDDD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText("Who is the CEO of Apple").setLanguageCode("en-US")).build();
        new RequestJavaV2Task(TestActivity.this, session, sessionsClient, queryInput).execute();

        final AIConfiguration config = new AIConfiguration("102593754322148236086",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        AIService aiService = AIService.getService(this, config);
        aiService.setListener(new DefaultAIListener() {
            @Override
            public void onResult(AIResponse result) {
                Log.d(TAG, "onResult: ");
            }

            @Override
            public void onError(AIError error) {
                Log.d(TAG, "onError: ");
            }

            @Override
            public void onListeningCanceled() {
                Log.d(TAG, "onListeningCanceled: ");
            }
        });

//        AIRequest request=new AIRequest();
//        request.setQuery("Hello");
//        request.setEvent();

    }


}