package com.tyagiabhinav.dialogflowchat.utility;

import android.content.Context;
import android.media.AudioManager;

public class AudioZ {

    public static void unmuteAudio(Context context){
        AudioManager amanager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
    }

    public static void muteAudio(Context context){
        AudioManager amanager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
    }
}
