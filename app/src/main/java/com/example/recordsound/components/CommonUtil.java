package com.example.recordsound.components;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class CommonUtil {

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
}
