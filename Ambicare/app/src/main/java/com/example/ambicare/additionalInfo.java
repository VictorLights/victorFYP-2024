package com.example.ambicare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.HashMap;
import java.util.Map;

public class additionalInfo extends AppCompatActivity {

    EditText elderlyNameEditText, emergencyContactEditText, emergencyContactNameEditText;
    RadioGroup elderlyGenderRadioGroup;
    Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional_info);

        // Initialize EditText fields
        elderlyNameEditText = findViewById(R.id.elderly_name);
        emergencyContactEditText = findViewById(R.id.emergency_contact);
        emergencyContactNameEditText = findViewById(R.id.emergency_contact_name);

        // Initialize RadioGroup for Elderly Gender
        elderlyGenderRadioGroup = findViewById(R.id.elderly_gender_radiogroup);

        // Initialize Save Button
        saveButton = findViewById(R.id.save_button);

        // Set OnClickListener for Save Button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve values from EditText fields
                String elderlyName = elderlyNameEditText.getText().toString().trim();
                String emergencyContact = emergencyContactEditText.getText().toString().trim();
                String emergencyContactName = emergencyContactNameEditText.getText().toString().trim();

                // Retrieve selected radio button for Elderly Gender
                RadioButton selectedGenderRadioButton = findViewById(elderlyGenderRadioGroup.getCheckedRadioButtonId());
                String elderlyGender = selectedGenderRadioButton != null ? selectedGenderRadioButton.getText().toString() : "";

                // Check if any field is empty
                if (elderlyName.isEmpty() || elderlyGender.isEmpty() || emergencyContact.isEmpty() || emergencyContactName.isEmpty()) {
                    // Display a toast message indicating all fields are required
                    Toast.makeText(additionalInfo.this, "All fields are required", Toast.LENGTH_SHORT).show();
                } else {
                    // All fields are filled, you can proceed to save the data
                    // Here you can save the data to Firebase Firestore or any other storage mechanism
                    // For demonstration purposes, just display a success message
                    Toast.makeText(additionalInfo.this, "Information saved successfully", Toast.LENGTH_SHORT).show();
                    // You can also navigate to another activity or perform any other action here
                    saveDataToFirestore(elderlyName, elderlyGender, emergencyContact, emergencyContactName);
                }
            }
        });
    }

    private void saveDataToFirestore(String elderlyName, String elderlyGender, String emergencyContact, String emergencyContactName) {
        // Get a Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get the current user's email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();

            // Check if the user already has an emergency contact registered
            db.collection("emergencyContact")
                    .whereEqualTo("email", userEmail)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Check if there is an existing emergency contact for the user
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                // User already has an emergency contact registered
                                Toast.makeText(additionalInfo.this, "You already have an emergency contact registered", Toast.LENGTH_SHORT).show();
                            } else {
                                // No existing emergency contact for the user, proceed to save the data

                                // Create a new document with a generated ID
                                DocumentReference docRef = db.collection("emergencyContact").document();

                                // Create a map to hold the data
                                Map<String, Object> data = new HashMap<>();
                                data.put("elderlyName", elderlyName);
                                data.put("email", userEmail); // Using the current user's email
                                data.put("emergencyName", emergencyContactName);
                                data.put("emergencyNumber", emergencyContact);
                                data.put("gender", elderlyGender);

                                // Add a new document with a generated ID
                                docRef.set(data)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Data saved successfully
                                                Toast.makeText(additionalInfo.this, "Information saved successfully", Toast.LENGTH_SHORT).show();
                                                // Audit Log
                                                AuditLog.logAction("Updated Elderly Information");
                                                Toast.makeText(additionalInfo.this, "Verification email send, please check your email", Toast.LENGTH_SHORT).show();
                                                // Navigate to Dashboard activity here
                                                navigateToDashboard();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Failed to save data
                                                Toast.makeText(additionalInfo.this, "Failed to save information: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            // Failed to check for existing emergency contact
                            Toast.makeText(additionalInfo.this, "Failed to check for existing emergency contact: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // User is not logged in, handle the case accordingly
            Toast.makeText(additionalInfo.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToDashboard() {
        // Start Dashboard activity
        startActivity(new Intent(additionalInfo.this, LoginActivity.class));
        // Finish current activity
        finish();
    }
}
