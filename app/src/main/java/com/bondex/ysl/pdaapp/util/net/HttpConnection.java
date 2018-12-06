package com.bondex.ysl.pdaapp.util.net;

import com.bondex.ysl.pdaapp.application.PdaApplication;
import com.bondex.ysl.pdaapp.bean.HttpRequestParam;
import com.bondex.ysl.pdaapp.util.interf.HtppReuquest;
import com.bondex.ysl.pdaapp.util.netutil.ParamUtils;
import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class HttpConnection {

    private static final String BASE_URL = "http://wol.bondex.com.cn:8089/";
    private static final String VERSION_URL = "http://pubdoc.bondex.com.cn:8087/fileking/app/";


    private static final OkHttpClient httpClient = new OkHttpClient
            .Builder()
            .connectTimeout(30000, TimeUnit.SECONDS)
            .build();

    public static NetApi getRretrofit(String url) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(httpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        return retrofit.create(NetApi.class);

    }


    public static Call<String> login(String name, String password) {

        JSONObject json = new JSONObject();
        try {
            json.put("userid", name);
            json.put("pwd", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String param = json.toString();
        param = ParamUtils.getParams(param, "login");
        Logger.i("登录参数 " + param);

        return getRretrofit(BASE_URL).connect(param);
    }




    public static Call<String> consigement(String id, int num) {

        String method = "so.shipment";//方法名

        JSONObject map = new JSONObject();

        try {
            map.put("warehouseno", Integer.valueOf(num));//当前登录用户选择的仓库ID
            map.put("Action", "Ship");//固定值
            map.put("ProcessBy", "OrderNo");//固定值
            map.put("userid", PdaApplication.LOGINBEAN.getUserid());//系统当前登录人ID
            map.put("OrderNO", id);//第一步扫描的发运单号

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String params = map.toString();
        params = ParamUtils.getParams(params, method);

        return getRretrofit(BASE_URL).connect(params);
    }

    public static Call<String> getVersion() {


        return getRretrofit(VERSION_URL).getVersion();
    }

    public static Call<String> getCall(String params) {

        return getRretrofit(BASE_URL).connect(params);
    }

    public static void connect(String params, HtppReuquest request) {


        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {

                getCall(params).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {


                        if (response.body() == null) {

                            emitter.onNext("N");
                        } else {

                            emitter.onNext(response.body());

                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {

                        emitter.onNext("N");
                    }
                });
            }
        });
        Consumer<String> consumer = new Consumer<String>() {
            @Override
            public void accept(String s) {

                if (s.equals("N")) {

                    request.httpError("连接服务器失败");
                } else {

                    Gson gson = new Gson();
                    HttpRequestParam param = gson.fromJson(s, HttpRequestParam.class);
                    request.httpSuccess(param);
                }

            }
        };
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer);


    }


}
