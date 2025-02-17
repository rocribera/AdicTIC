package com.adictic.client.rest;

import com.adictic.common.entity.NotificationInformation;
import com.adictic.common.entity.AppInfo;
import com.adictic.common.entity.AppTimesAccessed;
import com.adictic.common.entity.BlockAppEntity;
import com.adictic.common.entity.BlockedLimitedLists;
import com.adictic.common.entity.ChangePassword;
import com.adictic.common.entity.ChatsMain;
import com.adictic.common.entity.EventsAPI;
import com.adictic.common.entity.FillNom;
import com.adictic.common.entity.GeneralUsage;
import com.adictic.common.entity.GeoFill;
import com.adictic.common.entity.HorarisAPI;
import com.adictic.common.entity.HorarisEvents;
import com.adictic.common.entity.LiveApp;
import com.adictic.common.entity.Localitzacio;
import com.adictic.common.entity.NouFillLogin;
import com.adictic.common.entity.Oficina;
import com.adictic.common.entity.User;
import com.adictic.common.entity.UserLogin;
import com.adictic.common.entity.UserMessage;
import com.adictic.common.entity.UserRegister;
import com.adictic.common.entity.VellFillLogin;
import com.adictic.common.entity.YearEntity;

import java.util.Collection;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface AdicticApi extends com.adictic.common.rest.Api {
    @POST("/users/login")
    Call<User> login(@Body UserLogin login);

    // Si és tutor -> idChild = -1
    @POST("/users/token/{idChild}")
    Call<String> updateToken(@Path("idChild") Long idChild, @Body String token);

    @POST("/users/check")
    Call<User> checkWithToken(@Body String token);

    @POST("/users/register")
    Call<String> register(@Body UserRegister register);

    @POST("/users/{id}/child")
    Call<String> sendOldName(@Path("id") Long id, @Body VellFillLogin fill);

    @PUT("/users/{id}/child")
    Call<Long> sendNewName(@Path("id") Long id, @Body NouFillLogin fill);

    @GET("/usage/{id}/{xDays}")
    Call<Collection<GeneralUsage>> getAppUsage(@Path("id") Long childId, @Path("xDays") Integer xDays);

    /**
     * format {dd-mm-aaaa} o {mm-aaaa} per tot el mes
     **/
    @GET("/usage/{id}/{dataInicial}/{dataFinal}")
    Call<Collection<GeneralUsage>> getGenericAppUsage(@Path("id") Long childId, @Path("dataInicial") String dataInicial, @Path("dataFinal") String dataFinal);

    @POST("/usage/{id}")
    Call<String> sendAppUsage(@Path("id") Long childId, @Body Collection<GeneralUsage> appUsage);

    @GET("/users/{id}/blockedLists")
    Call<BlockedLimitedLists> getBlockedLimitedLists(@Path("id") Long childId);

    @GET("/users/{id}/child")
    Call<Collection<FillNom>> getUserChilds(@Path("id") Long userId);

    @GET("/users/{idTutor}/{idChild}")
    Call<Collection<FillNom>> getChildInfo(@Path("idTutor") Long idTutor, @Path("idChild") Long idChild);

    @POST("/users/{id}/callBlockedApp")
    Call<String> callBlockedApp(@Path("id") Long childId, @Body String packageName);

    @GET("/usage/{id}/daysUsage")
    Call<List<YearEntity>> getDaysWithData(@Path("id") Long childId);

    @POST("/usage/{id}/installedApps")
    Call<String> postInstalledApps(@Path("id") Long childId, @Body Collection<AppInfo> appInfos);

    @GET("/usage/{idChild}/installedApps")
    Call<Collection<AppInfo>> getInstalledApps(@Path("id") Long childId);

    @PUT("/usage/{id}/liveApp")
    Call<String> sendTutorLiveApp(@Path("id") Long childId, @Body LiveApp login);

    @GET("/users/{idChild}/blockStatus")
    Call<Boolean> getBlockStatus(@Path("idChild") Long childId);

    @GET("/usage/{id}/horaris")
    Call<HorarisAPI> getHoraris(@Path("id") Long childId);

    @GET("/usage/{id}/events")
    Call<EventsAPI> getEvents(@Path("id") Long childId);

    @GET("/usage/{id}/horarisEvents")
    Call<HorarisEvents> getHorarisEvents(@Path("id") Long childId);

    @POST("/icons/{pkgName}")
    @Multipart
    Call<String> postIcon(@Path("pkgName") String pkgName, @Part MultipartBody.Part file);

    @GET("/icons/{pkgName}")
    Call<ResponseBody> getIcon(@Path("pkgName") String pkgName);

    @GET("/usage/{idChild}/blockedApps")
    Call<Collection<BlockAppEntity>> getBlockApps(@Path("idChild") Long childId);

    @GET("/users/{idChild}/age")
    Call<Integer> getAge(@Path("idChild") Long idChild);

    @GET("/usage/{idChild}/timesTried")
    Call<List<AppTimesAccessed>> getAccessBlocked(@Path("idChild") Long childId);

    @GET("/users/geoloc")
    Call<List<GeoFill>> getGeoLoc();

    @POST("/users/geoloc")
    Call<String> postGeolocActive(@Body Boolean b);

    @POST("/users/geoloc/{idChild}")
    Call<String> postCurrentLocation(@Path("idChild") Long idChild, @Body GeoFill geoFill);

    @GET("/poblacions")
    Call<Collection<Localitzacio>> getLocalitzacions();

    ////////////////////////////////////
    //Chat
    ///////////////////////////////////

    @GET("/message/client/{childId}/info")
    Call<ChatsMain> getChatsInfo(@Path("childId") Long childId);

    @POST("/message/client/{childId}/{adminId}/close")
    Call<String> closeChat(@Path("adminId") Long idUserAdmin, @Path("childId") Long idChild);

    @GET("/message/client/{childId}/{adminId}")
    Call<List<UserMessage>> getMyMessagesWithUser(@Path("childId") Long childId, @Path("adminId") Long adminId);

    @POST("/message/client/{childId}/{adminId}")
    Call<String> sendMessageToUser(@Path("childId") Long childId, @Path("adminId") Long adminId, @Body UserMessage value);

    ///////////////////////////////////

    @GET("/admins/pictures/{id}")
    Call<ResponseBody> getAdminPicture(@Path("id") Long id);

    @GET("/usage/{idChild}/lastAppUsed")
    Call<LiveApp> getLastAppUsed(@Path("idChild") Long idChild);

    @POST("/usage/{idChild}/lastAppUsed")
    Call<String> postLastAppUsed(@Path("idChild") Long idChild, @Body LiveApp liveApp);

    @POST("/users/changePassword")
    Call<String> changePassword(@Body ChangePassword cp);

    @POST("/usage/{idChild}/installedApp")
    Call<String> postAppInstalled(@Path("idChild") Long idChild, @Body AppInfo appInfo);

    @POST("/usage/{idChild}/uninstalledApp")
    Call<String> postAppUninstalled(@Path("idChild") Long idChild, @Body String pkgName);

    @POST("/update/adictic")
    Call<String> checkForUpdates(@Body String version);

    @GET("/update/adictic")
    Call<ResponseBody> getLatestVersion();

    @GET("/usage/{idChild}/dailyLimit")
    Call<Integer> getDailyLimit(@Path("idChild") Long idChild);

    @PUT("/usage/{idChild}/timesUnlocked")
    Call<String> addTimeUnlocked(@Path("idChild") Long idChild, @Body Long dateTime);

    @POST("/users/checkPassword")
    Call<String> checkPassword(@Body UserLogin login);

    @POST("users/notification/{idChild}")
    Call<String> sendNotification(@Path("idChild") Long idChild, @Body NotificationInformation notificationInformation);
}
