package com.example.ambicare;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class FallDetectionService extends Service {

    private static final String CHANNEL_ID = "fall_detection_channel";

    public static final String ACTION_FALL_DETECTED = "com.example.ambicare.ACTION_FALL_DETECTED";
    public static final String ACTION_ELDERLY_SAFE = "com.example.ambicare.ACTION_ELDERLY_SAFE";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DocumentReference docRef = db.collection("fallrecLogs").document(uid);
            docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        boolean isFallen = snapshot.getBoolean("fallen");
                        if (isFallen) {
                            showNotification();
                            sendBroadcast(new Intent(ACTION_FALL_DETECTED));
                        } else {
                            sendBroadcast(new Intent(ACTION_ELDERLY_SAFE));
                        }
                    }

                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void showNotification() {
        Intent videoPlayerIntent = new Intent(this, VideoPlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, videoPlayerIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_visibility_24)
                .setContentTitle("Fall Detected!")
                .setContentText("A fall has been detected by the camera.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, builder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
