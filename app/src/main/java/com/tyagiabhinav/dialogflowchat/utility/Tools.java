package com.tyagiabhinav.dialogflowchat.utility;

import android.text.format.Time;
import android.util.Log;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Tools {

    private static final String TAG = "Tools";

    // Regular expression that will match our symbols
    // special case for "divided by" since that's two words for one symbol
    static final Pattern SYMBOL_PATTERN = Pattern.compile("(divided by|[a-z-]+)", Pattern.CASE_INSENSITIVE);
    private static final String ERROR_RESULT ="Sorry Your question is not correct ! Please say it again.";


    public static String mathResultFromNaturalLanguage(String input) {

        String sentence = input.toLowerCase().toString()
                .replaceAll("calculate please", "")
                .replaceAll("please calculate", "")
                .replaceAll("math calculate", "")
                .replaceAll("calculate", "")
                .replaceAll("please", "")
                .replaceAll("edubot", "")
                .replaceAll("bot", "")
                .replaceAll("can you please calculate", "")
                .replaceAll("-", "minus")
                .replaceAll("can you please", "");

        try {
            //  Log.d(TAG, "mathResultFromNaturalLanguage: "+sentence);
            System.out.println(sentence);
            String toMath = toMath(sentence);
            System.out.println(toMath);

            Context context = Context.enter(); //
            context.setOptimizationLevel(-1); // this is required[2]
            Scriptable scope = context.initStandardObjects();
            Object result = context.evaluateString(scope, toMath, "JavaScript", 1, null);
            System.out.println(result.toString());

            return "Result : "+result.toString();



        } catch (Exception e) {
            return "Sorry your answer is not correct ";
        }


    }

    private static String toMath(String sentence) {
        // Builder to build the translated string
        StringBuilder builder = new StringBuilder();
        // End of the last matched group
        int lastEnd = 0;
        // Go through all symbols in the string
        Matcher matcher = SYMBOL_PATTERN.matcher(sentence);
        while(matcher.find()) {
            // The matched symbol
            String symbol = matcher.group(0).toLowerCase();
            // Get the replacement
            String replacement = getReplacement(symbol);
            // Append everything between the previous match and this match
            builder.append(sentence.substring(lastEnd, matcher.start()));
            // Append the replacement
            builder.append(replacement);
            // Update the end
            lastEnd = matcher.end();
        }
        // Append the end of the string and return it
        builder.append(sentence.substring(lastEnd));
        return builder.toString();
    }

    // Map to hold replacement symbols
    static final HashMap<String, String> REPLACEMENT_MAP = new HashMap<>();
    static {
        REPLACEMENT_MAP.put("zero", "0");
        REPLACEMENT_MAP.put("one", "1");
        REPLACEMENT_MAP.put("two", "2");
        REPLACEMENT_MAP.put("three", "3");
        REPLACEMENT_MAP.put("four", "4");
        REPLACEMENT_MAP.put("five", "5");
        REPLACEMENT_MAP.put("six", "6");
        REPLACEMENT_MAP.put("seven", "7");
        REPLACEMENT_MAP.put("eight", "8");
        REPLACEMENT_MAP.put("nine", "9");
        REPLACEMENT_MAP.put("ten", "10");
        REPLACEMENT_MAP.put("eleven", "11");
        REPLACEMENT_MAP.put("twelve", "12");
        REPLACEMENT_MAP.put("thirteen", "13");
        REPLACEMENT_MAP.put("fourteen", "14");
        REPLACEMENT_MAP.put("fifteen", "15");
        REPLACEMENT_MAP.put("sixteen", "16");
        REPLACEMENT_MAP.put("seventeen", "17");
        REPLACEMENT_MAP.put("eighteen", "18");
        REPLACEMENT_MAP.put("nineteen", "19");
        REPLACEMENT_MAP.put("twenty", "20");
        REPLACEMENT_MAP.put("thirty", "30");
        REPLACEMENT_MAP.put("forty", "40");
        REPLACEMENT_MAP.put("fifty", "50");
        REPLACEMENT_MAP.put("sixty", "60");
        REPLACEMENT_MAP.put("seventy", "70");
        REPLACEMENT_MAP.put("eighty", "80");
        REPLACEMENT_MAP.put("ninety", "90");
        REPLACEMENT_MAP.put("ten", "10");
        REPLACEMENT_MAP.put("hundred", "*100");
        REPLACEMENT_MAP.put("thousand", "*1000");
        REPLACEMENT_MAP.put("million", "*1000000");
        REPLACEMENT_MAP.put("billion", "*1000000000");
        REPLACEMENT_MAP.put("trillion", "*trillion");
        REPLACEMENT_MAP.put("plus", "+");
        REPLACEMENT_MAP.put("minus", "-");
        REPLACEMENT_MAP.put("into", "*");
        REPLACEMENT_MAP.put("modulus", "%");
        REPLACEMENT_MAP.put("divided by", "/");
        REPLACEMENT_MAP.put("multiplication ", "*");
    }

    private static String getReplacement(String symbol) {
        // Handle compounds such as fifty-five
        if(symbol.contains("-")) {
            // add each individual symbol together and return the result
            // this is far from perfect since it will allow compounds such as
            // fifty-five-nine which would become 64
            int value = 0;
            // Go through each individual symbol and translate it
            String[] symbols = symbol.split("-");
            for(String s : symbols) {
                if(!REPLACEMENT_MAP.containsKey(s)) {
                    throw new IllegalArgumentException("Unknown symbol: " + s);
                }
                value += Integer.parseInt(getReplacement(s));
            }
            return String.valueOf(value);
            // Straight translation
        } else if (REPLACEMENT_MAP.containsKey(symbol)) {
            return REPLACEMENT_MAP.get(symbol);
            // Unknown symbol
        } else {
            throw new IllegalArgumentException("Unknown symbol: " + symbol);
        }
    }

    public static String getTime(){
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.forLanguageTag("bn-BD")).format(new Date());
        return "Now it's \n"+currentTime.toUpperCase();
    }

    public static String getDate(){
        String currentDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        return "Today is : "+currentDate;
    }






}
