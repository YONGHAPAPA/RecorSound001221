package com.example.recordsound.components;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.recordsound.R;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PermissionsUtil{

    String TAG = "[PERMISSIONS_UTIL]";

    public Map<Integer, Map<Integer, String>> getPermissions(Context context, View view, HashMap<Integer, String> permissions){

        Map<Integer, Map<Integer, String>> result = new HashMap<Integer, Map<Integer, String>>();

        try{
            for(Map.Entry entry : permissions.entrySet()){

                Boolean isGranted = ActivityCompat.checkSelfPermission(context, entry.getValue().toString()) != PackageManager.PERMISSION_GRANTED ? false : true;

                //Log.d(TAG, "checkPermissions >>> " + entry.getValue() + " : " + isGranted);
                result.put(Integer.parseInt(entry.getKey().toString()), new HashMap(){{put(entry.getValue().toString(), ActivityCompat.checkSelfPermission(context, entry.getValue().toString()));}});
            }
        } catch(Exception e){
            Log.e(TAG, e.getMessage());
        }

        return result;
    }


    public void requestPermission(Context context, View view, HashMap<Integer, HashMap<String, Integer>> reqestPermissions) {

        try{

            Activity activity = (Activity) context;

            for(Map.Entry<Integer, HashMap<String, Integer>> requestPermission : reqestPermissions.entrySet()){

                for(Map.Entry<String, Integer> permission : requestPermission.getValue().entrySet()){

                    Log.d(TAG, "request Permission >>>>> " + permission.getKey());

                    if(ActivityCompat.checkSelfPermission(context, permission.getKey()) != PackageManager.PERMISSION_GRANTED){
                        if(ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.getKey())){

                            //사용자가 권한처리 팝업을 중지처리한상태로 강제로 팝업창을 띄워서 다시 설정할수 있도록 해준다.
                            Log.d(TAG, "requestPermissionName >> " + permission.getKey());
                            CommonUtil.openPositiveNegativeDialog(context, context.getString(R.string.P002), context.getString(R.string.P003, permission.getKey()), context.getString(R.string.C001), context.getString(R.string.C002), new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "Request permission : " + permission.getKey());
                                    ActivityCompat.requestPermissions(activity, new String[]{ permission.getKey() }, Integer.parseInt(requestPermission.getKey().toString()));
                                }
                            }, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    Log.d(TAG, "CANCEL >>>>>>>>>>>>>>>>>>>>>>>>>>");
                                    Toast.makeText(context, R.string.P004, Toast.LENGTH_LONG).show();
                                }
                            });

                        } else {

                            //권한설정을 요청한다.
                            ActivityCompat.requestPermissions(activity, new String[] { permission.getKey().toString() }, Integer.parseInt(requestPermission.getKey().toString()));
                        }
                    }
                }
            }

        } catch (Exception e){
            Log.e(TAG, "requestPermission >> " + e.getMessage());
        }
    }

    public void requestPermissions(Activity activity, HashMap<Integer, String> requestPermissions){

    }
}
