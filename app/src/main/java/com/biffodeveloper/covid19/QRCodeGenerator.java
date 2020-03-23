package com.biffodeveloper.covid19;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.WriterException;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;


public class QRCodeGenerator extends AppCompatActivity {
    private String operation;
    private FirebaseUser mUser;
    private FirebaseFirestore db;
    private RelativeLayout loading, no_more_lyt, qrcode_lyt;
    private TextView no_more_entrances_tv, heresthecode_tv, remaining_accesses_tv;
    private Map<String, Object> userMap;
    private ImageView qrcode_img;
    private ListenerRegistration listenerRegistration;
    private long timesRequired;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_q_r_code_generator);

        operation = getIntent().getStringExtra("operation");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        loading = findViewById(R.id.loading);
        no_more_lyt = findViewById(R.id.no_more_lyt);
        no_more_entrances_tv = findViewById(R.id.no_more_entrances_tv);
        Button back_btn = findViewById(R.id.back_btn);
        qrcode_img = findViewById(R.id.qrcode_img);
        qrcode_lyt = findViewById(R.id.qrcode_lyt);
        heresthecode_tv = findViewById(R.id.heresthecode_tv);
        remaining_accesses_tv = findViewById(R.id.remaining_accesses_tv);
        Button back_btn_2 = findViewById(R.id.back_btn_2);
        View.OnClickListener finishListener = view -> finish();
        back_btn.setOnClickListener(finishListener);
        back_btn_2.setOnClickListener(finishListener);
        checkPerm();
    }
    private void checkPerm() {
        db.collection("users").document(mUser.getUid()).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document != null && document.exists()) {
                    userMap = document.getData();
                    timesRequired = (long) Objects.requireNonNull(document.getData()).get(operation);
                    if(timesRequired > 0) {
                        try {
                            showQRCode();
                        } catch (WriterException e) {
                            Toast.makeText(QRCodeGenerator.this, "Qualcosa è andato storto, si prega di riprovare", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    else {
                        noMore();
                    }
                }
                else {
                    Toast.makeText(QRCodeGenerator.this, "L'utente richiesto non è stato trovato", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(QRCodeGenerator.this, "Qualcosa è andato storto, si prega di riprovare", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    public String translateOperation() {
        switch (operation) {
            case "supermarket":
                return "al supermercato";
            case "drugstore":
                return "in farmacia";
            case "tobacconist":
                return "dal tabaccaio";
            default:
                return "";
        }
    }
    public void noMore() {
        no_more_entrances_tv.setText(getResources().getString(R.string.no_more_entrances, translateOperation()));
        no_more_lyt.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }
    public void showQRCode() throws WriterException {
        listenerRegistration = db.collection("users").document(mUser.getUid()).addSnapshotListener((documentSnapshot, e) -> {
            if(e != null) {
                Toast.makeText(QRCodeGenerator.this, "Qualcosa è andato storto, si prega di riprovare", Toast.LENGTH_SHORT).show();
            }
            if(documentSnapshot != null && documentSnapshot.exists()) {
                db.collection("users").document(mUser.getUid()).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        long gotTimes = (long) Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getData()).get(operation);
                        if(gotTimes == timesRequired - 1) {
                            detachListener();
                            Toast.makeText(QRCodeGenerator.this, "Validato", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
            }
            else {
                Toast.makeText(QRCodeGenerator.this, "Qualcosa è andato storto, si prega di riprovare", Toast.LENGTH_SHORT).show();
            }
        });
        heresthecode_tv.setText(getString(R.string.heresthecode, translateOperation()));
        qrcode_img.setImageBitmap(generateQRCode());
        remaining_accesses_tv.setText(getString(R.string.remaining_accesses, timesRequired));
        qrcode_lyt.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }
    public void detachListener() {
        if(listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
    public Bitmap generateQRCode() throws WriterException {
        String uid = mUser.getUid();
        String name = (String) userMap.get("name");
        String surname = (String) userMap.get("surname");
        Date birthdate = ((Timestamp) Objects.requireNonNull(userMap.get("birthdate"))).toDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(birthdate);
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        long time = (long) userMap.get(operation);
        String code = uid + "/" + name + "/" + surname + "/" + calendar.get(Calendar.DAY_OF_MONTH) + "_" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.YEAR) + "/" + operation + "/" + time;
        QRGEncoder qrgEncoder = new QRGEncoder(code, null, QRGContents.Type.TEXT, 700);
        return qrgEncoder.encodeAsBitmap();
    }
}
