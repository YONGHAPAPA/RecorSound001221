package com.example.recordsound.components;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtil {

    public static final int DATE_FORMAT_FILE = 1;
    public static void openPositiveNegativeDialog(Context context, String title, String messages, String positiveButtonText, String negativeButtonText, DialogInterface.OnClickListener positiveButtonListener, DialogInterface.OnClickListener negativeButtonListener){
        final AlertDialog openDialog;
        AlertDialog.Builder openDialogBuilder = new AlertDialog.Builder(context);
        openDialogBuilder.setTitle(title);
        openDialogBuilder.setMessage(messages);
        openDialogBuilder.setPositiveButton(positiveButtonText, positiveButtonListener);
        openDialogBuilder.setNegativeButton(negativeButtonText, negativeButtonListener);
        openDialog = openDialogBuilder.create();
        openDialog.show();
    }


    public static String getCurrentDateWithTime(int type){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String result = formatter.format(date);

        switch (type){
            case DATE_FORMAT_FILE:
                result = result.replaceAll(":", "");
                break;

            default:
                result = formatter.format(date);
                break;
        }
        return result;
    }

}
