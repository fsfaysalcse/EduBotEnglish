package com.tyagiabhinav.dialogflowchat.utility;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

public class FontTextView extends AppCompatTextView {

    Typeface typeface;

    public FontTextView(Context context) {
        super(context);
        String lang = SharedPref.getKey(context,Constants.LANG);
        if (lang=="en-US"){
            typeface=Typeface.createFromAsset(context.getAssets(), "kalpurush.ttf");
        }else {
            typeface=Typeface.createFromAsset(context.getAssets(), "montserrat_regular.ttf");
        }
        this.setTypeface(typeface);
    }

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        String lang = SharedPref.getKey(context,Constants.LANG);
        if (lang=="en-US"){
            typeface=Typeface.createFromAsset(context.getAssets(), "kalpurush.ttf");
        }else {
            typeface=Typeface.createFromAsset(context.getAssets(), "montserrat_regular.ttf");
        }
        this.setTypeface(typeface);
    }

    public FontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        String lang = SharedPref.getKey(context,Constants.LANG);
        if (lang=="en-US"){
            typeface=Typeface.createFromAsset(context.getAssets(), "kalpurush.ttf");
        }else {
            typeface=Typeface.createFromAsset(context.getAssets(), "montserrat_regular.ttf");
        }
        this.setTypeface(typeface);
    }

    protected void onDraw (Canvas canvas) {
        super.onDraw(canvas);


    }

}