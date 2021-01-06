package com.example.recordsound.components;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

import com.example.recordsound.MainActivity;
import com.example.recordsound.R;
import com.example.recordsound.vo.PermissionVO;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PermissionsUtil{

    private static final String TAG_PREFIX = "MZ_";
    static final String TAG = TAG_PREFIX + PermissionsUtil.class.getSimpleName();

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


    public void requestPermission(Context context, View view, PermissionVO permission, ActivityResultLauncher<String> requestPermissionLauncher) {

        try{

            Activity activity = (Activity) context;
            //for(Map.Entry<Integer, HashMap<String, Integer>> requestPermission : reqestPermissions.entrySet()){
            //Log.d(TAG, "request Permission >>>>> " + permission.getPermission());

            if(ActivityCompat.checkSelfPermission(context, permission.getPermission()) != PackageManager.PERMISSION_GRANTED){

                Boolean permissionRational = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.getPermission());
                Log.d(TAG, "permissionRational >>> " + permissionRational);

                if(!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.getPermission())){

                    //사용자가 권한처리 팝업을 중지처리한상태로 강제로 팝업창을 띄워서 다시 설정할수 있도록 해준다.
                    //Log.d(TAG, "requestPermissionName >> " + permission.getPermission());
                    CommonUtil.openPositiveNegativeDialog(context, context.getString(R.string.P002), context.getString(R.string.P003, permission.getPermission()), context.getString(R.string.C001), context.getString(R.string.C002), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Log.d(TAG, "Request permission : " + permission.getPermission());
                            startIntentPermissionSetting(context);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(context, R.string.P004, Toast.LENGTH_LONG).show();
                        }
                    });

                } else {

                    //권한설정을 요청한다.
                    //Log.d(TAG, "request final .>>> " + permission.getPermission());
                    //ActivityCompat.requestPermissions(activity, new String[] { permission.getPermission() }, permission.getRequestId());
                    requestPermissionLauncher.launch(permission.getPermission());
                }
            }

        } catch (Exception e){
            Log.e(TAG, "requestPermission >> " + e.getMessage());
        }
    }


    public void requestPermissions(Context context, View view, ArrayList<PermissionVO> permissions, ActivityResultLauncher<String[]> requestPermissionsLauncher){

        ArrayList<String> voList = new ArrayList<>();
        Activity activity = (Activity)context;
        Boolean showPermissionRationale = true;

        try{

            int i = 0;
            String[] permissionForRequest;
            ArrayList<String> showUIPermissions = new ArrayList<>();
            ArrayList<String> noShowUIPermissions = new ArrayList<>();

            for(PermissionVO vo: permissions){
                if(ActivityCompat.checkSelfPermission(context, vo.getPermission()) != PackageManager.PERMISSION_GRANTED){
//                    showPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, vo.getPermission());
//                    Log.d(TAG, "showPermissionRationale: " + showPermissionRationale + " > " + vo.getPermission());
                    if(ActivityCompat.shouldShowRequestPermissionRationale(activity, vo.getPermission())){
                         showUIPermissions.add(vo.getPermission());
                    } else {
                        //Rationale UI : False 인 권한
                        noShowUIPermissions.add(vo.getPermission());
                    }
                }
            }

            if(showUIPermissions.size() > 0){
                permissionForRequest = showUIPermissions.toArray(new String[showUIPermissions.size()-1]);
                requestPermissionsLauncher.launch(permissionForRequest);
            }

            if(showUIPermissions.size() == 0 && noShowUIPermissions.size() > 0){

                String kindOfPermissions = "";
                for(String item: noShowUIPermissions){
                    kindOfPermissions += item + ", ";
                }

                CommonUtil.openPositiveNegativeDialog(context, context.getString(R.string.P002), context.getString(R.string.P003, kindOfPermissions), context.getString(R.string.C001), context.getString(R.string.C002),
                  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startIntentPermissionSetting(context);
//                        String[] req = noShowUIPermissions.toArray(new String[noShowUIPermissions.size()-1]);
//                        requestPermissionsLauncher.launch(req);
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Application 종료처리 또는 whatever to do....
                        Toast.makeText(context, R.string.P004, Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e){
            Log.e(TAG, "requestPermissions >> " + e.getMessage());
        }
    }


    public void startIntentPermissionSetting(Context context){
        Activity activity = (Activity)context;

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);

        intent.setData(uri);
        activity.startActivity(intent);
    }
}
