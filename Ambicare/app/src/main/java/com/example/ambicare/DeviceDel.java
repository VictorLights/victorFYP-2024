package com.example.ambicare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class DeviceDel {

    private static FirebaseAuth firebaseAuth;
    private static final FirebaseFirestore deviceList = FirebaseFirestore.getInstance();
    private static final CollectionReference devListRef = deviceList.collection("deviceList");

    public static void deleteDevice(String deviceName, String deviceType, String userEmail) {

        devListRef
                .whereEqualTo("email", userEmail)
                .whereEqualTo("deviceName", deviceName)
                .whereEqualTo("deviceType", deviceType)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Delete the document from Firestore
                                document.getReference().delete();
                            }
                            // Audit Log
                            AuditLog.logAction("Device deletion: " + deviceName + " performed.");
                        } else {
                            Log.d("Error", "Error deleting device");
                        }
                    }
                });
    }
}