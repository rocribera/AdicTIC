package com.example.adictic.util;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.adictic.R;
import com.example.adictic.entity.AppInfo;
import com.example.adictic.entity.AppUsage;
import com.example.adictic.entity.GeneralUsage;
import com.example.adictic.entity.Horaris;
import com.example.adictic.entity.HorarisEvents;
import com.example.adictic.entity.MonthEntity;
import com.example.adictic.entity.TimeDay;
import com.example.adictic.entity.WakeSleepLists;
import com.example.adictic.entity.YearEntity;
import com.example.adictic.rest.TodoApi;
import com.example.adictic.roomdb.EventBlock;
import com.example.adictic.roomdb.HorarisNit;
import com.example.adictic.roomdb.RoomRepo;
import com.example.adictic.service.FinishBlockEventWorker;
import com.example.adictic.service.GeoLocWorker;
import com.example.adictic.service.LimitAppsWorker;
import com.example.adictic.service.StartBlockEventWorker;
import com.example.adictic.service.WindowChangeDetectingService;

import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Funcions {

    private static void setHoraris(Context ctx, List<HorarisNit> list) {
        //per cada dia (Sunday = 1) -> (Saturday = 7)
        for(HorarisNit horarisNit : list){
            RoomRepo roomRepo = new RoomRepo(ctx.getApplicationContext());
            roomRepo.insertHorarisNit(horarisNit);
        }
    }

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
        String data = "";
        if (dia < 10) data = "0" + dia + "-";
        else data = dia + "-";
        if (mes < 10) data += "0" + mes + "-";
        else data += mes + "-";

        return data + any;
    }

    public static void setIconDrawable(Context ctx, String pkgName, final ImageView d) {
        TodoApi mTodoService = ((TodoApp) (ctx.getApplicationContext())).getAPI();

        Call<ResponseBody> call = mTodoService.getIcon(pkgName);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(response.body().byteStream());
                    d.setImageBitmap(bmp);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    public static void checkHoraris(Context ctx) {

        TodoApi mTodoService = ((TodoApp) (ctx.getApplicationContext())).getAPI();

        Call<Horaris> call = mTodoService.getHoraris(TodoApp.getIDChild());

        call.enqueue(new Callback<Horaris>() {
            @Override
            public void onResponse(@NonNull Call<Horaris> call, @NonNull Response<Horaris> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setHoraris(ctx, response.body().horarisNits);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Horaris> call, @NonNull Throwable t) {

            }
        });
    }

    // To check if app has PACKAGE_USAGE_STATS enabled
    public static boolean isAppUsagePermissionOn(Context mContext) {
        boolean granted = false;
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

        Pair<Integer, Integer> res = new Pair<>(hores, Math.round(minuts));
        return res;
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

        Boolean found = false;
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

//    public static HorarisEvents getEventFromList(String name) {
//        RoomRepo roomRepo = new RoomRepo()
//        boolean trobat = false;
//        int i = 0;
//        List<HorarisEvents> listEvents = TodoApp.getListEvents();
//        HorarisEvents event = null;
//        while (!trobat && i < listEvents.size()) {
//            event = listEvents.get(i);
//            if (event.name.equals(name)) trobat = true;
//        }
//        return event;
//    }

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
        RoomRepo roomRepo = new RoomRepo(mContext);

//        // Transformem la List<HorarisEvents> en List<EventBlock> per poder interactuar amb repo
//
//        List<EventBlock> eventBlockList = horarisEvents2EventBlock(newEvents);
        List<EventBlock> currentEvents = roomRepo.getAllEventBlocks();

        List<EventBlock> disjunctionEvents = new ArrayList<>(CollectionUtils.disjunction(newEvents, currentEvents));

        WorkManager workManager = WorkManager.getInstance(mContext);

        for (EventBlock event : disjunctionEvents) {
            int index = newEvents.indexOf(event);

            // Event s'ha esborrat
            if (index == -1) {
                workManager.cancelUniqueWork(event.name);
                roomRepo.deleteEventBlock(event);
            }
            // És un nou event
            else if (event.exactSame(newEvents.get(index))) {
//                Pair<Integer, Integer> startEvent = stringToTime(event.start);
//                Pair<Integer, Integer> finishEvent = stringToTime(event.finish);
//
//                Calendar start = Calendar.getInstance();
//                start.set(Calendar.HOUR_OF_DAY, startEvent.first);
//                start.set(Calendar.MINUTE, startEvent.second);
//
//                Calendar finish = Calendar.getInstance();
//                finish.set(Calendar.HOUR_OF_DAY, finishEvent.first);
//                finish.set(Calendar.MINUTE, finishEvent.second);

                long now = DateTime.now().getMillisOfDay();

                if (now < event.startEvent) {
                    runStartBlockEventWorker(mContext, event.name, event.startEvent - now);
                } else if (now < event.endEvent) {
                    runFinishBlockEventWorker(mContext, event.name, event.endEvent - now);
                }
//                } else {
//                    start.add(Calendar.DATE, 1);
//                    runStartBlockEventWorker(mContext, event.name, start.getTimeInMillis() - now);
//                }

                roomRepo.insertEventBlock(event);
            }
        }
    }

    public static void runStartBlockEventWorker(Context mContext, String name, long delay) {
        Data.Builder data = new Data.Builder();
        data.putString("name", name);

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(StartBlockEventWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, myWork);
    }

    public static void runFinishBlockEventWorker(Context mContext, String name, long delay) {
        Data.Builder data = new Data.Builder();
        data.putString("name", name);

        OneTimeWorkRequest myWork =
                new OneTimeWorkRequest.Builder(FinishBlockEventWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, myWork);
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
                AppInfo app = new AppInfo();
                appUsage.app = app;

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

    public static void updateLimitedAppsList() {
        long millisToAdd = Calendar.getInstance().getTimeInMillis() - TodoApp.getStartFreeUse();
        Map<String, Long> newMap = new HashMap<>();
        for (Map.Entry<String, Long> entry : TodoApp.getLimitApps().entrySet()) {
            newMap.put(entry.getKey(), entry.getValue() + millisToAdd);
        }

        TodoApp.setStartFreeUse(0);
        TodoApp.setLimitApps(newMap);
    }

    public static void startFreeUseLimitList(Context mContext) {
        List<GeneralUsage> gul = getGeneralUsages(mContext, 0, -1);
        GeneralUsage gu = gul.get(0);

        List<AppUsage> appUsages = (List<AppUsage>) gu.usage;

        Map<String, Long> newMap = new HashMap<>();

        for (Map.Entry<String, Long> entry : TodoApp.getLimitApps().entrySet()) {
            AppUsage appUsage = appUsages.get(appUsages.indexOf(entry.getKey()));

            newMap.put(entry.getKey(), entry.getValue() - appUsage.totalTime);
        }

        TodoApp.setStartFreeUse(Calendar.getInstance().getTimeInMillis());
        TodoApp.setLimitApps(newMap);
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

    public static void closeKeyboard(View view, Activity a) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(a);
                    return false;
                }
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
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful()) {
                    Toast toast = Toast.makeText(ctx, ctx.getString(R.string.error_liveApp), Toast.LENGTH_LONG);
                    toast.show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast toast = Toast.makeText(ctx, ctx.getString(R.string.error_liveApp), Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public static String getFullURL(String url) {
        if (!url.contains("https://")) return "https://" + url;
        else return url;
    }
}
