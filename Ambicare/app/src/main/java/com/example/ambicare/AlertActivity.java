package com.example.ambicare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AlertActivity extends AppCompatActivity implements NotificationService.NotificationListener {
    TextView AlertText;
    Button ReqEmergencyAidbtn, IssueResolvedbtn;
    private FirebaseFirestore fStore;
    private String documentId;
    NotificationService notificationService;
    private static final int CALL_PHONE_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        Log.d("AAAAA", "AlertActivity onCreate() called");

        ReqEmergencyAidbtn = findViewById(R.id.RECbutton);
        IssueResolvedbtn = findViewById(R.id.IRbutton);
        fStore = FirebaseFirestore.getInstance();
        documentId = ListenerDBService.getDocId();
        AlertText = findViewById(R.id.textViewTitle);

        notificationService = new NotificationService();
        notificationService.setListener(this);

        String fallTime = NotificationService.getCurrentTime();
        Log.d("AAAAA", fallTime);
        AlertText.setText("Wearable detected fall at " + fallTime);

        ReqEmergencyAidbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("AAAAA", "Pressed Request Emergency Button - AlertActivity line48");
                Log.d("AAAAA", "Starting Contact Emergency Services - AlertActivity line49");
                Intent intent = new Intent(Intent.ACTION_CALL);
                String numberToCall = "0129803330"; // Replace with the phone number you want to call
                intent.setData(Uri.parse("tel:" + numberToCall));

                if (ActivityCompat.checkSelfPermission(AlertActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request the permission
                    ActivityCompat.requestPermissions(AlertActivity.this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_REQUEST_CODE);
                } else {
                    // Permission is already granted, start the call
                    startActivity(intent);
                }
            }
        });

        IssueResolvedbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("AAAAA", "Pressed IssueResolved Button - AlertActivity line67");
                updateIsFall();
                //further process will be function once 'updateIsFall' update successfully
            }
        });
    }

    private void updateIsFall() {
        Log.d("AAAAA", "Start UpdateIsFall status process - AlertActivity line75");
        CollectionReference fallLogsRef = fStore.collection("fallLogs");
        DocumentReference docRef = fallLogsRef.document(documentId);

        // Double check the current value of 'isFall' before updating
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    boolean isFall = documentSnapshot.getBoolean("isFall");
                    Log.d("AAAAA", "Check current value of isFall: " + isFall + " - AlertActivity line85"); // Add this line for logging
                    if (isFall) {
                        // Update 'isFall' field to false
                        fStore.collection("fallLogs")
                                .document(documentId)
                                .update("isFall", false)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("AAAAA", "DocumentSnapshot 'isFall = false' successfully updated - AlertActivity line94");

                                        Log.d("AAAAA", "Prompt user back to DashActivity and stop notification service - AlertActivity line96");
                                        startActivity(new Intent(AlertActivity.this, DashActivity.class));

                                        //stop notification service
                                        stopNotificationService();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("AAAAA", "Error updating document - AlertActivity line106");
                                    }
                                });
                    } else {
                        Log.d("AAAAA", "Document already updated - AlertActivity line110");
                    }
                } else {
                    Log.d("AAAAA", "Document does not exist - AlertActivity line113");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("AAAAA", "Error getting document - AlertActivity line119");
            }
        });
    }


    private void stopNotificationService() {
        // Audit Log
        AuditLog.logAction("Fall alert detected!");
        // Stop the NotificationService
        Intent notificationIntent = new Intent(getApplicationContext(), NotificationService.class);
        notificationIntent.putExtra("stopNotification", true);
        startService(notificationIntent);
    }

    @Override
    public void onNotificationStopped() {
        // just leave it empty but don't remove
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the NotificationService instance
        notificationService.setListener(null);
    }
}