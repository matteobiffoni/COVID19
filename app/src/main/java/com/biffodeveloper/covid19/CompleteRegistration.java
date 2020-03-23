package com.biffodeveloper.covid19;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CompleteRegistration extends AppCompatActivity {
    private EditText name_et, surname_et;
    private DatePicker birthdate_dp;
    private TextView not_acceptable_date;
    private Button confirm_btn;
    private RelativeLayout loading;
    private boolean dateOK, nameOK, surnameOK;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_registration);

        AndroidThreeTen.init(this);
        name_et = findViewById(R.id.name_et);
        surname_et = findViewById(R.id.surname_et);
        birthdate_dp = findViewById(R.id.birthdate_dp);
        confirm_btn = findViewById(R.id.confirm_btn);
        not_acceptable_date = findViewById(R.id.not_acceptable_date);
        loading = findViewById(R.id.loading);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        birthdate_dp.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), (datePicker, i, i1, i2) -> {
            dismissKeyboard();
            int day = datePicker.getDayOfMonth();
            int month = datePicker.getMonth();
            int year = datePicker.getYear();
            if(getAge(year, month + 1, day) >= 18) {
                not_acceptable_date.setVisibility(View.GONE);
                dateOK = true;
                if(nameOK && surnameOK) {
                    confirm_btn.setVisibility(View.VISIBLE);
                }
                else {
                    confirm_btn.setVisibility(View.GONE);
                }
            }
            else {
                not_acceptable_date.setVisibility(View.VISIBLE);
                dateOK = false;
                confirm_btn.setVisibility(View.GONE);
            }
        });
        name_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(!name_et.getText().toString().isEmpty() && name_et.getText().toString().length() > 0) {
                    nameOK = true;
                    if(surnameOK && dateOK) {
                        confirm_btn.setVisibility(View.VISIBLE);
                    }
                    else {
                        confirm_btn.setVisibility(View.GONE);
                    }
                }
                else {
                    nameOK = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        surname_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(!surname_et.getText().toString().isEmpty() && surname_et.getText().toString().length() > 0) {
                    surnameOK = true;
                    if(nameOK && dateOK) {
                        confirm_btn.setVisibility(View.VISIBLE);
                    }
                    else {
                        confirm_btn.setVisibility(View.GONE);
                    }
                }
                else {
                    surnameOK = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        confirm_btn.setOnClickListener(view -> {
            loading.setVisibility(View.VISIBLE);
            updateUser();
        });
    }
    public void updateUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("phone", Objects.requireNonNull(Objects.requireNonNull(user).getPhoneNumber()));
        userMap.put("name", name_et.getText().toString());
        userMap.put("surname", surname_et.getText().toString());
        Calendar calendar = Calendar.getInstance();
        calendar.set(birthdate_dp.getYear(), birthdate_dp.getMonth(), birthdate_dp.getDayOfMonth());
        Timestamp birthdate = new Timestamp(calendar.getTime());
        userMap.put("birthdate", birthdate);
        userMap.put("supermarket", 2);
        userMap.put("drugstore", 1);
        userMap.put("tobacconist", 1);
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Timestamp valid_to = new Timestamp(calendar.getTime());
        userMap.put("valid_to", valid_to);
        db.collection("users").document(user.getUid()).set(userMap).addOnSuccessListener(aVoid -> {
            Toast.makeText(CompleteRegistration.this, "Registrazione effettuata con successo", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CompleteRegistration.this, HomeActivity.class);
            startActivity(intent);
        }).addOnFailureListener(e -> {
            Toast.makeText(CompleteRegistration.this, "Qualcosa è andato storto, si prega di riprovare", Toast.LENGTH_SHORT).show();
            loading.setVisibility(View.GONE);
        });
    }
    public int getAge(int year, int month, int day) {
        LocalDate birthdate = LocalDate.of(year, month, day);
        LocalDate now = LocalDate.now();
        return Period.between(birthdate, now).getYears();
    }
    public void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) CompleteRegistration.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != CompleteRegistration.this.getCurrentFocus())
            imm.hideSoftInputFromWindow(CompleteRegistration.this.getCurrentFocus()
                    .getApplicationWindowToken(), 0);
    }
}
