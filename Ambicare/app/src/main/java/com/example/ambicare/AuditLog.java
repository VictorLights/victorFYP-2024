package com.example.ambicare;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AuditLog {
    //private static String emailPreserved;
    static FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private static String currentUserEmail = currentUser.getEmail();
    // Method to log an action into firebase
    public static void logAction(String action) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference logsRef = db.collection("auditLogs");

        Map<String, Object> logData = new HashMap<>();
        logData.put("userEmail", currentUserEmail);
        logData.put("activity", action);
        logData.put("timestamp", FieldValue.serverTimestamp());

        logsRef.add(logData)
                .addOnSuccessListener(documentReference -> {
                    // Log was added successfully
                    Log.d("AuditLog", "Action logged successfully: " + action);
                })
                .addOnFailureListener(e -> {
                    // Handle any errors
                    Log.e("AuditLog", "Error logging action: " + action, e);
                });
    }

}
