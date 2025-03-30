package com.p17142.vroom.utilities;

import android.util.Log;

import com.p17142.vroom.models.Trip;

public class Logger {
    public static void printLog(Class object, String type, String message){ // balader
        if (checkDebug()) Log.d("Vroom-"+type,"["+object.getSimpleName()+"] "+message);
    }

    public static void printLog(Class object, String message){
        if (checkDebug()) Log.d("Vroom-INFO","["+object.getSimpleName()+"] "+message);
    }
    public static void printLog(String source , String message){
        if (checkDebug()) Log.d("Vroom-INFO","["+source+"] "+message);
    }

    public static void printLogFatal(Class object, String message){
        if (checkDebug()) Log.d("Vroom-FATAL","["+object.getSimpleName()+"] "+message);
    }

    public static void printLogError(Class object, String message){
        if (checkDebug()) Log.d("Vroom-ERROR","["+object.getSimpleName()+"] "+message);
    }

    public static boolean checkDebug(){
        return Constants.DEBUG_MODE;
    }

}
