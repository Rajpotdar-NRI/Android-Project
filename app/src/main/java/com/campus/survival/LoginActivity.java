package com.campus.survival;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends BaseActivity {

    private EditText etName, etEmail, etPassword, etConfirm;
    private TextView tvMode, tvSwitch;
    private Button btnSubmit;
    private boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Prefs.isLoggedIn(this)) {   // already logged in -> skip this screen
            openDashboard();
            return;
        }
        setContentView(R.layout.activity_login);
        // Orange status/navigation bars with white icons so the glass screen looks edge-to-edge
        getWindow().setStatusBarColor(getResources().getColor(R.color.orange, null));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.orange_deep, null));
        getWindow().getDecorView().setSystemUiVisibility(0);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        tvMode = findViewById(R.id.tvMode);
        tvSwitch = findViewById(R.id.tvSwitch);
        btnSubmit = findViewById(R.id.btnSubmit);

        CheckBox cbShow = findViewById(R.id.cbShow);
        cbShow.setOnCheckedChangeListener((b, show) -> {
            int type = InputType.TYPE_CLASS_TEXT |
                    (show ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setInputType(type);
            etConfirm.setInputType(type);
            etPassword.setSelection(etPassword.getText().length());
        });

        tvSwitch.setOnClickListener(v -> setRegisterMode(!registerMode));
        btnSubmit.setOnClickListener(v -> { if (registerMode) register(); else login(); });
    }

    private void setRegisterMode(boolean on) {
        registerMode = on;
        etName.setVisibility(on ? View.VISIBLE : View.GONE);
        etConfirm.setVisibility(on ? View.VISIBLE : View.GONE);
        tvMode.setText(on ? "Create your account ✨" : "Welcome back 👋");
        btnSubmit.setText(on ? "Create Account" : "Login");
        tvSwitch.setText(on ? "Already have an account? Login" : "New here? Create an account");
        etPassword.setError(null);
        etConfirm.setError(null);
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        if (!validEmail(email) || !validPassword(pass)) return;

        long id = db.login(email, pass);
        if (id == -1) {
            etPassword.setError("Wrong email or password");
            etPassword.requestFocus();
            return;
        }
        Prefs.login(this, id);
        toast("Welcome back, " + db.userName(id) + "!");
        openDashboard();
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        if (name.length() < 2) { etName.setError("Enter your name"); etName.requestFocus(); return; }
        if (!validEmail(email) || !validPassword(pass)) return;
        if (!pass.equals(confirm)) { etConfirm.setError("Passwords don't match"); etConfirm.requestFocus(); return; }
        if (db.emailExists(email)) { etEmail.setError("Account already exists — login instead"); etEmail.requestFocus(); return; }

        long id = db.registerUser(name, email, pass);
        if (id == -1) { toast("Could not create account, try again"); return; }
        Prefs.login(this, id);
        toast("Account created. Let's survive this semester 💪");
        openDashboard();
    }

    private boolean validEmail(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validPassword(String pass) {
        if (pass.length() < 6) {
            etPassword.setError("At least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void openDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // so Back from the dashboard exits the app instead of returning here
    }
}
