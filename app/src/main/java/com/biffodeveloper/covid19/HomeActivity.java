package com.biffodeveloper.covid19;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.instacart.library.truetime.TrueTimeRx;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity {
    @SuppressWarnings("unused")
    private final String TAG = "HomeActivity";
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore firebaseFirestore;
    private TextView name_tv;
    private Button logout_btn, drugstore_btn, supermarket_btn, tobacconist_btn;
    private Map<String, Object> userMap;
    private RelativeLayout loading, ui_home;


    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        name_tv = findViewById(R.id.name_tv);
        logout_btn = findViewById(R.id.logout_btn);
        loading = findViewById(R.id.loading);
        ui_home = findViewById(R.id.ui_home);
        drugstore_btn = findViewById(R.id.drugstore_btn);
        supermarket_btn = findViewById(R.id.supermarket_btn);
        tobacconist_btn = findViewById(R.id.tobacconist_btn);
        logout_btn.setOnClickListener(view -> signOut());
        class ListenerSelector implements View.OnClickListener {
            private String op;
            private ListenerSelector(String op) {
                this.op = op;
            }
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, QRCodeGenerator.class);
                intent.putExtra("operation", op);
                startActivity(intent);
            }
        }
        drugstore_btn.setOnClickListener(new ListenerSelector("drugstore"));
        supermarket_btn.setOnClickListener(new ListenerSelector("supermarket"));
        tobacconist_btn.setOnClickListener(new ListenerSelector("tobacconist"));
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection("users").document(mUser.getUid()).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document != null && document.exists()) {
                    userMap = document.getData();
                    if(runningOnEmulator()) {
                        updateUI();
                        return;
                    }
                    //noinspection ResultOfMethodCallIgnored
                    TrueTimeRx.build()
                            .initializeRx("time.google.com")
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    date -> HomeActivity.this.runOnUiThread(() -> {
                                        Date valid_to = ((Timestamp) Objects.requireNonNull(userMap.get("valid_to"))).toDate();
                                        if(valid_to.before(date)) {
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.setTime(date);
                                            calendar.add(Calendar.DAY_OF_YEAR, 7);
                                            Timestamp new_valid_to = new Timestamp(calendar.getTime());
                                            userMap.put("valid_to", new_valid_to);
                                            userMap.put("supermarket", 2);
                                            userMap.put("drugstore", 1);
                                            userMap.put("tobacconist", 1);
                                            firebaseFirestore.collection("users")
                                                             .document(mUser.getUid())
                                                             .set(userMap)
                                                             .addOnSuccessListener(aVoid -> updateUI())
                                                             .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, "Aggiornamento settimanale fallito, si prega di chiudere l'app e riprovare", Toast.LENGTH_SHORT).show());
                                        }
                                        else {
                                            updateUI();
                                        }
                                    }),
                                    Throwable::printStackTrace
                            );
                }
                else {
                    Toast.makeText(HomeActivity.this, "Non è stato trovato alcun utente", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(HomeActivity.this, "Qualcosa è andato storto", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent);
    }
    public void updateUI() {
        String name = (String) userMap.get("name");
        String surname = (String) userMap.get("surname");
        name_tv.setText(name + " " + surname);
        ui_home.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }
    public boolean runningOnEmulator() {
        boolean result = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion"));
        if(result) return true;
        result = Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic");
        if(result) return true;
        result = Build.PRODUCT.equals("google_sdk");
        return result;
    }
}
