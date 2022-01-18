package com.adictic.client.util;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.adictic.client.R;
import com.adictic.client.rest.AdicticApi;
import com.adictic.client.service.AccessibilityScreenService;
import com.adictic.client.ui.BlockAppActivity;
import com.adictic.client.workers.AppUsageWorker;
import com.adictic.client.workers.EventWorker;
import com.adictic.client.workers.GeoLocWorker;
import com.adictic.client.workers.HorarisEventsWorkerManager;
import com.adictic.client.workers.HorarisWorker;
import com.adictic.client.workers.NotifWorker;
import com.adictic.client.workers.ServiceWorker;
import com.adictic.common.callbacks.BooleanCallback;
import com.adictic.common.database.AppDatabase;
import com.adictic.common.database.EventDatabase;
import com.adictic.common.database.HorarisDatabase;
import com.adictic.common.entity.AppUsage;
import com.adictic.common.entity.BlockedApp;
import com.adictic.common.entity.BlockedLimitedLists;
import com.adictic.common.entity.EventBlock;
import com.adictic.common.entity.EventsAPI;
import com.adictic.common.entity.GeneralUsage;
import com.adictic.common.entity.HorarisAPI;
import com.adictic.common.entity.HorarisNit;
import com.adictic.common.entity.LimitedApps;
import com.adictic.common.entity.NotificationInformation;
import com.adictic.common.entity.UserLogin;
import com.adictic.common.rest.Api;
import com.adictic.common.util.Callback;
import com.adictic.common.util.Constants;

import org.joda.time.DateTime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Response;

public class Funcions extends com.adictic.common.util.Funcions {
    private final static String TAG = "Funcions";

    // Contrasenya ha d'estar encriptada amb SHA256
    public static void isPasswordCorrect(Context ctx, String pwd, BooleanCallback callback){
        AdicticApi api = ((AdicticApp) (ctx.getApplicationContext())).getAPI();

        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(ctx);
        assert sharedPreferences != null;

        UserLogin userLogin = new UserLogin();
        userLogin.password = pwd;
        userLogin.username = sharedPreferences.getString(Constants.SHARED_PREFS_USERNAME, "");
        userLogin.token = "";
        userLogin.tutor = -1;

        Call<String> call = api.checkPassword(userLogin);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                super.onResponse(call, response);
                callback.onDataGot(response.isSuccessful() && response.body() != null && response.body().equals("ok"));
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                callback.onDataGot(false);
            }
        });
    }

    public static void addOverlayView(Context ctx, boolean blockApp) {

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
                PixelFormat.TRANSLUCENT);

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
            if (!blockApp){
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
            else{
                SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(ctx);
                assert sharedPreferences != null;

                boolean blockedDevice = sharedPreferences.getBoolean(Constants.SHARED_PREFS_BLOCKEDDEVICE, false);

                View floatyView = inflater.inflate(R.layout.block_device_layout, interceptorLayout);
                windowManager.addView(floatyView, params);

                TextView blockDeviceTitle = floatyView.findViewById(R.id.TV_block_device_title);
                blockDeviceTitle.setText(ctx.getString(R.string.locked_device));

                if (!blockedDevice){
                    EventDatabase eventDatabase = Room.databaseBuilder(ctx,
                            EventDatabase.class, Constants.ROOM_EVENT_DATABASE)
                            .enableMultiInstanceInvalidation()
                            .build();

                    List<EventBlock> eventsList = new ArrayList<>(eventDatabase.eventBlockDao().getEventsByDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)));
                    eventDatabase.close();

                    EventBlock eventBlock = eventsList.stream()
                            .filter(com.adictic.common.util.Funcions::eventBlockIsActive)
                            .findFirst()
                            .orElse(null);

                    if(eventBlock != null){
                        String title = eventBlock.name + "\n";
                        title += millis2horaString(ctx, eventBlock.startEvent) + " - " + millis2horaString(ctx, eventBlock.endEvent);
                        blockDeviceTitle.setText(title);
                    }
                }

                ConstraintLayout CL_device_blocked_call = floatyView.findViewById(R.id.CL_block_device_emergency_call);
                CL_device_blocked_call.setOnClickListener(view -> {
                    Uri number = Uri.parse("tel:" + 112);
                    Intent dial = new Intent(Intent.ACTION_CALL, number);
                    ctx.startActivity(dial);
                });
            }
        }
        else {
            Log.e("SAW-example", "Layout Inflater Service is null; can't inflate and display R.layout.floating_view");
        }
    }

    public static void checkEvents(Context ctx) {
        Log.d(TAG,"Check Events");
        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(ctx);
        assert sharedPreferences != null;

        Api mTodoService = ((AdicticApp) (ctx.getApplicationContext())).getAPI();

        // Agafem els horaris de la nit i Events
        Call<EventsAPI> call = mTodoService.getEvents(sharedPreferences.getLong(Constants.SHARED_PREFS_IDUSER,-1));
        call.enqueue(new Callback<EventsAPI>() {
            @Override
            public void onResponse(@NonNull Call<EventsAPI> call, @NonNull Response<EventsAPI> response) {
                EventDatabase eventDatabase = Room.databaseBuilder(ctx,
                        EventDatabase.class, Constants.ROOM_EVENT_DATABASE)
                        .enableMultiInstanceInvalidation()
                        .addMigrations()
                        .build();

                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        List<EventBlock> eventBlocks = new ArrayList<>(response.body().events);

                        eventDatabase.eventBlockDao().update(eventBlocks);

                        eventDatabase.close();

                        startHorarisEventsManagerWorker(ctx);

                        setEvents(ctx, eventBlocks);
                    }).start();
                }
                else {
                    new Thread(() -> {
                        List<EventBlock> list = new ArrayList<>(eventDatabase.eventBlockDao().getAll());
                        eventDatabase.close();

                        setEvents(ctx, list);
                    }).start();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EventsAPI> call, @NonNull Throwable t) {
                new Thread(() -> {
                    EventDatabase eventDatabase = Room.databaseBuilder(ctx,
                            EventDatabase.class, Constants.ROOM_EVENT_DATABASE)
                            .enableMultiInstanceInvalidation()
                            .build();

                    List<EventBlock> list = new ArrayList<>(eventDatabase.eventBlockDao().getAll());
                    eventDatabase.close();

                    setEvents(ctx, list);
                }).start();
            }
        });
    }

    public static void setEvents(Context ctx, List<EventBlock> events) {
        // Aturem tots els workers d'Events que estiguin configurats
        WorkManager.getInstance(ctx)
                .cancelAllWorkByTag(Constants.WORKER_TAG_EVENT_BLOCK);

        if(!Funcions.accessibilityServiceOn(ctx))
            return;

        AccessibilityScreenService.instance.setActiveEvents(0);

        if(events == null || events.isEmpty())
            return;

        int diaSetmana = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        List<EventBlock> eventsTodayList = events.stream()
                .filter(eventBlock -> eventBlock.days.contains(diaSetmana))
                .collect(Collectors.toList());

        setEventWorkerByDay(ctx, eventsTodayList);
    }

    private static void setEventWorkerByDay(Context ctx, List<EventBlock> eventList) {
        eventList.sort(Comparator.comparingInt(eventBlock -> eventBlock.startEvent));

        // Mirem quins workers són necessaris en cas que hi hagi events que es solapin
        List<Pair<Integer, Integer>> workersList = new ArrayList<>();
        for(EventBlock event : eventList){
            Pair<Integer, Integer> newTime = new Pair<>(event.startEvent, event.endEvent);

            if(workersList.isEmpty())
                workersList.add(newTime);
            else {
                int index = workersList.size()-1;
                Pair<Integer, Integer> lastTime = workersList.get(index);
                if(newTime.first < lastTime.second && newTime.second > lastTime.second)
                    workersList.set(index, new Pair<>(lastTime.first, newTime.second));
                else if(newTime.first > lastTime.second)
                    workersList.add(newTime);
            }
        }

        // Fer workers
        for(Pair<Integer, Integer> pair : workersList){
            long now = DateTime.now().getMillisOfDay();

            long startTimeDelay = pair.first - now;
            long endTimeDelay = pair.second - now;

            if(startTimeDelay > 0) {
                setUpEventWorker(ctx, startTimeDelay, true);
                setUpEventWorker(ctx, endTimeDelay, false);
            }
            else if(endTimeDelay > 0){
                setUpEventWorker(ctx, endTimeDelay, false);

                if(Funcions.accessibilityServiceOn(ctx))
                    AccessibilityScreenService.instance.setActiveEvents(1);
            }
        }

        if(Funcions.accessibilityServiceOn(ctx))
            AccessibilityScreenService.instance.updateDeviceBlock();
    }

    public static void checkHoraris(Context ctx) {
        Log.d(TAG,"Check Horaris");
        SharedPreferences sharedPreferences = Funcions.getEncryptedSharedPreferences(ctx);
        assert sharedPreferences != null;

        Api mTodoService = ((AdicticApp) (ctx.getApplicationContext())).getAPI();

        // Agafem els horaris de la nit i Events
        Call<HorarisAPI> call = mTodoService.getHoraris(sharedPreferences.getLong(Constants.SHARED_PREFS_IDUSER,-1));
        call.enqueue(new Callback<HorarisAPI>() {
            @Override
            public void onResponse(@NonNull Call<HorarisAPI> call, @NonNull Response<HorarisAPI> response) {
                HorarisDatabase horarisDatabase = Room.databaseBuilder(ctx,
                        HorarisDatabase.class, Constants.ROOM_HORARIS_DATABASE)
                        .enableMultiInstanceInvalidation()
                        .build();

                if (response.isSuccessful() && response.body() != null) {
                    // Actualitzem BDD
                    new Thread(() -> {
                        List<HorarisNit> horarisList = new ArrayList<>(response.body().horarisNit);

                        horarisDatabase.horarisNitDao().update(horarisList);
                        horarisDatabase.close();

                        startHorarisEventsManagerWorker(ctx);

                        setHoraris(ctx, horarisList);
                    }).start();
                }
                else {
                    new Thread(() -> {
                        List<HorarisNit> horarisNit = new ArrayList<>(horarisDatabase.horarisNitDao().getAll());
                        horarisDatabase.close();

                        setHoraris(ctx, horarisNit);
                    }).start();
                }
            }

            @Override
            public void onFailure(@NonNull Call<HorarisAPI> call, @NonNull Throwable t) {
                new Thread(() -> {
                    HorarisDatabase horarisDatabase = Room.databaseBuilder(ctx,
                            HorarisDatabase.class, Constants.ROOM_HORARIS_DATABASE)
                            .enableMultiInstanceInvalidation()
                            .build();

                    List<HorarisNit> horarisNit = new ArrayList<>(horarisDatabase.horarisNitDao().getAll());
                    horarisDatabase.close();

                    setHoraris(ctx, horarisNit);
                }).start();
            }
        });
    }

    public static void setHoraris(Context ctx, List<HorarisNit> horarisNit){
        // Aturem tots els workers d'Horaris que estiguin configurats
        WorkManager.getInstance(ctx)
                .cancelAllWorkByTag(Constants.WORKER_TAG_HORARIS_BLOCK);

        if(!Funcions.accessibilityServiceOn(ctx))
            return;

        AccessibilityScreenService.instance.setHorarisActius(false);

        HorarisDatabase horarisDatabase = Room.databaseBuilder(ctx,
                HorarisDatabase.class, Constants.ROOM_HORARIS_DATABASE)
                .enableMultiInstanceInvalidation()
                .build();

        HorarisNit horariAvui = horarisDatabase.horarisNitDao().findByDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));

        if(horariAvui == null){
            AccessibilityScreenService.instance.updateDeviceBlock();
            return;
        }

        int now = DateTime.now().getMillisOfDay();

        int wakeTimeDelay = horariAvui.despertar - now;
        int sleepTimeDelay = horariAvui.dormir - now;

        if(wakeTimeDelay > 0) {
            Funcions.setUpHorariWorker(ctx, wakeTimeDelay, false);
            Funcions.setUpHorariWorker(ctx, sleepTimeDelay, true);

            AccessibilityScreenService.instance.setHorarisActius(true);
        }
        else if(sleepTimeDelay > 0)
            Funcions.setUpHorariWorker(ctx, sleepTimeDelay, true);
        else
            AccessibilityScreenService.instance.setHorarisActius(true);

        AccessibilityScreenService.instance.updateDeviceBlock();
    }

    //POST usage information to server

    /**
     * Actualitza la bdd de room de l'ús d'apps del dia actual i envia al servidor si fa molt que s'ha fet. Retorna el general usage del dia actual
     */
    public static GeneralUsage sendAppUsage(Context ctx){
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(ctx);
        assert sharedPreferences != null;
        AdicticApi api = ((AdicticApp) ctx.getApplicationContext()).getAPI();

        // Agafem quants dies fa que no s'agafen dades (màxim 6)
        long lastMillisAppUsage = sharedPreferences.getLong(Constants.SHARED_PREFS_LAST_DAY_SENT_DATA, 0);

        // Si hem enviat dades fa menys de 1 minuts, no tornem a enviar, actualitzem bdd del dia d'avui
        if(System.currentTimeMillis() - lastMillisAppUsage < 1000*60)
            return Funcions.getGeneralUsages(ctx, 0).get(0);

        int daysToFetch;
        if(lastMillisAppUsage == 0)
            daysToFetch = 6;
        else {
            long lastDay = new DateTime(lastMillisAppUsage).withTimeAtStartOfDay().getMillis();
            long today = new DateTime().withTimeAtStartOfDay().getMillis();
            daysToFetch = Math.min(Math.round(TimeUnit.MILLISECONDS.toDays(Math.abs(today-lastDay))), 6);
        }

        // Agafem les dades
        List<GeneralUsage> gul = Funcions.getGeneralUsages(ctx, daysToFetch);

        long totalTime = gul.stream()
                .mapToLong(generalUsage -> generalUsage.totalTime)
                .sum();

//        long lastTotalUsage = sharedPreferences.getLong(Constants.SHARED_PREFS_LAST_TOTAL_USAGE, 0);

//        // Si és el mateix dia i no ha pujat més de 5 minuts el total, tornem
//        if(sameDay(lastMillisAppUsage, System.currentTimeMillis()) && totalTime - lastTotalUsage < 5 * 60 * 1000)
//            return;

        Call<String> call = api.sendAppUsage(sharedPreferences.getLong(Constants.SHARED_PREFS_IDUSER,-1), gul);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                super.onResponse(call, response);
                if(response.isSuccessful()) {
                    sharedPreferences.edit().putLong(Constants.SHARED_PREFS_LAST_TOTAL_USAGE, totalTime).apply();
                    sharedPreferences.edit().putLong(Constants.SHARED_PREFS_LAST_DAY_SENT_DATA, System.currentTimeMillis()).apply();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                super.onFailure(call, t);
            }
        });

        return gul.stream()
                .filter(generalUsage ->
                                generalUsage.day.equals(DateTime.now().getDayOfMonth()) &&
                                generalUsage.month.equals(DateTime.now().getMonthOfYear()) &&
                                generalUsage.year.equals(DateTime.now().getYear())
                        )
                .findFirst()
                .orElse(gul.get(0));
    }

    public static boolean sameDay(long millisDay1, long millisDay2){
        DateTime date1 = new DateTime(millisDay1);
        DateTime date2 = new DateTime(millisDay2);

        return date1.withTimeAtStartOfDay().getMillis() == date2.withTimeAtStartOfDay().getMillis();
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
            if (enabledServiceInfo.packageName.equals(mContext.getPackageName()) && enabledServiceInfo.name.equals(AccessibilityScreenService.class.getName()))
                return true;
        }

        String prefString = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(prefString!= null && prefString.contains(mContext.getPackageName() + "/" + AccessibilityScreenService.class.getName())) {
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

    public static int getDayAppUsage(Context mContext, String pkgName){
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar finalTime = Calendar.getInstance();

        Calendar initialTime = Calendar.getInstance();
        initialTime.set(Calendar.HOUR_OF_DAY, 0);
        initialTime.set(Calendar.MINUTE, 0);
        initialTime.set(Calendar.SECOND, 0);

        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, initialTime.getTimeInMillis(), finalTime.getTimeInMillis());

        UsageStats appUsageStats = stats.stream()
                .filter(usageStats -> usageStats.getPackageName().equals(pkgName))
                .findFirst()
                .orElse(null);

        if (appUsageStats == null)
            return 0;
        else
            return (int) appUsageStats.getTotalTimeInForeground();
    }

    // **************** WORKERS ****************

    // ForegroundService Worker

    public static void startServiceWorker(Context mCtx){
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(mCtx);
        assert sharedPreferences != null;

        if(sharedPreferences.getBoolean(Constants.SHARED_PREFS_ISTUTOR, true))
            return;

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(ServiceWorker.class, 20, TimeUnit.MINUTES)
                    .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork("ServiceWorker",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        myWork);
    }

    // AppUsageWorkers

    public static void startAppUsageWorker24h(Context mCtx){
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(AppUsageWorker.class, 24, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                5,
                                TimeUnit.MINUTES)
                        .addTag(Constants.WORKER_TAG_APP_USAGE)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork("pujarAppInfo",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        myWork);

        Log.d(TAG,"Worker AppUsage 24h Configurat");
    }

    // EventWorkers

    public static void setUpEventWorker(Context mContext, long delay, boolean startEvent){
        Data data = new Data.Builder()
                .putBoolean("start", startEvent)
                .build();

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(EventWorker.class, 7, TimeUnit.DAYS)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag(Constants.WORKER_TAG_EVENT_BLOCK)
                        .setInputData(data)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueue(myWork);

        Log.d(TAG,"setUpEventWorker Començat");
    }

    // Horaris Worker

    public static void startHorarisEventsManagerWorker(Context mCtx){
        long startOfDay = DateTime.now().withTimeAtStartOfDay().plusDays(1).getMillisOfDay() + 500;
        long delay = startOfDay - DateTime.now().getMillis();

        PeriodicWorkRequest myWork =
                new PeriodicWorkRequest.Builder(HorarisEventsWorkerManager.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(delay,TimeUnit.MILLISECONDS)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                30,
                                TimeUnit.SECONDS
                        )
                        .addTag(Constants.WORKER_TAG_HORARIS_EVENTS_MANAGER)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueueUniquePeriodicWork(Constants.WORKER_TAG_HORARIS_EVENTS_MANAGER,
                        ExistingPeriodicWorkPolicy.KEEP,
                        myWork);

        Log.d(TAG,"Worker HorarisEventManager Configurat");
    }

    private static void setUpHorariWorker(Context mContext, long delay, boolean startSleep){
        Data data = new Data.Builder()
                .putBoolean("start", startSleep)
                .build();

        WorkRequest myWork;

        myWork = new OneTimeWorkRequest.Builder(HorarisWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(mContext)
                .enqueue(myWork);

        Log.d(TAG,"setUpHorariWorker Començat");
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
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(GeoLocWorker.class)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork("geoLocWorkerOnce", ExistingWorkPolicy.REPLACE, myWork);
    }

    // **************** END WORKERS ****************


    public static boolean isXiaomi(){
        return Build.MANUFACTURER.equalsIgnoreCase("xiaomi");
    }

    public static boolean isMIUI(){
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            String miui = (String) get.invoke(c, "ro.miui.ui.version.code"); // maybe this one or any other

            // if string miui is not empty, bingo
            return miui != null && !miui.equals("");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void showBlockAppScreen(Context ctx, String pkgName, String appName) {
        // Si és MIUI
        try {
            if(Funcions.isXiaomi() && false)
                addOverlayView(ctx, false);
            else{
                Log.d(TAG,"Creant Intent cap a BlockAppActivity");
                Intent lockIntent = new Intent(ctx, BlockAppActivity.class);
                lockIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_REORDER_TO_FRONT);
                lockIntent.putExtra("pkgName", pkgName);
                lockIntent.putExtra("appName", appName);
                ctx.startActivity(lockIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void fetchAppBlockFromServer(Context mCtx){
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(mCtx);
        assert sharedPreferences != null;

        if(sharedPreferences.contains(Constants.SHARED_PREFS_IDUSER)) {
            AdicticApi mTodoService = ((AdicticApp) mCtx.getApplicationContext()).getAPI();
            long idChild = sharedPreferences.getLong(Constants.SHARED_PREFS_IDUSER, -1);
            Call<BlockedLimitedLists> call = mTodoService.getBlockedLimitedLists(idChild);
            call.enqueue(new Callback<BlockedLimitedLists>() {
                @Override
                public void onResponse(@NonNull Call<BlockedLimitedLists> call, @NonNull Response<BlockedLimitedLists> response) {
                    super.onResponse(call, response);
                    if (response.isSuccessful() && response.body() != null) {
                        new Thread(() -> updateDB_BlockedApps(mCtx, response.body())).start();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BlockedLimitedLists> call, @NonNull Throwable t) {
                    super.onFailure(call, t);
                }
            });
        }
    }

    private static void updateDB_BlockedApps(Context ctx, BlockedLimitedLists body) {
        if(!Funcions.accessibilityServiceOn(ctx))
            return;

        List<BlockedApp> blockedApps = new ArrayList<>();
        List<String> BlockedAppsList = body.blockedApps != null ? body.blockedApps : new ArrayList<>();
        for(String blockedApp : BlockedAppsList) {
            BlockedApp app = new BlockedApp();
            app.pkgName = blockedApp;
            app.timeLimit = 0L;
            blockedApps.add(app);
        }

        List<LimitedApps> listLimitedApps = body.limitApps != null ? body.limitApps : new ArrayList<>();
        for(LimitedApps limitedApps : listLimitedApps){
            BlockedApp app = new BlockedApp();
            app.pkgName = limitedApps.name;
            app.timeLimit = limitedApps.time;
            blockedApps.add(app);
        }

        AppDatabase appDatabase = Room.databaseBuilder(ctx,
                AppDatabase.class, Constants.ROOM_APP_DATABASE)
                .enableMultiInstanceInvalidation()
                .fallbackToDestructiveMigration()
                .build();

        appDatabase.blockedAppDao().update(blockedApps);
        GeneralUsage generalUsage = Funcions.getGeneralUsages(ctx, 0).get(0);

        List<BlockedApp> listBlockedApps = new ArrayList<>(appDatabase.blockedAppDao().getAll());
        appDatabase.close();

        Map<String, Long> mapUsage = getTimeLeftMapAccessibilityService(listBlockedApps, generalUsage);

        AccessibilityScreenService.instance.setTimeLeftMap(mapUsage);
        AccessibilityScreenService.instance.isCurrentAppBlocked();
    }

    public static Map<String, Long> getTimeLeftMapAccessibilityService(List<BlockedApp> listBlockedApps, GeneralUsage generalUsage) {
        Map<String, Long> mapUsage = new HashMap<>();
        for(BlockedApp blockedApp : listBlockedApps){
            AppUsage app= generalUsage.usage.stream()
                    .filter(appUsage -> appUsage.app.pkgName.equals(blockedApp.pkgName))
                    .findFirst().orElse(null);
            if(app == null)
                continue;

            long timeUsed = app.totalTime;

            mapUsage.put(blockedApp.pkgName, blockedApp.timeLimit - timeUsed);
        }

        return mapUsage;
    }

    public static boolean accessibilityServiceOn(Context mCtx){
        boolean res = AccessibilityScreenService.instance != null;

        if(!res){
            NotificationInformation notif = new NotificationInformation();
            notif.title = mCtx.getString(R.string.notif_accessibility_error_title);
            notif.message = mCtx.getString(R.string.notif_accessibility_error_body);
            notif.important = true;
            notif.dateMillis = System.currentTimeMillis();
            notif.read = false;
            notif.notifCode = Constants.NOTIF_SETTINGS_ACCESSIBILITY_ERROR;

            sendNotifToParent(mCtx, notif);
        }

        return res;
    }

    public static void sendNotifToParent(Context mCtx, NotificationInformation notif) {
        AdicticApi api = ((AdicticApp) mCtx.getApplicationContext()).getAPI();

        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(mCtx);
        assert sharedPreferences != null;

        long idChild = sharedPreferences.getLong(Constants.SHARED_PREFS_IDUSER, -1);

        Call<String> call = api.sendNotification(idChild, notif);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                super.onResponse(call, response);
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                startNotificationWorker(mCtx, notif, idChild);
            }
        });
    }

    public static void startNotificationWorker(Context mCtx, NotificationInformation notif, Long idChild){
        Data data = new Data.Builder()
                .putString("title", notif.title)
                .putString("body", notif.message)
                .putBoolean("important", notif.important)
                .putLong("dateMillis", notif.dateMillis)
                .putLong("idChild", idChild)
                .putString("notifCode", notif.notifCode)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        WorkRequest myWork =
                new OneTimeWorkRequest.Builder(NotifWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                5,
                                TimeUnit.MINUTES)
                        .setInputData(data)
                        .build();

        WorkManager.getInstance(mCtx)
                .enqueue(myWork);
    }

    public static Map<String, Long> getTodayAppUsage(Context mContext) {
        long startOfDay = new DateTime().withTimeAtStartOfDay().getMillis();
        long now = System.currentTimeMillis();

        List<AppUsage> listUsages = getAppUsages(mContext, startOfDay, now).first;

        return listUsages.stream()
                .collect(Collectors.toMap(appUsage -> appUsage.app.pkgName,appUsage -> appUsage.totalTime));
    }
}
