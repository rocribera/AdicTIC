package com.example.adictic.util;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedFile;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.adictic.common.entity.BlockedLimitedLists;
import com.adictic.common.entity.EventBlock;
import com.adictic.common.entity.EventsAPI;
import com.adictic.common.entity.HorarisAPI;
import com.adictic.common.entity.HorarisNit;
import com.adictic.common.entity.LimitedApps;
import com.adictic.common.rest.Api;
import com.adictic.common.util.Constants;
import com.example.adictic.R;
import com.example.adictic.entity.BlockedApp;
import com.example.adictic.service.WindowChangeDetectingService;
import com.example.adictic.ui.BlockScreenActivity;
import com.example.adictic.workers.AppUsageWorker;
import com.example.adictic.workers.GeoLocWorker;
import com.example.adictic.workers.UpdateTokenWorker;
import com.example.adictic.workers.block_apps.BlockAppWorker;
import com.example.adictic.workers.block_apps.RestartBlockedApps;
import com.example.adictic.workers.event_workers.FinishBlockEventWorker;
import com.example.adictic.workers.event_workers.RestartEventsWorker;
import com.example.adictic.workers.event_workers.StartBlockEventWorker;
import com.example.adictic.workers.horaris_workers.DespertarWorker;
import com.example.adictic.workers.horaris_workers.DormirWorker;
import com.example.adictic.workers.horaris_workers.RestartHorarisWorker;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Funcions extends com.adictic.common.util.Funcions {
    private final static String TAG = "Funcions";

    public static void ensenyarBlockScreenActivity(Context ctx, String lastPackage) {
        // Si és MIUI
        try {
            if(Funcions.isXiaomi())
                addOverlayView(ctx);
            else{
                Log.d(TAG,"Creant Intent cap a BlockScreenActivity");
                Intent lockIntent = new Intent(ctx, BlockScreenActivity.class);
                lockIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                lockIntent.putExtra("pkgName",lastPackage);
                ctx.startActivity(lockIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addOverlayView(Context ctx) {

        final WindowManager.LayoutParams params;
        int layoutParamsType;

        WindowManager windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            layoutParamsType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else {
            layoutParamsType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutParamsType,
                0,
                PixelFormat.OPAQUE);

        params.gravity = Gravity.CENTER | Gravity.START;
        params.x = 0;
        params.y = 0;

        FrameLayout interceptorLayout = new FrameLayout(ctx) {

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {

                // Only fire on the ACTION_DOWN event, or you'll get two events (one for _DOWN, one for _UP)
                if (event.getAction() == KeyEvent.ACTION_DOWN) {

                    // Check if the HOME button is pressed
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                        Log.v(TAG, "BACK Button Pressed");

                        // As we've taken action, we'll return true to prevent other apps from consuming the event as well
                        return true;
                    }
                }

                // Otherwise don't intercept the event
                return super.dispatchKeyEvent(event);
            }
        };

        LayoutInflater inflater = ((LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE));

        if (inflater != null) {
            View floatyView = inflater.inflate(R.layout.block_layout, interceptorLayout);
            windowManager.addView(floatyView, params);

            Button BT_sortir = floatyView.findViewById(R.id.btn_sortir);
            BT_sortir.setOnClickListener(view -> {
                Intent startHomescreen = new Intent(Intent.ACTION_MAIN);
                startHomescreen.addCategory(Intent.CATEGORY_HOME);
                startHomescreen.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(startHomescreen);
                windowManager.removeView(floatyView);
            });
        }
        else {
            Log.e("SAW-example", "Layout Inflater Service is null; can't inflate and display R.layout.floating_view");
        }
    }

    public static void checkEvents(Context ctx) {
        Log.d(TAG,"Check Events");
        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(ctx);

        Api mTodoService = ((AdicticApp) (ctx.getApplicationContext())).getAPI();

        assert sharedPreferences != null;

        // Agafem els horaris de la nit i Events
        Call<EventsAPI> call = mTodoService.getEvents(sharedPreferences.getLong(Constants.SHARED_PREFS_IDUSER,-1));
        call.enqueue(new Callback<EventsAPI>() {
            @Override
            public void onResponse(@NonNull Call<EventsAPI> call, @NonNull Response<EventsAPI> response) {
                if (response.isSuccessful()) {
                    if(response.body() == null)
                        write2File(ctx, Constants.FILE_EVENT_BLOCK, null);
                    else
                        write2File(ctx, Constants.FILE_EVENT_BLOCK, response.body().events);

                    // Engeguem els workers
                    runRestartEventsWorkerOnce(ctx,0);
                    startRestartEventsWorker24h(ctx);
                }
            }

            @Override
            public void onFailure(@NonNull Call<EventsAPI> call, @NonNull Throwable t) { }
        });
    }

    public static void checkHoraris(Context ctx) {
        Log.d(TAG,"Check Horaris");
        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(ctx);

        Api mTodoService = ((AdicticApp) (ctx.getApplicationContext())).getAPI();

        assert sharedPreferences != null;

        // Agafem els horaris de la nit i Events
        Call<HorarisAPI> call = mTodoService.getHoraris(sharedPreferences.getLong(Constants.SHARED_PREFS_IDUSER,-1));
        call.enqueue(new Callback<HorarisAPI>() {
            @Override
            public void onResponse(@NonNull Call<HorarisAPI> call, @NonNull Response<HorarisAPI> response) {
                if (response.isSuccessful()) {
                    if(response.body() == null || response.body().horarisNit.isEmpty())
                        write2File(ctx, Constants.FILE_HORARIS_NIT, null);
                    else
                        write2File(ctx, Constants.FILE_HORARIS_NIT, new ArrayList<>(response.body().horarisNit));

                    // Engeguem els workers
                    runRestartHorarisWorkerOnce(ctx,0);
                    startRestartHorarisWorker24h(ctx);
                }
            }

            @Override
            public void onFailure(@NonNull Call<HorarisAPI> call, @NonNull Throwable t) { }
        });
    }

    // To check if app has PACKAGE_USAGE_STATS enabled
    public static boolean isAppUsagePermissionOn(Context mContext) {
        boolean granted;
        AppOpsManager appOps = (AppOpsManager) mContext
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), mContext.getPackageName());
        }
        else
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), mContext.getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (mContext.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }

        return granted;
    }

    // To check if accessibility service is enabled
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        AccessibilityManager am = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(mContext.getPackageName()) && enabledServiceInfo.name.equals(WindowChangeDetectingService.class.getName()))
                return true;
        }

        String prefString = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(prefString!= null && prefString.contains(mContext.getPackageName() + "/" + WindowChangeDetectingService.class.getName())) {
            Log.e(TAG, "AccessibilityServiceInfo negatiu però prefString positiu");
            return true;
        }
        return false;
    }

    public static boolean isBackgroundLocationPermissionOn(Context mContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        else
            return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // To check if Admin Permissions are on
    public static boolean isAdminPermissionsOn(Context mContext) {
        DevicePolicyManager mDPM = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        List<ComponentName> mActiveAdmins = mDPM.getActiveAdmins();

        if (mActiveAdmins == null) return false;

        boolean found = false;
        int i = 0;
        while (!found && i < mActiveAdmins.size()) {
            if (mActiveAdmins.get(i).getPackageName().equals(mContext.getPackageName()))
                found = true;
            i++;
        }
        return found;
    }

    public static void updateDB_BlockedApps(Context ctx, BlockedLimitedLists body) {
        List<BlockedApp> llista = new ArrayList<>();

        for(LimitedApps limitedApp : body.limitApps){
            BlockedApp blockedApp = new BlockedApp();
            blockedApp.pkgName = limitedApp.name;
            blockedApp.timeLimit = limitedApp.time;
            llista.add(blockedApp);
        }

        write2File(ctx, Constants.FILE_BLOCKED_APPS,llista);

        runRestartBlockedAppsWorkerOnce(ctx,0);
        startRestartBlockedAppsWorker24h(ctx);
    }

    // **************** WORKERS ****************

    // AppUsageWorkers

    public static void startAppUsageWorker24h(Context mCtx){
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(mCtx);
        Calendar cal = Calendar.getInstance();
        // Agafem dades dels últims X dies per inicialitzar dades al servidor
        cal.add(Calendar.DAY_OF_YEAR, -6);
        assert sharedPreferences != null;
        sharedPreferences.edit().putInt(Constants.SHARED_PREFS_DAYOFYEAR,cal.get(Calendar.DAY_OF_YEAR)).apply();

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(AppUsageWorker.class, 24, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork("pujarAppInfo",
                        ExistingPeriodicWorkPolicy.KEEP,
                        myWork);

        Log.d(TAG,"Worker AppUsage 24h Configurat");

        runUniqueAppUsageWorker(mCtx);
    }

    public static void runUniqueAppUsageWorker(Context mContext) {
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(AppUsageWorker.class)
                        .setInitialDelay(0, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("uniqueAppUsageWorker", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker AppUsage (únic) Començat");
    }

    // EventWorkers

    public static void startRestartEventsWorker24h(Context mCtx){
        long now = DateTime.now().getMillisOfDay();
        long delay = Constants.TOTAL_MILLIS_IN_DAY + 30000 - now; // 30 segons més de mitjanit

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(RestartEventsWorker.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(delay,TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork("24h_eventBlock",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        myWork);

        Log.d(TAG,"Worker RestartEvents 24h Configurat");
    }

    public static void runRestartEventsWorkerOnce(Context mContext, long delay){
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(RestartEventsWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("EventsWorkerOnce", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker RestartEvents (únic) Començat");
    }

    public static void runStartBlockEventWorker(Context mContext, long id, long delay) {
        Data.Builder data = new Data.Builder();
        data.putLong("id", id);

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(StartBlockEventWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data.build())
                        .addTag(Constants.WORKER_TAG_EVENT_BLOCK)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork(String.valueOf(id), ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker StartBlockEvent Configurat - ID=" + id + " | delay=" + delay);
    }

    public static void runFinishBlockEventWorker(Context mContext, long id, long delay) {
        Data.Builder data = new Data.Builder();
        data.putLong("id", id);

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(FinishBlockEventWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data.build())
                        .addTag(Constants.WORKER_TAG_EVENT_BLOCK)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork(String.valueOf(id), ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker FinishBlockEvent Configurat - ID=" + id + " | delay=" + delay);
    }

    // Horaris Worker

    public static void startRestartHorarisWorker24h(Context mCtx){
        long now = DateTime.now().getMillisOfDay();
        long delay = Constants.TOTAL_MILLIS_IN_DAY + 30000 - now; // 30 segons més de mitjanit

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(RestartHorarisWorker.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(delay,TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork("24h_horarisWorker",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        myWork);

        Log.d(TAG,"Worker RestartEvents 24h Configurat");
    }

    public static void runRestartHorarisWorkerOnce(Context mContext, long delay){
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(RestartHorarisWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("HorarisWorkerOnce", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker RestartEvents (únic) Començat");
    }

    public static void runDespertarWorker(Context mContext, long delay){
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(DespertarWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag(Constants.WORKER_TAG_HORARIS_BLOCK)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("despertarWorker", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker Despertar Configurat - delay=" + delay);
    }

    public static void runDormirWorker(Context mContext, long delay){
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(DormirWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag(Constants.WORKER_TAG_HORARIS_BLOCK)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("dormirWorker", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker Dormir Configurat - delay=" + delay);
    }

    // BlockAppsWorkers

    public static void startRestartBlockedAppsWorker24h(Context mCtx){
        long now = DateTime.now().getMillisOfDay();
        long delay = Constants.TOTAL_MILLIS_IN_DAY + 30000 - now; // 30 segons més de mitjanit

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(RestartBlockedApps.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(delay,TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork("24h_blockApps",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        myWork);

        Log.d(TAG,"Worker RestartBlockedApps 24h Configurat");
    }

    public static void runRestartBlockedAppsWorkerOnce(Context mContext, long delay){
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(RestartBlockedApps.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("BlockedAppsWorkerOnce", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker RestartBlockedApps (únic) Començat");
    }

    public static void runBlockAppsWorker(Context mContext, long delay) {
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(BlockAppWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag(Constants.WORKER_TAG_BLOCK_APPS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("blockAppWorker", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker BlockApp Configurat - Delay=" + delay);
    }

    // GeolocWorkers

    public static void runGeoLocWorker(Context mContext) {
        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(GeoLocWorker.class, 1, TimeUnit.HOURS)
                        .setInitialDelay(0, TimeUnit.MILLISECONDS)
                        .addTag(Constants.WORKER_TAG_GEOLOC_PERIODIC)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniquePeriodicWork("geoLocWorker", ExistingPeriodicWorkPolicy.KEEP, myWork);
    }

    public static void runGeoLocWorkerOnce(Context mContext) {
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(GeoLocWorker.class)
                        .setInitialDelay(0, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("geoLocWorkerOnce", ExistingWorkPolicy.REPLACE, myWork);
    }

    // UpdateTokenWorker

    public static void runUpdateTokenWorker(Context mContext, long idUser, String token, long delay){
        Data.Builder data = new Data.Builder();
        data.putLong("idUser", idUser);
        data.putString("token", token);

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(UpdateTokenWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("UpdateTokenWorker", ExistingWorkPolicy.REPLACE, myWork);

        Log.d(TAG,"Worker UpdateToken Configurat - ID=" + idUser + " | delay=" + delay);
    }

    // **************** END WORKERS ****************



    private static EncryptedFile getEncryptedFile(Context mCtx, String fileName, boolean write){
        File file = new File(mCtx.getFilesDir(),fileName);

        try {
            boolean wasSuccessful = true;
            if(write && file.exists())
                wasSuccessful = file.delete();

            if(!wasSuccessful) {
                String TAG = "Funcions (getEncryptedFile)";
                Log.i(TAG, "No s'ha pogut esborrar el fitxer: " + fileName);
                return null;
            }

            return new EncryptedFile.Builder(
                    mCtx,
                    file,
                    Objects.requireNonNull(getMasterKey(mCtx)),
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> void write2File(Context mCtx, String filename, List<T> list){
        // Mirem a quin fitxer escriure
        Objects.requireNonNull(getEncryptedSharedPreferences(mCtx)).edit().putBoolean(filename, true).apply();

        EncryptedFile encryptedFile = getEncryptedFile(mCtx, filename, true);
        if(encryptedFile == null) return;

        if(list != null && !list.isEmpty()){
            Set<T> setList = new HashSet<>(list);

            // Agafem el JSON de la llista i inicialitzem EncryptedFile
            String json = new Gson().toJson(setList);

            // Escrivim al fitxer
            try {
                FileOutputStream fileOutputStream = encryptedFile.openFileOutput();
                fileOutputStream.write(json.getBytes());
                fileOutputStream.flush();
                fileOutputStream.close();


            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();

            }
        }
        else{
            File file = new File(mCtx.getFilesDir(), filename);
            if(file.exists()){
                if(file.delete())
                    Log.d(TAG, "write2File: fitxer \"" + filename + "\" s'ha esborrat correctament");
                else
                    Log.d(TAG, "write2File: fitxer \"" + filename + "\" no s'ha pogut esborrar");
            }
        }

    }

    public static <T> List<T> readFromFile(Context mCtx, String filename, boolean storeChanges){
        if(fileEmpty(mCtx,filename)) {
            if(storeChanges)
                Objects.requireNonNull(getEncryptedSharedPreferences(mCtx)).edit().putBoolean(filename, false).apply();
            return new ArrayList<>();
        }

        EncryptedFile encryptedFile = getEncryptedFile(mCtx, filename, false);
        assert encryptedFile != null;

        StringBuilder stringBuilder = new StringBuilder();
        try {
            FileInputStream fileInputStream = encryptedFile.openFileInput();

            InputStreamReader inputStreamReader =
                    new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);

            BufferedReader reader = new BufferedReader(inputStreamReader);

            String line = reader.readLine();
            while(line != null){
                stringBuilder.append(line).append('\n');
                line = reader.readLine();
            }

            Gson gson = new Gson();
            Type listType = getListType(filename);
            ArrayList<T> res = gson.fromJson(stringBuilder.toString(),listType);
            reader.close();
            inputStreamReader.close();
            fileInputStream.close();

            if(storeChanges)
                Objects.requireNonNull(getEncryptedSharedPreferences(mCtx)).edit().putBoolean(filename, false).apply();

            return res;

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isXiaomi(){
        return Build.MANUFACTURER.toLowerCase().equals("xiaomi");
    }

    public static boolean fileEmpty(Context mCtx, String fileName){
        File file = new File(mCtx.getFilesDir(),fileName);

        // Si el fitxer no existeix el tractem com si fos buit.
        if(!file.exists())
            return true;

        // Retornem si el fitxer està buit
        return file.length() == 0;
    }

    private static Type getListType(String filename) {
        switch (filename) {
            case Constants.FILE_BLOCKED_APPS:
                return new TypeToken<ArrayList<BlockedApp>>() {
                }.getType();
            case Constants.FILE_EVENT_BLOCK:
                return new TypeToken<ArrayList<EventBlock>>() {
                }.getType();
            case Constants.FILE_HORARIS_NIT:
                return new TypeToken<ArrayList<HorarisNit>>() {
                }.getType();
            case Constants.FILE_CURRENT_BLOCKED_APPS:
                return new TypeToken<ArrayList<String>>() {
                }.getType();
        }
        return null;
    }

    public static void endFreeUse(Context mCtx) {
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(mCtx);
        assert sharedPreferences != null;
        boolean isBlocked = sharedPreferences.getBoolean(Constants.SHARED_PREFS_BLOCKEDDEVICE, false) ||
                sharedPreferences.getInt(Constants.SHARED_PREFS_ACTIVE_EVENTS, 0) > 0 ||
                sharedPreferences.getBoolean(Constants.SHARED_PREFS_ACTIVE_HORARIS_NIT, false) ||
                !sharedPreferences.getBoolean(Constants.SHARED_PREFS_FREEUSE, false);
        if(isBlocked && !sharedPreferences.getBoolean(Constants.SHARED_PREFS_FREEUSE, false)) {
            DevicePolicyManager mDPM = (DevicePolicyManager) mCtx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            assert mDPM != null;
            mDPM.lockNow();
        }
    }
}
