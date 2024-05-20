package com.example.ambicare;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ListenerDBService extends Service {
    private static final String TAG = "ListenerService";
    private FirebaseAuth auth;
    private FirebaseFirestore fStore;
    public String deviceId;
    private ListenerRegistration listenerRegistration;
    private static String documentId;
    public static String getDocId() {
        return documentId;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AAAAA", "ListenerDBService onCreate() called");
        fStore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start to run continuously
        Log.d("AAAAA", "ListenerDBService onStartCommand() called");
        fStore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        startListeningForChanges();
        return START_STICKY;
    }

    private void startListeningForChanges() {
        String currentUserEmail = auth.getCurrentUser().getEmail();
        CollectionReference fallLogsRef = fStore.collection("fallLogs");
        CollectionReference deviceListRef = fStore.collection("deviceList");

        // Query to get the device ID for the current user's email
        Log.d("AAAAA", "Fetching documents before starting to listen - ListenerDBService line52");
        deviceListRef.whereEqualTo("email", currentUserEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AAAAA", "Documents retrieved successfully - ListenerDBService line57");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                                // Retrieve the device ID from the document
                                deviceId = document.getString("deviceId");
                                Log.d("AAAAA", "checking deviceId: " + deviceId);
                        }
                    } else {
                        // Handle the error
                        Log.e("AAAAA", "Error getting documents - ListenerDBService line65 ", task.getException());
                    }
                    Log.d("AAAAA", "Starting to listen for new FallLogs - ListenerDBService line68");

                    listenerRegistration = fallLogsRef
                            //checks if the deviceId value matches with the deviceId got earlier
                            .whereEqualTo("deviceId", deviceId)
                            .whereEqualTo("isFall", true) // check if field 'isFall' is true
                            .addSnapshotListener((value, error) -> {
                                if (error != null) {
                                    Log.d("AAAAA", "Listen failed - ListenerDBService line73");
                                    return;
                                }
                                if (value != null) {
                                    for (DocumentChange dc : value.getDocumentChanges()) {
                                        if (dc.getType() == DocumentChange.Type.ADDED) {
                                            Log.d("AAAAA", "Fall detected -> Starting notification service - ListenerDBService line81");

                                            // Get documentId
                                            documentId = dc.getDocument().getId();
                                            Log.d("AAAAA", "documentid: " + documentId );
                                            Toast.makeText(this, "Fall detected", Toast.LENGTH_SHORT).show();

                                            // Pass the data to the NotificationService
                                            Intent notificationIntent = new Intent(getApplicationContext(), NotificationService.class);
                                            Log.d("AAAAA", "Calling notification service - ListenerDBService line90");
                                            getApplicationContext().startForegroundService(notificationIntent);
                                        }
                                    }
                                }

                            });
                });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            Log.d("AAAAA", "Listener Service Stopped");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}




