package com.adictic.common.util;

import static androidx.security.crypto.MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE;
import static androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS;

import android.graphics.Color;
import android.widget.TextView;

import com.adictic.common.R;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

public class Constants {
    public static final long HOUR_IN_MILLIS = 3600000;
    public static final int TOTAL_MILLIS_IN_DAY = 86400000;

    public static final String MASTER_KEY_ALIAS = DEFAULT_MASTER_KEY_ALIAS;
    public static final int KEY_SIZE = DEFAULT_AES_GCM_MASTER_KEY_SIZE;

    public static final HashMap<Integer, String> DAYS_NAME = initializeDaysName();
    public static final String CRYPT_KEY = "";

    private static HashMap<Integer, String> initializeDaysName() {
        HashMap<Integer, String> map = new HashMap<>();
        for(int i = 1; i <= 7; i++) {
            if(i == Calendar.MONDAY)
                map.put(i, App.getRes().getString(R.string.monday));
            else if (i == Calendar.TUESDAY)
                map.put(i, App.getRes().getString(R.string.tuesday));
            else if (i == Calendar.WEDNESDAY)
                map.put(i, App.getRes().getString(R.string.wednesday));
            else if (i == Calendar.THURSDAY)
                map.put(i, App.getRes().getString(R.string.thursday));
            else if (i == Calendar.FRIDAY)
                map.put(i, App.getRes().getString(R.string.friday));
            else if (i == Calendar.SATURDAY)
                map.put(i, App.getRes().getString(R.string.saturday));
            else
                map.put(i, App.getRes().getString(R.string.sunday));
        }
        return map;
    }

    /** NOTIFICACIONS **/

    public static final String NOTIF_SETTINGS_CHAT = "notif_chat";

    // Notificacions dispositiu tutor
    public static final String NOTIF_SETTINGS_INSTALL_APPS = "notif_installApps";
    public static final String NOTIF_SETTINGS_UNINSTALL_APPS = "notif_uninstallApps";
    public static final String NOTIF_SETTINGS_ACCESSIBILITY_ERROR = "notif_accessibilityError";
    public static final String NOTIF_SETTINGS_DISABLE_ADMIN = "notif_disableAdmin";

    // Notificacions dispostiu fill
    public static final String NOTIF_SETTINGS_DAILY_LIMIT = "notif_dailyLimit";
    public static final String NOTIF_SETTINGS_BLOCK_DEVICE = "notif_blockDevice";
    public static final String NOTIF_SETTINGS_FREE_USE = "notif_freeUse";
    public static final String NOTIF_SETTINGS_BLOCK_APP = "notif_blockApp";
    public static final String NOTIF_SETTINGS_HORARIS = "notif_horaris";
    public static final String NOTIF_SETTINGS_EVENTS = "notif_events";

    /** FITXERS **/

    public static final String FILE_LIMITED_APPS = "limited_apps";
    public static final String FILE_BLOCKED_APPS = "blocked_apps";
    public static final String FILE_EVENT_BLOCK = "event_block";
    public static final String FILE_HORARIS_NIT = "horaris_nit";
    public static final String FILE_CURRENT_BLOCKED_APPS = "current_blocked_apps";

    public static final String SHARED_PREFS_ID_ADMIN = "adminId";
    public static final String SHARED_PREFS_IS_SUPERADMIN = "isSuperAdmin";
    public static final String SHARED_PREFS_USERNAME = "username";
    public static final String SHARED_PREFS_PASSWORD = "password";
    public static final String SHARED_PREFS_TOKEN = "firebaseToken";

    public static final String SHARED_PREFS_BIOMETRIC_AUTH = "biometricAuth";

    public static final String SHARED_PREFS_IDUSER = "idUser";
    public static final String SHARED_PREFS_IDTUTOR = "idTutor";
    public static final String SHARED_PREFS_ISTUTOR = "isTutor";
    public static final String SHARED_PREFS_BLOCKEDDEVICE = "blockedDevice";
    public static final String SHARED_PREFS_BLOCKEDDEVICE_START = "blockedDeviceStart";
    public static final String SHARED_PREFS_FREEUSE = "freeuse";
    public static final String SHARED_PREFS_FREEUSE_START = "shared_prefs_freeuse_start";
    public static final String SHARED_PREFS_DAYOFYEAR = "dayOfYear";
    public static final String SHARED_PREFS_LAST_DAY_SENT_DATA = "daysToSendData";
    public static final String SHARED_PREFS_LIVEAPP = "liveApp";
    public static final String SHARED_PREFS_LAST_TOTAL_USAGE = "lastTotalUsage";
    public static final String SHARED_PREFS_DAILY_USAGE_LIMIT = "dailyUsageLimit";

    public static final String SHARED_PREFS_NOTIFS = "notificationList";

    public static final String SHARED_PREFS_PATCH_NOTES = "patch_notes";

    public static final String SHARED_PREFS_APPUSAGEWORKERUPDATE = "appUsageWorkerUpdate";
    public static final String SHARED_PREFS_LASTUPDATEAPPUSAGEWORKER = "lastUpdateAppUsageWorker";

    public static final String SHARED_PREFS_ACTIVE_EVENTS = "current_active_events";
    public static final String SHARED_PREFS_ACTIVE_HORARIS_NIT = "blockedDeviceNit";

    public static final int[] GRAPH_COLORS = {
            Color.parseColor("#3c9df8"), Color.parseColor("#deefff"), Color.parseColor("#76b3ec"),
            Color.parseColor("#2390F5"), Color.parseColor("#1b62a5")
    };

    public static final String CHANNEL_ID = "my_channel_01";
    public static final String CHANNEL_NAME = "Standard Notification";
    public static final String CHANNEL_DESCRIPTION = "Trying out notifications";

    public static final String WORKER_TAG_BLOCK_APPS = "blocked_apps_worker_tag";
    public static final String WORKER_TAG_EVENT_BLOCK = "event_block_worker_tag";
    public static final String WORKER_TAG_APP_USAGE = "app_usage_worker_tag";
    public static final String WORKER_TAG_EVENT_MANAGER = "event_manager_tag";
    public static final String WORKER_TAG_HORARIS_BLOCK = "horaris_worker_tag";
    public static final String WORKER_TAG_HORARIS_EVENTS_MANAGER = "horaris_manager_tag";
    public static final String WORKER_TAG_GEOLOC_PERIODIC = "geoloc_worker_periodic_tag";

    public static final String FOREGROUND_SERVICE_ACTION_DEVICE_BLOCK_SCREEN = "device_block_screen";
    public static final String NO_DEVICE_BLOCK_SCREEN = "no_device_block_screen";
    public static final String NO_APP_BLOCK_SCREEN = "no_app_block_screen";

    public static int CORRECT_USAGE_DAY = 3;
    public static int DANGEROUS_USAGE_DAY = 5;
    public static int CORRECT_USAGE_APP = 2;
    public static int DANGEROUS_USAGE_APP = 4;

    public static final long[] AGE_TIMES_MILLIS = createAgeTimesMillis();
    public static final String[] AGE_TIMES_STRING = createAgeTimesString();

    private static long[] createAgeTimesMillis(){
        long[] array = new long[30];
        Arrays.fill(array, 0, 3, 0);
        array[3] = Math.round(0.5 * HOUR_IN_MILLIS);
        array[4] = Math.round(0.75 * HOUR_IN_MILLIS);
        Arrays.fill(array, 5, 13, HOUR_IN_MILLIS);
        Arrays.fill(array, 13, 16, Math.round(1.5 * HOUR_IN_MILLIS));
        Arrays.fill(array, 16, 30, 2 * HOUR_IN_MILLIS);

        return array;
    }

    private static String[] createAgeTimesString(){
        String[] array = new String[30];
        Arrays.fill(array, 0, 3, "0 " + App.getRes().getString(R.string.hours));
        array[3] = "30 " + App.getRes().getString(R.string.minutes);
        array[4] = "45 " + App.getRes().getString(R.string.minutes);
        Arrays.fill(array, 5, 13, "1 " + App.getRes().getString(R.string.hours));
        Arrays.fill(array, 13, 16, "1 " + App.getRes().getString(R.string.hours) + "30 " + App.getRes().getString(R.string.minutes));
        Arrays.fill(array, 16, 30, "2 " + App.getRes().getString(R.string.hours));

        return array;
    }
}
