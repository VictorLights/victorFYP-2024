package com.example.ambicare;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends Service {
    private static final long COUNTDOWN_DURATION = 600 * 1000; // 5 minutes in milliseconds
    private static final int CALL_PHONE_REQUEST_CODE = 101;
    private static String currentTime;
    private Handler handler;
    private Runnable stopServiceRunnable;
    private static final String CHANNEL_ID = "FallDetectionServiceChannel";
    private static final String CHANNEL_NAME = "Fall Detection Service";
    private static final String CHANNEL_DESC = "deviceId detected fall";
    private FirebaseFirestore fStore;
    private NotificationListener listener;
    private FirebaseAuth auth;
    private String currentUserEmail;
    public static String getCurrentTime() { return currentTime; }

    // Method to set the listener
    public void setListener(NotificationListener listener) {
        this.listener = listener;
    }

    public interface NotificationListener {
        void onNotificationStopped();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AAAAA", "NotificationService onCreate() called");
        auth = FirebaseAuth.getInstance();
        currentUserEmail = auth.getCurrentUser().getEmail();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AAAAA", "Notification onStartCommand() called");
        if (intent != null) {
            boolean stopNotification = intent.getBooleanExtra("stopNotification", false);
            if (stopNotification) {
                Log.d("AAAAA", "Stopping foreground service and self - NotificationService line70");
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
            fStore = FirebaseFirestore.getInstance();
            Log.d("AAAAA", "Start createNotificationChannel() - NotificationService line77");
            createNotificationChannel();

            // Get the current time
            Date currentDate = new Date();
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            currentTime = timeFormat.format(currentDate);
            Log.d("AAAAA","currentTime:" + currentTime);

            Log.d("AAAAA", "Starting foreground service and create notification - NotificationService line80");
            startForeground(1, createNotification());
            startCountdownTimer();
        }
        return START_STICKY;
    }

    private Notification createNotification() {
        // Create an Intent for opening the AlertActivity when notification is clicked
        Intent alertintent = new Intent(this, AlertActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, alertintent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_visibility_24)
                .setContentTitle("Fall Detected!")
                .setContentText("A fall has been detected by wearable.")//
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(CHANNEL_DESC);
        Log.d("AAAAA", "NotificationChannel created successfully - NotificationService line117");
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private void startCountdownTimer() {
        handler = new Handler();
        stopServiceRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("AAAAA", "Countdown timer elapsed, starting Contact Emergency Services - NotificationService line127");
                callEmergencyContact();
            }
        };
        handler.postDelayed(stopServiceRunnable, COUNTDOWN_DURATION);
        Log.d("AAAAA", "Countdown timer started. Service will perform additional action after " + COUNTDOWN_DURATION / 1000 + " seconds.");
    }

    private void callEmergencyContact() {
        // Query the emergencyContact collection to find the document with the current user's email
        fStore.collection("emergencyContact")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // Check if there's any document retrieved
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Get the first document (assuming there's only one document per user)
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);

                            // Retrieve emergency contact number
                            String emergencyNumber = documentSnapshot.getString("emergencyNumber");
                            Log.d("AAAAA", "Current user emergency number: " + emergencyNumber);

                            // Check if emergency contact number is provided
                            if (emergencyNumber != null && !emergencyNumber.isEmpty()) {
                                // Call the emergency contact number
                                callPhoneNumber(emergencyNumber);
                            } else {
                                // Emergency contact number not provided, call default emergency number
                                callDefaultEmergencyNumber();
                            }
                        } else {
                            // Document for current user not found
                            Log.d("AAAAA", "Document for current user not found in emergencyContact collection - NotificationService line163");
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Error retrieving emergency contact information
                        Log.e("TAG", "Error getting emergency contact information: " + e.getMessage() + " - NotificationService line170");
                    }
                });
    }

    private void callPhoneNumber(String phoneNumber) {
        Log.d("AAAAA", "calling EmergencyNumber - NotificationService line 176");
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_REQUEST_CODE);
        } else {
            // Permission is already granted, start the call
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Start the call in a new task
            startActivity(intent);

            // Stop the timer
            handler.removeCallbacks(stopServiceRunnable);
        }
    }

    private void callDefaultEmergencyNumber() {
        Log.d("AAAAA", "calling DefaultEmergencyNumber - NotificationService line 194");
        // Call the default emergency number (e.g., 999 or anything)
        callPhoneNumber("1234567890");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove callbacks to prevent memory leaks
        if (handler != null && stopServiceRunnable != null) {
            handler.removeCallbacks(stopServiceRunnable);
        }

        Log.d("AAAAA", "Notification Service Stopped - NotificationService line207");
        NotificationManagerCompat.from(this).cancel(1);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}