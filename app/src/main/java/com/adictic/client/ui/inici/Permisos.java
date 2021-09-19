package com.adictic.client.ui.inici;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.text.LineBreaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.adictic.client.util.Funcions;
import com.adictic.client.R;
import com.adictic.common.util.Constants;
import com.judemanutd.autostarter.AutoStartPermissionHelper;

public class Permisos extends AppCompatActivity {
    private boolean accessibilityPerm, usagePerm, adminPerm, overlayPerm, locationPerm, batteryPerm, autostartPerm;
    private final String TAG = "Permisos";

    ActivityResultLauncher<Intent> activityResult = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK) {
                TextView TV_permiso_admin_status = findViewById(R.id.TV_permiso_admin_status);
                if (Funcions.isAdminPermissionsOn(Permisos.this)) {
                    adminPerm = true;
                    TV_permiso_admin_status.setText(getText(R.string.activat));
                    TV_permiso_admin_status.setTextColor(Color.GREEN);
                }
                else{
                    TV_permiso_admin_status.setText(getText(R.string.desactivat));
                    TV_permiso_admin_status.setTextColor(getColor(R.color.vermell));
                }
            }
        }
    );

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permissions_layout);

        TextView TV_permisosDesc = findViewById(R.id.TV_permisos_desc);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            TV_permisosDesc.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);

        Button BT_finish = findViewById(R.id.BT_permisos_continuar);
        BT_finish.setOnClickListener(view -> {
            if(totsPermisosActivats())
                acabarActivitat();
            else{
                new androidx.appcompat.app.AlertDialog.Builder(Permisos.this)
                        .setTitle(getString(R.string.falten_perm_titol))
                        .setMessage(getString(R.string.falten_perm_desc))
                        .setPositiveButton(getString(R.string.accept), (dialogInterface, i) -> acabarActivitat())
                        .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                        .show();
            }
        });
    }

    private void acabarActivitat(){
        if(usagePerm) {
            SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(Permisos.this);
            assert  sharedPreferences != null;

            sharedPreferences.edit().putInt(Constants.SHARED_PREFS_DAYS_TO_SEND_DATA, 6).apply();

            Funcions.startAppUsageWorker24h(Permisos.this);
        }

        if(Funcions.isMIUI())
            Permisos.this.startActivity(new Intent(Permisos.this, PermisosMIUI.class));
        else
            Permisos.this.startActivity(new Intent(Permisos.this, AppLock.class));
        Permisos.this.finish();
    }
    
    private boolean totsPermisosActivats(){
        return accessibilityPerm && usagePerm && adminPerm && overlayPerm && locationPerm && batteryPerm && autostartPerm;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setAccessibilityLayout();
        setUsageLayout();
        setAdminLayout();
        setOverlayLayout();
        setLocationLayout();
        setBatteryLayout();
        setAutoStartLayout();
    }

    private void setAutoStartLayout() {
        ConstraintLayout CL_auto_start = findViewById(R.id.CL_auto_start);
        TextView TV_permiso_auto_start_status = findViewById(R.id.TV_permiso_auto_start_status);
        boolean autoStartAvailable = AutoStartPermissionHelper.Companion.getInstance().isAutoStartPermissionAvailable(this, true);

        if (!autoStartAvailable) {
            CL_auto_start.setVisibility(View.GONE);
            autostartPerm = true;
        }
        else {
            CL_auto_start.setOnClickListener(view -> new AlertDialog.Builder(Permisos.this)
                    .setTitle(getString(R.string.auto_start))
                    .setMessage(getString(R.string.auto_start_info))
                    .setPositiveButton(getString(R.string.configurar), (dialogInterface, i) -> {
                        boolean autoStartSuccess = AutoStartPermissionHelper.Companion.getInstance().getAutoStartPermission(this,true, false);
                        if(autoStartSuccess){
                            TV_permiso_auto_start_status.setText(getText(R.string.activat));
                            TV_permiso_auto_start_status.setTextColor(Color.GREEN);
                            autostartPerm = true;
                        }
                        else
                            autostartPerm = false;
                    })
                    .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                    .show());
        }
    }

    private void setBatteryLayout() {
        batteryPerm = false;
        ConstraintLayout CL_battery_optimisation = findViewById(R.id.CL_battery_optimisation);
        TextView TV_permiso_battery_optimisation_status = findViewById(R.id.TV_permiso_battery_optimisation_status);
        PowerManager mPm = (PowerManager) getSystemService(POWER_SERVICE);

        boolean ignoringBatteryOptimizations = mPm.isIgnoringBatteryOptimizations(getPackageName());
        if (ignoringBatteryOptimizations) {
            batteryPerm = true;
            TV_permiso_battery_optimisation_status.setText(getText(R.string.desactivat));
            TV_permiso_battery_optimisation_status.setTextColor(Color.GREEN);
        }
        else{
            TV_permiso_battery_optimisation_status.setText(getText(R.string.activat));
            TV_permiso_battery_optimisation_status.setTextColor(getColor(R.color.vermell));
        }

        CL_battery_optimisation.setOnClickListener(view -> new AlertDialog.Builder(Permisos.this)
                .setTitle(getString(R.string.battery_optimisation))
                .setMessage(getString(R.string.battery_optimisation_info))
                .setPositiveButton(getString(R.string.configurar), (dialogInterface, i) -> {
                    @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName()));
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .show());
    }

    private void setLocationLayout() {
        locationPerm = false;
        ConstraintLayout CL_location = findViewById(R.id.CL_location);
        TextView TV_permiso_location_status = findViewById(R.id.TV_permiso_location_status);

        if (Funcions.isBackgroundLocationPermissionOn(Permisos.this)) {
            locationPerm = true;
            TV_permiso_location_status.setText(getText(R.string.activat));
            TV_permiso_location_status.setTextColor(Color.GREEN);
        }
        else{
            TV_permiso_location_status.setText(getText(R.string.desactivat));
            TV_permiso_location_status.setTextColor(getColor(R.color.vermell));
        }

        CL_location.setOnClickListener(view -> new AlertDialog.Builder(Permisos.this)
                .setTitle(getString(R.string.perm_background_loc_title))
                .setMessage(getString(R.string.perm_back_location_desc))
                .setPositiveButton(getString(R.string.configurar), (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse("package:" + getPackageName()));
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .show());
    }

    private void setOverlayLayout() {
        overlayPerm = false;
        ConstraintLayout CL_overlay = findViewById(R.id.CL_overlay);
        TextView TV_permiso_overlay_status = findViewById(R.id.TV_permiso_overlay_status);

        if (Settings.canDrawOverlays(this)) {
            overlayPerm = true;
            TV_permiso_overlay_status.setText(getText(R.string.activat));
            TV_permiso_overlay_status.setTextColor(Color.GREEN);
        }
        else{
            TV_permiso_overlay_status.setText(getText(R.string.desactivat));
            TV_permiso_overlay_status.setTextColor(getColor(R.color.vermell));
        }

        CL_overlay.setOnClickListener(view -> new AlertDialog.Builder(Permisos.this)
                .setTitle(getString(R.string.overlay_title))
                .setMessage(getString(R.string.overlay_info))
                .setPositiveButton(getString(R.string.configurar), (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .show());
    }

    private void setAdminLayout() {
        adminPerm = false;
        ConstraintLayout CL_admin = findViewById(R.id.CL_admin);
        TextView TV_permiso_admin_status = findViewById(R.id.TV_permiso_admin_status);

        if (Funcions.isAdminPermissionsOn(this)) {
            adminPerm = true;
            TV_permiso_admin_status.setText(getText(R.string.activat));
            TV_permiso_admin_status.setTextColor(Color.GREEN);
        }
        else{
            TV_permiso_admin_status.setText(getText(R.string.desactivat));
            TV_permiso_admin_status.setTextColor(getColor(R.color.vermell));
        }

        CL_admin.setOnClickListener(view -> new AlertDialog.Builder(Permisos.this)
                .setTitle(getString(R.string.admin_pem_title))
                .setMessage(getString(R.string.admin_pem_info))
                .setPositiveButton(getString(R.string.configurar), (dialogInterface, i) -> {
                    ComponentName mDPAdmin = new ComponentName(Permisos.this,
                            MyDevicePolicyReceiver.class);
                    Intent intent = new Intent(
                            DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(
                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            mDPAdmin);
                    intent.putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            getString(R.string.admin_pem_intent));
                    activityResult.launch(intent);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .show());
    }

    private void setUsageLayout() {
        usagePerm = false;
        ConstraintLayout CL_usage = findViewById(R.id.CL_usage);
        TextView TV_permiso_usage_status = findViewById(R.id.TV_permiso_usage_status);

        if (Funcions.isAppUsagePermissionOn(this)) {
            usagePerm = true;
            TV_permiso_usage_status.setText(getText(R.string.activat));
            TV_permiso_usage_status.setTextColor(Color.GREEN);
        }
        else{
            TV_permiso_usage_status.setText(getText(R.string.desactivat));
            TV_permiso_usage_status.setTextColor(getColor(R.color.vermell));
        }

        CL_usage.setOnClickListener(view -> new AlertDialog.Builder(Permisos.this)
                .setTitle(getString(R.string.appusage_pem_title))
                .setMessage(getString(R.string.appusage_pem_info))
                .setPositiveButton(getString(R.string.configurar), (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .show());
    }

    private void setAccessibilityLayout() {
        accessibilityPerm = false;
        ConstraintLayout CL_accessibility = findViewById(R.id.CL_accessibility);
        TextView TV_accessibility_status = findViewById(R.id.TV_permiso_accessibility_status);

        if (Funcions.isAccessibilitySettingsOn(this)) {
            accessibilityPerm = true;
            TV_accessibility_status.setText(getText(R.string.activat));
            TV_accessibility_status.setTextColor(Color.GREEN);
        }
        else{
            TV_accessibility_status.setText(getText(R.string.desactivat));
            TV_accessibility_status.setTextColor(getColor(R.color.vermell));
        }

        CL_accessibility.setOnClickListener(view -> new AlertDialog.Builder(Permisos.this)
                .setTitle(getString(R.string.accessibility_pem_title))
                .setMessage(getString(R.string.accessibility_pem_info))
                .setPositiveButton(getString(R.string.configurar), (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .show());
    }

    public static class MyDevicePolicyReceiver extends DeviceAdminReceiver {
        @Override
        public void onDisabled(Context context, Intent intent) {
            Toast.makeText(context, "AdicTIC's Device Admin Disabled",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEnabled(Context context, Intent intent) {
            Toast.makeText(context, "AdicTIC's Device Admin is now enabled",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            return "Requesting to disable Device Admin";
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("MyDevicePolicyReciever",
                    "Received: " + intent.getAction());
            super.onReceive(context, intent);
        }


    }
}
