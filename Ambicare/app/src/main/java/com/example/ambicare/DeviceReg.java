package com.example.ambicare;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class DeviceReg extends AppCompatActivity {

    public class addDevice {
        private String email;
        private String deviceId;
        private String deviceType;
        private String deviceName;

        public addDevice() {

        }

        public addDevice(String email, String deviceId, String deviceType, String deviceName) {
            this.email = email;
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.deviceName = deviceName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }
    }

    EditText deviceId;
    EditText deviceOtp;
    EditText deviceName;
    RadioGroup radioGroup;
    RadioButton radioButton;
    Button saveButton;

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore deviceList = FirebaseFirestore.getInstance();
    private final CollectionReference devRef = deviceList.collection("devices");
    private final CollectionReference devListRef = deviceList.collection("deviceList");

    private static final String TAG = "DeviceReg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        devRegSuccess();
    }

    private void devRegSuccess() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_device_reg);

        findViews(dialog);

        setupCloseButton(dialog);
        setupSaveButton(dialog);

        setupDialogProperties(dialog);
        dialog.show();
    }

    private void findViews(Dialog dialog) {
        deviceId = dialog.findViewById(R.id.device_id);
        deviceOtp = dialog.findViewById(R.id.device_OTP);
        deviceName = dialog.findViewById(R.id.device_name);
        radioGroup = dialog.findViewById(R.id.radio);
        saveButton = dialog.findViewById(R.id.save_button);
    }

    private void setupCloseButton(Dialog dialog) {
        ImageView closeButton = dialog.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> finish());
    }

    private void setupSaveButton(Dialog dialog) {
        saveButton.setOnClickListener(view -> onSaveButtonClicked(dialog));
    }

    private void onSaveButtonClicked(Dialog dialog) {
        String inputDeviceId = deviceId.getText().toString();
        String inputDeviceOtp = deviceOtp.getText().toString();
        String inputDeviceName = deviceName.getText().toString();
        int selectedId = radioGroup.getCheckedRadioButtonId();

        // check if radio button is selected
        if (selectedId != -1) {
            radioButton = dialog.findViewById(selectedId);
            if (radioButton == null) {
                showToast("Error: Radio Button");
            } else {
                // obtain the device type from radio button and convert to string
                String inputDeviceType = radioButton.getText().toString();

                // check if other fields are filled in
                if (isInputValid(inputDeviceId, inputDeviceOtp, inputDeviceName, inputDeviceType)) {
                    /* perform a check if the device id and type matches
                    with generated device id from wearable **/
                    checkDevIdType(inputDeviceId, inputDeviceOtp, inputDeviceType);
                } else {
                    showToast("Please fill out all the fields");
                }
            }
        } else {
            showToast("Please fill out all the fields");
        }
    }

    private boolean isInputValid(String inputDeviceId, String inputDeviceOtp, String inputDeviceName, String inputDeviceType) {
        return !(inputDeviceId.isEmpty() || inputDeviceOtp.isEmpty() || inputDeviceName.isEmpty() || inputDeviceType.isEmpty());
    }



    private void checkDevDup() {
        Task<QuerySnapshot> deviceDup = devListRef
                .whereEqualTo("deviceId", deviceId.getText().toString())
                .whereEqualTo("deviceType", radioButton.getText().toString())
                .get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        handleDevDup(task1.getResult());
                    } else {
                        logError("Error getting documents", task1.getException());
                    }
                });
    }

    private void checkDevIdType(String inputDeviceId, String inputDeviceOtp, String inputDeviceType) {
        Task<QuerySnapshot> deviceIdTypeQuery = devRef
                .whereEqualTo("deviceId", inputDeviceId)
                .whereEqualTo("deviceOTP", inputDeviceOtp)
                .whereEqualTo("deviceType", inputDeviceType)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handleDevIdType(task.getResult());
                    } else {
                        logError("Error getting documents", task.getException());
                    }
                });
    }

    private void handleDevIdType(QuerySnapshot querySnapshot) {
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            // if snapshot exists, check if device ID already registered under other user
            checkDevDup();
        } else {
            // if snapshot does not exist
            showToast("Incorrect input, please check again.");
        }
    }

    private void handleDevDup(QuerySnapshot querySnapshot) {
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            showToast("Existing registered device. Kindly check Device ID and Device Type.");
        } else {
            registerNewDevice();
        }
    }

    private void registerNewDevice() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String userEmail = currentUser.getEmail();
        addDevice newDevice = new addDevice(userEmail,
                deviceId.getText().toString(),
                radioButton.getText().toString(),
                deviceName.getText().toString());

        DocumentReference newReference = devListRef.document();
        newReference.set(newDevice)
                .addOnSuccessListener(unused -> {
                    showToast("Device registered successfully.");
                    //start listening to db service
                    startService(new Intent(this, ListenerDBService.class));
                    finish();
                });

    }



    private void showToast(String message) {
        Toast.makeText(DeviceReg.this, message, Toast.LENGTH_SHORT).show();
    }

    private void logError(String message, Exception exception) {
        Log.d(TAG, message + ": " + exception.getMessage());
        showToast("Error: " + exception.getMessage());
    }

    private void setupDialogProperties(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

}

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // Instead of setting content view, use a dialog for the layout
//        final Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.activity_device_reg);
//
//        // Find views inside the dialog layout
//        deviceId = dialog.findViewById(R.id.device_id);
//        deviceName = dialog.findViewById(R.id.device_name);
//        radioGroup = dialog.findViewById(R.id.radio);
//        saveButton = dialog.findViewById(R.id.save_button);
//
//        // Close button setup
//        // Close button setup
//        ImageView closeButton = dialog.findViewById(R.id.close_button);
//        closeButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish(); // Finish the DeviceReg activity
//            }
//        });
//
//
//        saveButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String inputDeviceId = deviceId.getText().toString();
//                String inputDeviceName = deviceName.getText().toString();
//                int selectedId = radioGroup.getCheckedRadioButtonId();
//                if (selectedId != -1) {
//                    radioButton = dialog.findViewById(selectedId); // Use dialog.findViewById()
//                    if (radioButton == null) {
//                        Toast.makeText(DeviceReg.this, "Error: Radio Button", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                }
//                radioButton = dialog.findViewById(selectedId); // Use dialog.findViewById()
//                String inputDeviceType = radioButton.getText().toString();
//
//                // Check if userInput is not empty
//                if (!(inputDeviceId.isEmpty() || inputDeviceName.isEmpty() || inputDeviceType.isEmpty())) {
//                    // Get current authenticated user
//                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//
//                    // Check id and type
//                    Task<QuerySnapshot> deviceIdTypeQuery = devRef
//                            .whereEqualTo("deviceId", inputDeviceId)
//                            .whereEqualTo("deviceType", inputDeviceType)
//                            .get()
//                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                                @Override
//                                public void onComplete(Task<QuerySnapshot> task) {
//                                    if (task.isSuccessful()) {
//                                        QuerySnapshot querySnapshot = task.getResult();
//                                        // id and type matches
//                                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
//                                            Task<QuerySnapshot> deviceDup = devListRef
//                                                    .whereEqualTo("deviceId", inputDeviceId)
//                                                    .whereEqualTo("deviceType", inputDeviceType)
//                                                    .get()
//                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                                                        @Override
//                                                        public void onComplete(@NonNull Task<QuerySnapshot> task1) {
//                                                            if (task1.isSuccessful()) {
//                                                                QuerySnapshot querySnapshot1 = task1.getResult();
//                                                                // id and type already in use
//                                                                if (querySnapshot1 != null && !querySnapshot1.isEmpty()) {
//                                                                    Toast.makeText(DeviceReg.this, "Existing registered device. Kindly check Device ID and Device Type.", Toast.LENGTH_SHORT).show();
//                                                                } else {
//                                                                    String userEmail = currentUser.getEmail();
//                                                                    addDevice newDevice = new addDevice(userEmail, inputDeviceId, inputDeviceType, inputDeviceName);
//                                                                    DocumentReference newReference = devListRef.document();
//                                                                    newReference.set(newDevice)
//                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                                                @Override
//                                                                                public void onSuccess(Void unused) {
//                                                                                    Toast.makeText(DeviceReg.this, "Device registered successfully.", Toast.LENGTH_SHORT).show();
//                                                                                    finish(); // Finish the activity
//                                                                                }
//                                                                            });
//                                                                }
//                                                            } else {
//                                                                Log.d("DeviceReg", "Error getting documents: " + task1.getException());
//                                                                Toast.makeText(DeviceReg.this, "Error: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                                                            }
//                                                        }
//                                                    });
//                                        } else {
//                                            Toast.makeText(DeviceReg.this, "Incorrect Device ID and Type.", Toast.LENGTH_SHORT).show();
//                                        }
//                                    } else {
//                                        Log.d("DeviceReg", "Error getting documents: " + task.getException());
//                                        Toast.makeText(DeviceReg.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                                    }
//                                }
//                            });
//                } else {
//                    Toast.makeText(DeviceReg.this, "Empty field", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//
//
//        // Set dialog properties
//        Window window = dialog.getWindow();
//        if (window != null) {
//            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
//            window.setBackgroundDrawableResource(android.R.color.transparent); // Set background to null
//        }
//
//        dialog.show(); // Show the dialog
//    }


