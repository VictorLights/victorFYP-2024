package com.example.ambicare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    EditText signupEmail, signupPassword, signupPassword2;
    TextView loginRedirectText, forgotPassword;
    Button signupButton;
    ImageView backscreen;
    private FirebaseAuth auth;
    private FirebaseFirestore fStore;
    boolean passwordvisible;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupPassword2 = findViewById(R.id.signup_password2);
        loginRedirectText = findViewById(R.id.login_redirect);
        signupButton = findViewById(R.id.signup_button);
        backscreen = findViewById(R.id.back_screen);
        forgotPassword = findViewById(R.id.forgot_password);
        auth = FirebaseAuth.getInstance();

        fStore = FirebaseFirestore.getInstance();

        attachPasswordToggleListener(signupPassword);
        attachPasswordToggleListener(signupPassword2);

        // Load animation
        Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animation to your CardView
        CardView loginCardView = findViewById(R.id.login_cardview);
        loginCardView.startAnimation(slideUpAnimation);
    }


    @SuppressLint("ClickableViewAccessibility")
        private void attachPasswordToggleListener(final EditText passwordEditText) {
            passwordEditText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int Right = 2;
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (event.getRawX() >= passwordEditText.getRight() - passwordEditText.getCompoundDrawables()[Right].getBounds().width()) {
                            int selection = passwordEditText.getSelectionEnd();
                            if (passwordvisible) {
                                //set drawable image here
                                passwordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_24, 0);
                                //for hide password
                                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                                passwordvisible = false;
                            } else {
                                //set drawable image here
                                passwordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_24, 0);
                                //for show password
                                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                                passwordvisible = true;
                            }
                            passwordEditText.setSelection(selection);
                            return true;
                        }
                    }
                    return false;
                }
            });




        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }

        });

        backscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, ForgotPwActivity.class);
                startActivity(intent);
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String emailReg =  signupEmail.getText().toString();
                String passwordReg = signupPassword.getText().toString();
                String passwordReg2 = signupPassword2.getText().toString();

                if (TextUtils.isEmpty(emailReg) || TextUtils.isEmpty(passwordReg)) {
                    //prompt that they must fill in email or password
                    Toast.makeText(SignupActivity.this, "Please fill in your email or password !", Toast.LENGTH_SHORT).show();

                } else if (!(passwordReg.length() > 5 && passwordReg.length() < 13)) {
                    Toast.makeText(SignupActivity.this, "Password needs to be at least 6 - 12 characters.", Toast.LENGTH_SHORT).show();

                } else if (!isValidPassword(passwordReg)) {
                    // Prompt that password does not meet complexity requirements
                    Toast.makeText(SignupActivity.this, "Please try a mix of Uppercase Lowercase, digits and special character.", Toast.LENGTH_SHORT).show();

                } else if (!passwordReg.equals(passwordReg2)) {
                    //prompt that password1 and password2 must be the same
                    Toast.makeText(SignupActivity.this, "Both passwords must be the same.", Toast.LENGTH_SHORT).show();

                } else {
                    //start registration user input in database
                    registerUser(emailReg, passwordReg);
                }
            }
        });
    }

    // Function to validate password format
    private boolean isValidPassword(String password) {
        // Regular expression to match password complexity requirements
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,12}$";
        return password.matches(passwordPattern);
    }

    private void registerUser(String regisEmail, String regisPassword) {
        auth.createUserWithEmailAndPassword(regisEmail, regisPassword).addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d("AAAAA", "SignupUser");
                    boolean isActive = false;
                    FirebaseUser user = auth.getCurrentUser();
                    DocumentReference df = fStore.collection("Users").document(user.getUid());
                    Map<String,Object> userInfo = new HashMap<>();
                    userInfo.put("UserEmail", signupEmail.getText().toString());
                    userInfo.put("isActive", isActive);

                    // Audit Log
                    AuditLog.logAction("Signup performed.");

                    // Store data into Firestore
                    df.set(userInfo);

                    Log.d("AAAAA", "Signup successfully and created isActive = false status");


                    // Create a folder in Firebase Storage with the user's UID as the folder name
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("recordings/" + user.getUid() + "/video file/" + "/foldertest");
                    storageRef.putBytes(new byte[0]).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.d("AAAAA", "Folder created successfully in Firebase Storage");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("AAAAA", "Failed to create folder in Firebase Storage: " + e.getMessage());
                        }
                    });


                    // Send verification email
                    user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Verification email sent successfully
                                Toast.makeText(SignupActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            } else {
                                // Verification email sending failed, display a message
                                Toast.makeText(SignupActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    // Redirect user to additional information page
                    Intent intent = new Intent(SignupActivity.this, additionalInfo.class);
                    // Pass necessary data to the additional information activity
                    intent.putExtra("userEmail", signupEmail.getText().toString());
                    startActivity(intent);
                } else {
                    // Registration failed
                    // Check if the error is due to the account already being registered
                    Exception exception = task.getException();
                    if (exception instanceof FirebaseAuthUserCollisionException) {
                        // If account has already been registered
                        // Display an appropriate error message
                        Toast.makeText(SignupActivity.this, "This email address has already been registered before. Please try again with a different email.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Other registration failures
                        Toast.makeText(SignupActivity.this, "Account registration failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

}