package com.example.ambicare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPwActivity extends AppCompatActivity {

    Button forgotPasswordButton, gotoLoginButton;
    EditText forgotPasswordEmail;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pw);

        forgotPasswordEmail = findViewById(R.id.forgotPwEmail);
        forgotPasswordButton = findViewById(R.id.forgotPwButton);
        gotoLoginButton = findViewById(R.id.loginNavButton);

        auth = FirebaseAuth.getInstance();

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String forgotEmailTXT = forgotPasswordEmail.getText().toString();
                if (TextUtils.isEmpty(forgotEmailTXT)) {
                    // ask user to key in their email in the email section
                    Toast.makeText(ForgotPwActivity.this, "Please enter your email address to proceed with the password reset.", Toast.LENGTH_LONG).show();
                } else {
                    auth.sendPasswordResetEmail(forgotEmailTXT).addOnCompleteListener(new OnCompleteListener<Void>() {

                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(ForgotPwActivity.this, "The password reset request is sent to your email.", Toast.LENGTH_SHORT).show();
                                // Audit Log
                                AuditLog.logAction("Password Reset Requested");
                            } else {
                                Toast.makeText(ForgotPwActivity.this, "Email isn't registered, please make sure the email is correct.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        gotoLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ForgotPwActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}