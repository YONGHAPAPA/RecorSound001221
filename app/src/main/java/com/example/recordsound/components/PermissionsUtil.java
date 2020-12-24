package com.example.recordsound.components;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PermissionsUtil {

    String TAG = "PermissionsUtil";

    public HashMap<Integer, Boolean> checkPermissions(Context context, HashMap<Integer, String> requestPermissions, Boolean requestFlag){

        HashMap<Integer, Boolean> result = null;
        HashMap<Integer, String> permissionForCheck = requestPermissions;

        try{
            for(Map.Entry entry : requestPermissions.entrySet()){
                Log.d(TAG, entry.getKey() + ": " + entry.getValue());
            }

        } catch(Exception e){
            Log.e(TAG, e.getMessage());
        } finally {

        }

        return result;
    }
}
