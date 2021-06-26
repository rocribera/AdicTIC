package com.example.adictic_admin;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import com.example.adictic_admin.entity.UserLogin;
import com.example.adictic_admin.rest.Api;
import com.example.adictic_admin.util.Constants;
import com.example.adictic_admin.util.Global;
import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class App extends Application {
    private Api mTodoService;

    private static Drawable adminPic = null;
    public static Drawable getAdminPic() { return adminPic; }
    public static void setAdminPic(Drawable d) { adminPic = d; }

    private static SharedPreferences sharedPreferences = null;

    public static SharedPreferences getSharedPreferences() { return  sharedPreferences; }
    public static void setSharedPreferences(SharedPreferences sharedPreferences1) { sharedPreferences = sharedPreferences1; }

    public static String[] newFeatures = {

    };

    public static String[] fixes = {
            "Ja no peta quan es miren els horaris nocturns del fill",
            "El percentatge de l'informe funciona bé"
    };

    public static String[] changes = {

    };

    @Override
    public void onCreate() {
        super.onCreate();

        OkHttpClient httpClient = getOkHttpClient();

        Gson gson = new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        String URL = Global.BASE_URL_RELEASE;
        if(BuildConfig.DEBUG) URL = Global.BASE_URL_DEBUG;

        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient)
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        mTodoService = retrofit.create(Api.class);
    }

    public Api getAPI() {
        return mTodoService;
    }

    public OkHttpClient getOkHttpClient() {

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        if(BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            httpClient.addInterceptor(interceptor);
        }

        ClearableCookieJar cookieJar =
                new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this));

        return httpClient
                .cookieJar(cookieJar)
                .authenticator((route, response) -> {
                    if (responseCount(response) >= 3) {
                        return null; // If we've failed 3 times, give up.
                    }

                    String username = sharedPreferences.getString(Constants.SHARED_PREFS_USERNAME,null);
                    String password = sharedPreferences.getString(Constants.SHARED_PREFS_PASSWORD, null);

                    if(username != null && password != null) {
                        System.out.println("Authenticating for response: " + response);
                        System.out.println("Challenges: " + response.challenges());

                        UserLogin userLogin = new UserLogin();
                        userLogin.username = username;
                        userLogin.password = password;
                        userLogin.token = sharedPreferences.getString(Constants.SHARED_PREFS_TOKEN, "");

                        String gson = new Gson().toJson(userLogin);
                        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                        RequestBody body = RequestBody.create(gson, JSON);

                        String url = BuildConfig.DEBUG ? Global.BASE_URL_DEBUG : Global.BASE_URL_RELEASE;
                        url += "users/loginAdmin";

                        return response.request().newBuilder()
                                .url(url)
                                .post(body)
                                .build();
                    }

                    return null;
                })
                .build();
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
