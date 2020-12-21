package com.example.recordsound.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.recordsound.MainActivity;
import com.example.recordsound.R;
import com.example.recordsound.broadcast.MyBroadcastReceiver;

public class MyForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    //private MediaSessionCompat mediaSession;

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel("my_service", "My Background Service");
        /*Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Log.e("1", "MyForegroundService input > " + input);


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);*/

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    private void createNotificationChannel(String channelId, String channelName){
//        Log.e("VERSION", Integer.toString(Build.VERSION.SDK_INT));
//        Log.e("VERSION_CODES", Integer.toString(Build.VERSION_CODES.O));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
           /* NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);*/

            Intent resultIntent = new Intent (this, MainActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            PendingIntent resultRendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            assert manager != null;
            manager.createNotificationChannel(chan);


            Intent broadCastIntent = new Intent(this, MyBroadcastReceiver.class);

            broadCastIntent.setAction("ACTION_START_RECORD");
            broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 0);
            PendingIntent startRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

            broadCastIntent.setAction("ACTION_STOP_RECORD");
            broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 1);
            PendingIntent stopRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

            //일반 Notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Recording Sound")
                    .setContentText("Recording Sound in background.")
//                    .setStyle(new NotificationCompat.BigTextStyle()
//                        .bigText("Much longer text that cannot fit one line")
//                    )
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(resultRendingIntent)
                    .addAction(R.drawable.ic_audio_wave_26, "RECORD", startRecordIntent)
                    .addAction(R.drawable.ic_stop_26, "STOP", stopRecordIntent)
                    .build();

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.notify(1, notificationBuilder.build());
            startForeground(1, notification);
        }
    }
}
