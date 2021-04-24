package com.example.adictic.util;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.adictic.R;
import com.example.adictic.entity.AppInfo;
import com.example.adictic.entity.AppUsage;
import com.example.adictic.entity.BlockedApp;
import com.example.adictic.entity.BlockedLimitedLists;
import com.example.adictic.entity.EventBlock;
import com.example.adictic.entity.FreeUseApp;
import com.example.adictic.entity.GeneralUsage;
import com.example.adictic.entity.Horaris;
import com.example.adictic.entity.HorarisEvents;
import com.example.adictic.entity.HorarisNit;
import com.example.adictic.entity.LimitedApps;
import com.example.adictic.entity.MonthEntity;
import com.example.adictic.entity.YearEntity;
import com.example.adictic.rest.TodoApi;
import com.example.adictic.service.AppUsageWorker;
import com.example.adictic.service.FinishBlockEventWorker;
import com.example.adictic.service.GeoLocWorker;
import com.example.adictic.service.LimitAppsWorker;
import com.example.adictic.service.StartBlockEventWorker;
import com.example.adictic.service.WindowChangeDetectingService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.adictic.util.Constants.KEY_SIZE;
import static com.example.adictic.util.Constants.SHARED_PREFS_CHANGE_BLOCKED_APPS;
import static com.example.adictic.util.Constants.SHARED_PREFS_CHANGE_EVENT_BLOCK;
import static com.example.adictic.util.Constants.SHARED_PREFS_CHANGE_FREE_USE_APPS;
import static com.example.adictic.util.Constants.SHARED_PREFS_CHANGE_HORARIS_NIT;

public class Funcions {

    private static long getHorariInMillis() {
        Calendar cal = Calendar.getInstance();
        String wakeTime = TodoApp.getWakeHoraris().get(cal.get(Calendar.DAY_OF_WEEK));
        String sleepTime = TodoApp.getSleepHoraris().get(cal.get(Calendar.DAY_OF_WEEK));

        int wakeHour = Integer.parseInt(wakeTime.split(":")[0]);
        int wakeMinute = Integer.parseInt(wakeTime.split(":")[1]);

        int sleepHour = Integer.parseInt(sleepTime.split(":")[0]);
        int sleepMinute = Integer.parseInt(sleepTime.split(":")[1]);

        Calendar calWake = Calendar.getInstance();
        calWake.set(Calendar.HOUR_OF_DAY, wakeHour);
        calWake.set(Calendar.MINUTE, wakeMinute);

        Calendar calSleep = Calendar.getInstance();
        calSleep.set(Calendar.HOUR_OF_DAY, sleepHour);
        calSleep.set(Calendar.MINUTE, sleepMinute);

        long timeNow = cal.getTimeInMillis();
        long wakeMillis = calWake.getTimeInMillis();
        long sleepMillis = calSleep.getTimeInMillis();

        if (wakeMillis > sleepMillis) {
            if (timeNow >= wakeMillis) {
                TodoApp.setBlockedDevice(false);
                calSleep.add(Calendar.DAY_OF_YEAR, 1);

                return calSleep.getTimeInMillis();
            } else if (timeNow >= sleepMillis) {
                TodoApp.setBlockedDevice(true);
                return wakeMillis;
            } else {
                return sleepMillis;
            }
        } else {
            if (timeNow >= sleepMillis) {
                TodoApp.setBlockedDevice(true);
                calWake.add(Calendar.DAY_OF_YEAR, 1);

                return calWake.getTimeInMillis();
            } else if (timeNow >= wakeMillis) {
                TodoApp.setBlockedDevice(false);
                return sleepMillis;
            } else return wakeMillis;
        }
    }

    public static String date2String(int dia, int mes, int any) {
        String data;
        if (dia < 10) data = "0" + dia + "-";
        else data = dia + "-";
        if (mes < 10) data += "0" + mes + "-";
        else data += mes + "-";

        return data + any;
    }

    public static void setIconDrawable(Context ctx, String pkgName, final ImageView d) {
        Uri imageUri = Uri.parse(Global.BASE_URL_PORTFORWARDING).buildUpon()
                .appendPath("icons")
                .appendPath(pkgName)
                .build();

        Glide.with(ctx)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(d);
    }

    public static void checkHoraris(Context ctx) {

        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(ctx);

        TodoApi mTodoService = ((TodoApp) (ctx.getApplicationContext())).getAPI();

        Call<Horaris> call = mTodoService.getHoraris(sharedPreferences.getLong("idUser",-1));

        call.enqueue(new Callback<Horaris>() {
            @Override
            public void onResponse(@NonNull Call<Horaris> call, @NonNull Response<Horaris> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if(response.body().horarisNits != null)
                        write2File(ctx,response.body().horarisNits);
                    if(response.body().events != null)
                        write2File(ctx,response.body().events);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Horaris> call, @NonNull Throwable t) {

            }
        });
    }

    // To check if app has PACKAGE_USAGE_STATS enabled
    public static boolean isAppUsagePermissionOn(Context mContext) {
        boolean granted;
        AppOpsManager appOps = (AppOpsManager) mContext
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
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

        return false;
    }

    public static Pair<Integer, Integer> millisToString(float l) {
        float minuts = l / (60000);
        int hores = 0;

        while (minuts >= 60) {
            hores++;
            minuts -= 60;
        }

        return new Pair<>(hores, Math.round(minuts));
    }

    public static int string2MillisOfDay(String time){
        String[] time2 = time.split(":");
        DateTime dateTime = new DateTime()
                .withHourOfDay(Integer.parseInt(time2[0]))
                .withMinuteOfHour(Integer.parseInt(time2[1]));
        return dateTime.getMillisOfDay();
    }

    public static String millisOfDay2String(int millis){
        DateTime dateTime = new DateTime()
                .withMillisOfDay(millis);

        return dateTime.getHourOfDay() + ":" + dateTime.getMinuteOfHour();
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

    public static Map<Integer, Map<Integer, List<Integer>>> convertYearEntityToMap(List<YearEntity> yearList) {
        Map<Integer, Map<Integer, List<Integer>>> res = new HashMap<>();
        for (YearEntity yEntity : yearList) {
            Map<Integer, List<Integer>> mMap = new HashMap<>();
            for (MonthEntity mEntity : yEntity.months) {
                mMap.put(mEntity.month, mEntity.days);
            }
            res.put(yEntity.year, mMap);
        }

        return res;
    }

    public static void runLimitAppsWorker(Context mContext, long delay) {
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(LimitAppsWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("checkLimitedApps", ExistingWorkPolicy.REPLACE, myWork);
    }

    public static void runGeoLocWorker(Context mContext) {
        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(GeoLocWorker.class)
                        .setInitialDelay(0, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("geoLocWorker", ExistingWorkPolicy.REPLACE, myWork);
    }

    public static void updateDB_BlockedApps(Context ctx, BlockedLimitedLists body) {
        List<BlockedApp> llista = new ArrayList<>();

        for(String pkgName : body.blockedApps){
            BlockedApp blockedApp = new BlockedApp();
            blockedApp.pkgName = pkgName;
            blockedApp.blockedNow = true;
            blockedApp.timeLimit = -1;
            llista.add(blockedApp);
        }
        for(LimitedApps limitedApp : body.limitApps){
            BlockedApp blockedApp = new BlockedApp();
            blockedApp.pkgName = limitedApp.name;
            blockedApp.blockedNow = false;
            blockedApp.timeLimit = limitedApp.time;
            llista.add(blockedApp);
        }

        write2File(ctx,llista);
    }

    private static List<EventBlock> horarisEvents2EventBlock(List<HorarisEvents> horarisEvents){
        List<EventBlock> res = new ArrayList<>();
        
        for(HorarisEvents event : horarisEvents){
            EventBlock eventBlock = new EventBlock();

            eventBlock.id = event.id;
            eventBlock.name = event.name;

            Pair<Integer,Integer> startTime = stringToTime(event.start);
            DateTime dateTimeStart = new DateTime()
                    .withHourOfDay(startTime.first)
                    .withMinuteOfHour(startTime.second);
            eventBlock.startEvent = dateTimeStart.getMillisOfDay();
            Pair<Integer,Integer> endTime = stringToTime(event.start);
            DateTime dateTimeFinish = new DateTime()
                    .withHourOfDay(endTime.first)
                    .withMinuteOfHour(endTime.second);
            eventBlock.endEvent = dateTimeFinish.getMillisOfDay();

            int millisNow = DateTime.now().getMillisOfDay();
            eventBlock.activeNow = millisNow >= eventBlock.startEvent && millisNow < eventBlock.endEvent;

            eventBlock.monday = event.days.contains(Calendar.MONDAY);
            eventBlock.tuesday = event.days.contains(Calendar.TUESDAY);
            eventBlock.wednesday = event.days.contains(Calendar.WEDNESDAY);
            eventBlock.thursday = event.days.contains(Calendar.THURSDAY);
            eventBlock.friday = event.days.contains(Calendar.FRIDAY);
            eventBlock.saturday = event.days.contains(Calendar.SATURDAY);
            eventBlock.sunday = event.days.contains(Calendar.SUNDAY);

            res.add(eventBlock);
        }

        return res;
    }
    
    public static void updateEventList(Context mContext, List<EventBlock> newEvents) {
        WorkManager workManager = WorkManager.getInstance(mContext);

        // Llegim la llista d'Events actuals
        List<EventBlock> currentEvents = readFromFile(mContext,Constants.FILE_EVENT_BLOCK,false);

        // Agafem els events diferents entre les dues llistes
        List<EventBlock> disjunctionEvents = new ArrayList<>(CollectionUtils.disjunction(newEvents, currentEvents));

        // recorrem els events diferents
        for (EventBlock event : disjunctionEvents) {
            int index = newEvents.indexOf(event);

            // Event s'ha esborrat (no existeix a la nova llista amb el mateix id)
            if (index == -1) {
                workManager.cancelUniqueWork(event.name);
            }
            // És un nou event o editat
            else if (event.exactSame(newEvents.get(index))) {
                long now = DateTime.now().getMillisOfDay();

                if (now < event.startEvent) {
                    runStartBlockEventWorker(mContext, event.id, event.startEvent - now);
                } else if (now < event.endEvent) {
                    runFinishBlockEventWorker(mContext, event.id, event.endEvent - now);
                }
            }
        }

        // Afegim la nova llista al fitxer
        write2File(mContext,newEvents);
    }

    public static void runStartBlockEventWorker(Context mContext, long id, long delay) {
        Data.Builder data = new Data.Builder();
        data.putLong("id", id);

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(StartBlockEventWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork(String.valueOf(id), ExistingWorkPolicy.REPLACE, myWork);
    }

    public static void runFinishBlockEventWorker(Context mContext, long id, long delay) {
        Data.Builder data = new Data.Builder();
        data.putLong("id", id);

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(FinishBlockEventWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork(String.valueOf(id), ExistingWorkPolicy.REPLACE, myWork);
    }

    /**
     * pre: si fTime = -1, agafa valors del dia actual inacabat
     **/
    public static List<GeneralUsage> getGeneralUsages(Context mContext, int iTime, int fTime) {
        List<GeneralUsage> gul = new ArrayList<>();

        if (fTime == -1) {
            Calendar finalTime = Calendar.getInstance();

            Calendar initialTime = Calendar.getInstance();
            initialTime.set(Calendar.HOUR_OF_DAY, 0);
            initialTime.set(Calendar.MINUTE, 0);
            initialTime.set(Calendar.SECOND, 0);

            List<AppUsage> appUsages = getAppUsages(mContext, initialTime, finalTime);

            GeneralUsage gu = new GeneralUsage();
            gu.day = finalTime.get(Calendar.DAY_OF_MONTH);
            gu.month = finalTime.get(Calendar.MONTH) + 1;
            gu.year = finalTime.get(Calendar.YEAR);
            gu.usage = appUsages;

            gu.totalTime = Long.parseLong("0");
            for (AppUsage au : appUsages) {
                gu.totalTime += au.totalTime;
            }

            gul.add(gu);
        } else {
            for (int i = iTime; i <= fTime; i++) {
                Calendar finalTime = Calendar.getInstance();
                finalTime.set(Calendar.DAY_OF_YEAR, i);
                finalTime.set(Calendar.HOUR_OF_DAY, 23);
                finalTime.set(Calendar.MINUTE, 59);
                finalTime.set(Calendar.SECOND, 59);

                Calendar initialTime = Calendar.getInstance();
                initialTime.set(Calendar.DAY_OF_YEAR, i);
                initialTime.set(Calendar.HOUR_OF_DAY, 0);
                initialTime.set(Calendar.MINUTE, 0);
                initialTime.set(Calendar.SECOND, 0);

                List<AppUsage> appUsages = getAppUsages(mContext, initialTime, finalTime);

                GeneralUsage gu = new GeneralUsage();
                gu.day = finalTime.get(Calendar.DAY_OF_MONTH);
                gu.month = finalTime.get(Calendar.MONTH) + 1;
                gu.year = finalTime.get(Calendar.YEAR);
                gu.usage = appUsages;

                gul.add(gu);
            }
        }
        return gul;
    }

    private static List<AppUsage> getAppUsages(Context mContext, Calendar initialTime, Calendar finalTime) {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager mPm = mContext.getPackageManager();

        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST,
                initialTime.getTimeInMillis(), finalTime.getTimeInMillis());

        List<AppUsage> appUsages = new ArrayList<>();
        final int statCount = stats.size();
        for (int j = 0; j < statCount; j++) {
            final android.app.usage.UsageStats pkgStats = stats.get(j);
            ApplicationInfo appInfo = null;
            try {
                appInfo = mPm.getApplicationInfo(pkgStats.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                //e.printStackTrace();
            }
            if (appInfo != null && pkgStats.getLastTimeUsed() >= initialTime.getTimeInMillis() && pkgStats.getLastTimeUsed() <= finalTime.getTimeInMillis() && pkgStats.getTotalTimeInForeground() > 5000 && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                AppUsage appUsage = new AppUsage();
                appUsage.app = new AppInfo();

                if (Build.VERSION.SDK_INT >= 26) appUsage.app.category = appInfo.category;
                appUsage.app.appName = mPm.getApplicationLabel(appInfo).toString();
                appUsage.app.pkgName = pkgStats.getPackageName();
                appUsage.lastTimeUsed = pkgStats.getLastTimeUsed();
                appUsage.totalTime = pkgStats.getTotalTimeInForeground();
                appUsages.add(appUsage);
            }
        }

        return appUsages;
    }

    public static void updateLimitedAppsList(Context mContext) {
        // Desactivem el "freeUse"
        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(mContext);
        sharedPreferences.edit().putBoolean("freeUse",false).apply();

        // Agafem les dades d'ús d'avui
        List<GeneralUsage> generalUsages = getGeneralUsages(mContext,-1, -1);
        List<AppUsage> appUsage = new ArrayList<>(generalUsages.get(0).usage);

        // Agafem totes les FreeUseApps
        List<FreeUseApp> freeUseApps = readFromFile(mContext,Constants.FILE_FREE_USE_APPS,false);

        // Per cada blockedApp -> creem un FreeUseApp amb el temps total d'ús de l'app en aquest moment
        for(FreeUseApp app : freeUseApps){
            AppUsage au = appUsage.get(appUsage.indexOf(app.pkgName));
            app.millisUsageEnd = au.totalTime;
        }

        write2File(mContext,freeUseApps);
    }

    public static void startFreeUseLimitList(Context mContext) {
        // Activem el "freeUse"
        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(mContext);
        sharedPreferences.edit().putBoolean("freeUse",true).apply();

        // Agafem les dades d'ús d'avui
        List<GeneralUsage> generalUsages = getGeneralUsages(mContext,-1, -1);
        List<AppUsage> appUsage = new ArrayList<>(generalUsages.get(0).usage);

        // Inicialitzem la taula de FreeUseApps i agafem totes les BlockedApps que no han sobrepassat el límit
        List<BlockedApp> blockedApps = readFromFile(mContext,Constants.FILE_BLOCKED_APPS,false);

        List<FreeUseApp> freeUseApps = new ArrayList<>();

        // Per cada blockedApp -> creem un FreeUseApp amb el temps total d'ús de l'app en aquest moment
        for(BlockedApp app : blockedApps){
            if(!app.blockedNow) {
                FreeUseApp freeUseApp = new FreeUseApp();
                AppUsage au = appUsage.get(appUsage.indexOf(app.pkgName));
                freeUseApp.pkgName = app.pkgName;
                freeUseApp.millisUsageStart = au.totalTime;
                freeUseApp.millisUsageEnd = -1;

                freeUseApps.add(freeUseApp);
            }
        }

        write2File(mContext,freeUseApps);
    }

    /**
     * Retorna -1 als dos valors si no és un string acceptable
     **/
    public static Pair<Integer, Integer> stringToTime(String s) {
        int hour, minutes;
        String[] hora = s.split(":");

        if (hora.length != 2) {
            hour = -1;
            minutes = -1;
        } else {
            if (Integer.parseInt(hora[0]) < 0 || Integer.parseInt(hora[0]) > 23) {
                hour = -1;
                minutes = -1;
            } else if (Integer.parseInt(hora[1]) < 0 || Integer.parseInt(hora[1]) > 59) {
                hour = -1;
                minutes = -1;
            } else {
                hour = Integer.parseInt(hora[0]);
                minutes = Integer.parseInt(hora[1]);
            }
        }

        return new Pair<>(hour, minutes);
    }

    public static void canviarMesosDeServidor(Collection<GeneralUsage> generalUsages) {
        for (GeneralUsage generalUsage : generalUsages) {
            generalUsage.month -= 1;
        }
    }

    public static void canviarMesosAServidor(Collection<GeneralUsage> generalUsages) {
        for (GeneralUsage generalUsage : generalUsages) {
            generalUsage.month += 1;
        }
    }

    public static void canviarMesosDeServidor(List<YearEntity> yearList) {
        for (YearEntity yearEntity : yearList) {
            for (MonthEntity monthEntity : yearEntity.months) {
                monthEntity.month -= 1;
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void closeKeyboard(View view, Activity a) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideSoftKeyboard(a);
                return false;
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                closeKeyboard(innerView, a);
            }
        }
    }

    private static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager.isAcceptingText() && activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(),
                    0
            );
        }
    }

    public static void askChildForLiveApp(Context ctx, long idChild, boolean liveApp) {
        TodoApi mTodoService = ((TodoApp) (ctx.getApplicationContext())).getAPI();
        Call<String> call = mTodoService.askChildForLiveApp(idChild, liveApp);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (!response.isSuccessful()) {
                    Toast toast = Toast.makeText(ctx, ctx.getString(R.string.error_liveApp), Toast.LENGTH_LONG);
                    toast.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast toast = Toast.makeText(ctx, ctx.getString(R.string.error_liveApp), Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public static String getFullURL(String url) {
        if (!url.contains("https://")) return "https://" + url;
        else return url;
    }

    /**
     * SHARED PREFERENCES
     */

    private static MasterKey getMasterKey(Context mCtx) {
        try {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    Constants.MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE)
                    .build();

            return new MasterKey.Builder(mCtx)
                    .setKeyGenParameterSpec(spec)
                    .build();
        } catch (Exception e) {
            Log.e(mCtx.getClass().getSimpleName(), "Error on getting master key", e);
        }
        return null;
    }

    public static SharedPreferences getEncryptedSharedPreferences(Context mCtx) {
        try {
            return EncryptedSharedPreferences.create(
                    mCtx,
                    "values",
                    getMasterKey(mCtx), // calling the method above for creating MasterKey
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(mCtx.getClass().getSimpleName(), "Error on getting encrypted shared preferences", e);
        }
        return null;
    }

    private static EncryptedFile getEncryptedFile(Context mCtx, String fileName, boolean write){
        File file = new File(mCtx.getFilesDir(),fileName);

        try {
            if(write && file.exists())
                file.delete();
            else if(!write && !file.exists())
                file.createNewFile();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return new EncryptedFile.Builder(
                    mCtx,
                    file,
                    getMasterKey(mCtx),
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> boolean write2File(Context mCtx, List<T> list){
        if(!list.isEmpty()){
            Set<T> setList = new HashSet<>(list);

            // Agafem el JSON de la llista i inicialitzem EncryptedFile
            String json = new Gson().toJson(setList);
            EncryptedFile encryptedFile;

            // Mirem a quin fitxer escriure
            encryptedFile = inicialitzarFitxer(mCtx,list.get(0));

            if(encryptedFile == null) return false;

            // Escrivim al fitxer
            try {
                FileOutputStream fileOutputStream = encryptedFile.openFileOutput();
                fileOutputStream.write(json.getBytes());
                fileOutputStream.close();

                updateSharedPrefsChange(mCtx,list.get(0), true);

                return true;
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();

                return false;
            }
        }
        else return false;
    }

    public static <T> List<T> readFromFile(Context mCtx, String filename, boolean storeChanges){
        EncryptedFile encryptedFile = getEncryptedFile(mCtx, filename, false);

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
            inputStreamReader.close();

            if(storeChanges) updateSharedPrefsChange(mCtx,res.get(0),false);

            return res;

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean fileEmpty(Context mCtx, String fileName){
        File file = new File(mCtx.getFilesDir(),fileName);

        // Si el fitxer no existeix el tractem com si fos buit.
        if(!file.exists())
            return true;

        // Retornem si el fitxer està buit
        return file.length() == 0;
    }

    public static void startAppUsageWorker(Context mCtx){
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(mCtx);
        Calendar cal = Calendar.getInstance();
        // Agafem dades dels últims X dies per inicialitzar dades al servidor
        cal.add(Calendar.DAY_OF_YEAR, -6);
        sharedPreferences.edit().putInt("dayOfYear",cal.get(Calendar.DAY_OF_YEAR)).apply();

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(AppUsageWorker.class, 24, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork("pujarAppInfo",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        myWork);
    }

    private static Type getListType(String filename) {
        switch (filename) {
            case Constants.FILE_BLOCKED_APPS:
                return new TypeToken<ArrayList<BlockedApp>>() {
                }.getType();
            case Constants.FILE_EVENT_BLOCK:
                return new TypeToken<ArrayList<EventBlock>>() {
                }.getType();
            case Constants.FILE_FREE_USE_APPS:
                return new TypeToken<ArrayList<FreeUseApp>>() {
                }.getType();
            case Constants.FILE_HORARIS_NIT:
                return new TypeToken<ArrayList<HorarisNit>>() {
                }.getType();
        }
        return null;
    }

    /**
     * Actualitzar els valors a SharedPrefs per si hi ha hagut canvis en els fitxers
     * @param mCtx El Context de l'activitat
     * @param object Per saber quin valor tocar
     * @param bool El valor a donar
     */
    private static void updateSharedPrefsChange(Context mCtx, Object object, boolean bool) {
        // Mirem a quin sharedPrefs escriure
        if(object instanceof BlockedApp)
            getEncryptedSharedPreferences(mCtx).edit().putBoolean(SHARED_PREFS_CHANGE_BLOCKED_APPS,bool).apply();
        else if (object instanceof EventBlock)
            getEncryptedSharedPreferences(mCtx).edit().putBoolean(SHARED_PREFS_CHANGE_EVENT_BLOCK,bool).apply();
        else if (object instanceof FreeUseApp)
            getEncryptedSharedPreferences(mCtx).edit().putBoolean(SHARED_PREFS_CHANGE_FREE_USE_APPS,bool).apply();
        else if (object instanceof HorarisNit)
            getEncryptedSharedPreferences(mCtx).edit().putBoolean(SHARED_PREFS_CHANGE_HORARIS_NIT,bool).apply();
    }

    /**
     * Retorna el fitxer adient
     * @param mCtx Context de l'activitat
     * @param object Per saber quin fitxer agafar
     * @return El fitxer o "null" si han passat un objecte dolent
     */
    private static EncryptedFile inicialitzarFitxer(Context mCtx, Object object){
        // Mirem a quin fitxer escriure
        if(object instanceof BlockedApp)
            return getEncryptedFile(mCtx,Constants.FILE_BLOCKED_APPS, true);
        else if (object instanceof EventBlock)
            return getEncryptedFile(mCtx,Constants.FILE_EVENT_BLOCK, true);
        else if (object instanceof FreeUseApp)
            return getEncryptedFile(mCtx,Constants.FILE_FREE_USE_APPS, true);
        else if (object instanceof HorarisNit)
            return getEncryptedFile(mCtx,Constants.FILE_HORARIS_NIT, true);
        else return null;
    }

}
