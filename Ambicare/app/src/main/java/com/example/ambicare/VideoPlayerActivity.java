package com.example.ambicare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final int CALL_PHONE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Button playButton = findViewById(R.id.play_button);
        Button ReqEmergencyAidbtn = findViewById(R.id.emergencyReq);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String uid = user.getUid();
                FirebaseStorage.getInstance().getReference().child("recordings/" + uid).listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        List<StorageReference> items = listResult.getItems();
                        if (!items.isEmpty()) {
                            Collections.sort(items, new Comparator<StorageReference>() {
                                @Override
                                public int compare(StorageReference o1, StorageReference o2) {
                                    return Long.compare(getUpdatedTime(o1), getUpdatedTime(o2));
                                }
                            });
                            StorageReference lastModifiedFile = items.get(items.size() - 1);
                            lastModifiedFile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(uri, "video/mp4");
                                    startActivity(intent);
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("VideoPlayerActivity", "Failed to get download URL", e);
                    }
                });
            }
        });
        Button goBackButton = findViewById(R.id.go_back_button);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String uid = user.getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference docRef = db.collection("fallrecLogs").document(uid);
                docRef.update("fallen", false)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("VideoPlayerActivity", "DocumentSnapshot successfully updated!");
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("VideoPlayerActivity", "Error updating document", e);
                            }
                        });
            }
        });
        ReqEmergencyAidbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("AAAAA", "Pressed Request Emergency Button - AlertActivity line48");
                Log.d("AAAAA", "Starting Contact Emergency Services - AlertActivity line49");
                Intent intent = new Intent(Intent.ACTION_CALL);
                String numberToCall = "0129803330"; // Replace with the phone number you want to call
                intent.setData(Uri.parse("tel:" + numberToCall));

                if (ActivityCompat.checkSelfPermission(VideoPlayerActivity.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request the permission
                    ActivityCompat.requestPermissions(VideoPlayerActivity.this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_REQUEST_CODE);
                } else {
                    // Permission is already granted, start the call
                    startActivity(intent);
                }
            }
        });
    }

    private long getUpdatedTime(StorageReference reference) {
        Task<StorageMetadata> metadataTask = reference.getMetadata();
        if (metadataTask.isSuccessful()) {
            StorageMetadata metadata = metadataTask.getResult();
            if (metadata != null) {
                return metadata.getUpdatedTimeMillis();
            }
        }
        return 0;
    }
}
