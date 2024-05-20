package com.example.ambicare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class changePwActivity extends AppCompatActivity {
    Button changePasswordButton, backToSettingsButton;
    EditText changePasswordEmail;
    private FirebaseAuth auth;
    private FirebaseFirestore fStore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pw);

        changePasswordEmail = findViewById(R.id.changePwEmail);
        changePasswordButton = findViewById(R.id.changePwButton);
        backToSettingsButton = findViewById(R.id.goToSettingsButton);

        auth = FirebaseAuth.getInstance();

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String forgotEmailTXT = changePasswordEmail.getText().toString();
                if (TextUtils.isEmpty(forgotEmailTXT)) {
                    // ask user to key in their email in the email section
                    Toast.makeText(changePwActivity.this, "Please enter your email address to proceed with password change.", Toast.LENGTH_LONG).show();
                } else {
                    auth.sendPasswordResetEmail(forgotEmailTXT).addOnCompleteListener(new OnCompleteListener<Void>() {

                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(changePwActivity.this, "The password reset request is sent to your email.", Toast.LENGTH_SHORT).show();
                                // Audit Log
                                AuditLog.logAction("Change Password Requested");
                                logoutAndUpdateIsActive();

                            } else {
                                Toast.makeText(changePwActivity.this, "Email isn't registered, please make sure the email is correct.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        private void logoutAndUpdateIsActive() {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                Log.d("AAAAA", "LogoutUser");
                                Log.d("AAAAA", "Update isActive status = false");
                                FirebaseFirestore.getInstance()
                                        .collection("Users")
                                        .document(user.getUid())
                                        .update("isActive", false)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Audit Log
                                                AuditLog.logAction("Logout performed.");
                                                // Successfully updated isActive status to false
                                                FirebaseAuth.getInstance().signOut();
                                                Toast.makeText(changePwActivity.this, "Logout successful", Toast.LENGTH_SHORT).show();

                                                //stop ListenerDBService once logged out
                                                Log.d("AAAAA", "Stop listener service - SettingFragment line96");
                                                Intent stopServiceIntent = new Intent(changePwActivity.this, ListenerDBService.class);
                                                changePwActivity.this.stopService(stopServiceIntent);

                                                Intent intent = new Intent(changePwActivity.this, LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Handle failure to update isActive status
                                                Toast.makeText(changePwActivity.this, "Failed to update isActive status", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }

                    });
                }
                fStore = FirebaseFirestore.getInstance();

            }
        });
        fStore = FirebaseFirestore.getInstance();
        backToSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });



    }
}