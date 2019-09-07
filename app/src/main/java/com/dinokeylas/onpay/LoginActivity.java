package com.dinokeylas.onpay;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dinokeylas.onpay.model.UserModel;
import com.dinokeylas.onpay.util.Encript;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import static com.dinokeylas.onpay.util.Constant.COLLECTION.COLLECTION_USER;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    Intent intent = new Intent();
    EditText editTextEmail, editTextPassword;
    ProgressBar progress_bar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String email;
    private int flag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        progress_bar = (ProgressBar) findViewById(R.id.progressbar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.buttonLogin).setOnClickListener(this);
        findViewById(R.id.textViewSignup).setOnClickListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //check is login or not
        if (mAuth.getCurrentUser() != null) {
            navigateToHomeActivity();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonLogin:
                verify();
                break;
            case R.id.textViewSignup:
                intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void verify() {
        email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty()) {
            editTextEmail.setError("Email tidak boleh kosong");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Format email tidak valid");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Sandi tidak boleh kosong");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Panjang sandi minimal 6");
            editTextPassword.requestFocus();
            return;
        }

        //encrypted password
        Encript encript = new Encript(password);
        password = encript.getEcripted();

        progress_bar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    login();
                } else {
                    progress_bar.setVisibility(View.GONE);
                    if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                        Toast.makeText(getApplicationContext(), "Email dan kata sandi tidak sesuai", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    //check is user or not
    public void login() {
        db.collection(COLLECTION_USER).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        if (!documentSnapshots.isEmpty()) {
                            progress_bar.setVisibility(View.GONE);
                            List<DocumentSnapshot> list = documentSnapshots.getDocuments();
                            for (DocumentSnapshot d : list) {
                                UserModel p = d.toObject(UserModel.class);
                                assert p != null;
                                if (p.getEmailAddress().equalsIgnoreCase(email)) {
                                    flag = 1;
                                    break;
                                }
                            }
                            if (flag == 1) {
                                navigateToHomeActivity();
                            } else {
                                mAuth.signOut();
                                AlertDialog alert = new AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Peringatan")
                                        .setMessage("Anda bukan terdaftar sebagai pengguna")
                                        .setPositiveButton("mengerti", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .show();
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, "Anda Belum Terdaftar Sebagai Petugas Lapangan", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHomeActivity() {
        intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
