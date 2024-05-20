package com.example.ambicare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity {
    EditText loginEmail, loginPassword;
    TextView signupRedirectText, forgotPassword;
    Button loginButton;
    ImageView backscreen;

    private FirebaseAuth auth;
    private FirebaseFirestore fStore;
    boolean passwordvisible;
    private int wrongPw = 3;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_email);
        loginButton = findViewById(R.id.login_button);
        loginPassword = findViewById(R.id.login_password);
        signupRedirectText = findViewById(R.id.signup_redirect);
        backscreen = findViewById(R.id.back_screen);
        forgotPassword = findViewById(R.id.forgot_password);

        auth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        loginPassword.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int Right=2;
                if(event.getAction()==MotionEvent.ACTION_UP) {
                    if(event.getRawX()>=loginPassword.getRight()-loginPassword.getCompoundDrawables()[Right].getBounds().width()) {
                        int selection = loginPassword.getSelectionEnd();
                        if(passwordvisible){
                            //set drawable image here
                            loginPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0, R.drawable.baseline_visibility_24,0);
                            //for hide password
                            loginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            passwordvisible = false;
                        } else {
                            //set drawable image here
                            loginPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0, R.drawable.baseline_visibility_24,0);
                            //for show password
                            loginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                            passwordvisible = true;
                        }
                        loginPassword.setSelection(selection);
                        return true;
                    }
                }
                return false;
            }
        });


        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        backscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, ForgotPwActivity.class);
                startActivity(intent);
            }
        });

        // Set click listener for login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String txtEmail = loginEmail.getText().toString();
                String txtPassword = loginPassword.getText().toString();

                if (TextUtils.isEmpty(txtEmail) || TextUtils.isEmpty(txtPassword)) {
                    //prompt that they must fill in email or password
                    Toast.makeText(LoginActivity.this, "Please fill in your email or password !", Toast.LENGTH_SHORT).show();
                } else {
                    // Check if the account is active on other devices
                    checkActiveSessionAndLogin(txtEmail, txtPassword);
                }


            }
        });
    }


    private void checkActiveSessionAndLogin(String email, String password) {
        // Check if the email account is active on other devices
        Log.d("AAAAA", "Start to check account if logged on other device (is isActive = true?) ");
        fStore.collection("Users")
                .whereEqualTo("UserEmail", email)
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Account is active on another device, prompt user
                                Log.d("AAAAA", "Account is active on another device, cannot login");
                                Toast.makeText(LoginActivity.this, "This account is already logged in on another device. Please try again later.", Toast.LENGTH_SHORT).show();
                            } else {
                                // If the account is not active on other devices, prompt the user to log in with their credentials
                                Log.d("AAAAA", "Account is not active on other devices, proceeding to check credentials");
                                checkcredentials(email, password);
                            }
                        } else {
                            // Handle the case where there is an error retrieving the document
                            Log.w("AAAAA", "Error getting documents.", task.getException());
                            Toast.makeText(LoginActivity.this, "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkcredentials(String email, String password) {
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            // Prompt that both email and password must be filled in
            Toast.makeText(LoginActivity.this, "Please fill in your email and password!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(email)) {
            // Prompt that the email must be filled in
            Toast.makeText(LoginActivity.this, "Please fill in your email!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            // Prompt that the password must be filled in
            Toast.makeText(LoginActivity.this, "Please fill in your password!", Toast.LENGTH_SHORT).show();
        } else {
            // Proceed to authenticate user with entered credentials
            loginUser(email, password);
        }
    }

    private void loginUser(String email, String password) {
        Log.d("AAAAA", "LoginUser");
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d("AAAAA", "Credentials correct then proceed to update isActive = true");
                        UpdateIsActive(authResult.getUser().getUid(), true);

                        //proceed with the checking deviceId for listen/not listen
                        checkDeviceRegistration(email);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("AAAAA", "FAIL (email/password error)");
                        // Prompt that the login is failed
                        // Login failed
                        Toast.makeText(LoginActivity.this, "Login Failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                        wrongPw--;
                        Toast.makeText(LoginActivity.this, wrongPw + " more password attempt before locked.", Toast.LENGTH_SHORT).show();
                        if (wrongPw <= 0) {
                            Toast.makeText(LoginActivity.this, "Account locked. Please try again later.", Toast.LENGTH_SHORT).show();
                            lockLogin();
                        }
                    }
                });
    }


    private void lockLogin() {
        loginButton.setVisibility(View.GONE);
        // Set timer to unlock account after 5 minutes
        new CountDownTimer(300000, 1000) { // 5 minutes (300,000 milliseconds)
            public void onTick(long millisUntilFinished) {
                // Countdown is ticking
            }
            public void onFinish() {
                // Reset failed attempts and unlock account
                wrongPw = 3;
                loginButton.setVisibility(View.VISIBLE);
                Toast.makeText(LoginActivity.this, "Account unlocked. You can try again.", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void UpdateIsActive(String uid, boolean isActive) {
        // Update isActive status in Firestore
        DocumentReference userRef = fStore.collection("Users").document(uid);
        userRef.update("isActive", isActive).addOnSuccessListener(aVoid -> {
            Log.d("AAAAA", "login isActive status updated successfully");

            // Proceed with login and navigate user to DashActivity
            // Login successful
            Log.d("AAAAA", "Login successfully");

            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getApplicationContext(), DashActivity.class));


        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle failure to update isActive status
                Log.w("AAAAA", "Error updating isActive status", e);
                Toast.makeText(LoginActivity.this, "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkDeviceRegistration(String email) {
        CollectionReference deviceListRef = fStore.collection("deviceList");

        deviceListRef.whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.exists()) {
                                // Retrieve the device ID from the document
                                String deviceId = document.getString("deviceId");
                                Log.d("AAAAA", "email and deviceId: " + email + ", " + deviceId);

                                // Registered deviceId found -> go home page and start listening service
                                Log.d("AAAAA", "start ListenerDBService after logged in");
                                // Audit Log
                                AuditLog.logAction("Login performed.");

                                startService(new Intent(this, ListenerDBService.class));
                            }
                        }
                        // No document found, then let it runs as usual
                    } else {
                        // Handle failure to retrieve documents
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                    }
                });
    }
}



