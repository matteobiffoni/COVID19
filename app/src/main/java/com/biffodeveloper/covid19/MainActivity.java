package com.biffodeveloper.covid19;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private EditText phone_number_et, code_et;
    private Button verify_phone_btn;
    private Button verify_code_btn;
    private RelativeLayout insert_phone_lyt, insert_code_lyt, loading;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    PhoneAuthCredential credentials;
    private String verificationID;
    private String code;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phone_number_et = findViewById(R.id.phone_number_et);
        verify_phone_btn = findViewById(R.id.verify_phone_btn);
        code_et = findViewById(R.id.code_et);
        verify_code_btn = findViewById(R.id.verify_code_btn);
        insert_phone_lyt = findViewById(R.id.insert_phone_lyt);
        insert_code_lyt = findViewById(R.id.insert_code_lyt);
        loading = findViewById(R.id.loading);
        Button back_btn = findViewById(R.id.back_btn);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
        }
        else {
            loading.setVisibility(View.GONE);
        }
        phone_number_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(validatePhoneNumber(phone_number_et.getText().toString())) {
                    verify_phone_btn.setVisibility(View.VISIBLE);
                }
                else {
                    verify_phone_btn.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        code_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(validateCode(code_et.getText().toString())) {
                    verify_code_btn.setVisibility(View.VISIBLE);
                    code = code_et.getText().toString();
                }
                else {
                    verify_code_btn.setVisibility(View.GONE);
                    code = "";
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        phone_number_et.setOnKeyListener((view, i, keyEvent) -> {
            if(keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(validatePhoneNumber(phone_number_et.getText().toString())) {
                    dismissKeyboard();
                    verifyPhoneNumber();
                }
            }
            return false;
        });
        code_et.setOnKeyListener((view, i, keyEvent) -> {
            if(keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(validateCode(code_et.getText().toString())) {
                    dismissKeyboard();
                    credentials = PhoneAuthProvider.getCredential(verificationID, code);
                    signInWithPhoneAuthCredential();
                }
            }
            return false;
        });
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                credentials = phoneAuthCredential;
                signInWithPhoneAuthCredential();
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(MainActivity.this, "Fallito", Toast.LENGTH_SHORT).show();
                loading.setVisibility(View.GONE);
                insert_phone_lyt.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                Toast.makeText(MainActivity.this, "Codice inviato", Toast.LENGTH_SHORT).show();
                verificationID = s;
                MainActivity.this.enableUserCodeInput();
            }
        };
        verify_phone_btn.setOnClickListener(view -> {
            dismissKeyboard();
            verifyPhoneNumber();
        });
        verify_code_btn.setOnClickListener(view -> {
            dismissKeyboard();
            credentials = PhoneAuthProvider.getCredential(verificationID, code);
            signInWithPhoneAuthCredential();
        });
        back_btn.setOnClickListener(view -> {
            insert_code_lyt.setVisibility(View.GONE);
            insert_phone_lyt.setVisibility(View.VISIBLE);
        });
    }
    public void verifyPhoneNumber() {
        insert_phone_lyt.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phone_number_et.getText().toString(), 60, TimeUnit.SECONDS, MainActivity.this, mCallbacks);
    }
    public boolean validatePhoneNumber(String number) {
        return number.matches("^([+]39)?((38[{8,9}|0])|(34[{7-9}|0])|(36[6|8|0])|(33[{3-9}|0])|(32[{8,9}]))([\\d]{7})$");
    }
    public boolean validateCode(String code) {
        if(code == null || code.isEmpty()) return false;
        try {
            Integer.parseInt(code);
        } catch(NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }
    public void enableUserCodeInput() {
        loading.setVisibility(View.GONE);
        insert_code_lyt.setVisibility(View.VISIBLE);
    }
    public void signInWithPhoneAuthCredential() {
        insert_code_lyt.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credentials).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Accesso avvenuto con successo", Toast.LENGTH_SHORT).show();
                boolean isNewUser = Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getAdditionalUserInfo()).isNewUser();
                Intent intent;
                if(isNewUser) {
                    intent = new Intent(MainActivity.this, CompleteRegistration.class);
                }
                else {
                    intent = new Intent(MainActivity.this, HomeActivity.class);
                }
                startActivity(intent);
            }
            else {
                Toast.makeText(MainActivity.this, "Errore: accesso non eseguito", Toast.LENGTH_SHORT).show();
                loading.setVisibility(View.GONE);
                insert_code_lyt.setVisibility(View.VISIBLE);
            }
        });
    }
    public void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != MainActivity.this.getCurrentFocus())
            imm.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus()
                    .getApplicationWindowToken(), 0);
    }
}
