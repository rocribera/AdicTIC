package com.example.adictic.ui.permisos;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

import com.example.adictic.R;
import com.example.adictic.ui.main.NavActivity;
import com.example.adictic.util.Funcions;

public class AccessibilityPermActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accessibility_perm_info);

        Button bt_okay = findViewById(R.id.BT_okAccessibilityPerm);

        if (Funcions.isAccessibilitySettingsOn(this)) {
            this.startActivity(new Intent(this, NavActivity.class));
            this.finish();
        }

        bt_okay.setOnClickListener(v -> AccessibilityPermActivity.this.startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0));
    }

    @Override
    protected void onResume() {

        if (Funcions.isAccessibilitySettingsOn(this)) {
            this.startActivity(new Intent(this, NavActivity.class));
            this.finish();
        }

        super.onResume();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            if (Funcions.isAccessibilitySettingsOn(this)) {
                this.startActivity(new Intent(this, NavActivity.class));
                this.finish();
            }
        }
    }
}
