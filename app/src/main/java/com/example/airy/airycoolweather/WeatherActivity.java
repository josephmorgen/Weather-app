package com.example.airy.airycoolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.airy.airycoolweather.gson.DailyForecast;
import com.example.airy.airycoolweather.gson.HourlyForecast;
import com.example.airy.airycoolweather.gson.Weather;
import com.example.airy.airycoolweather.service.AutoUpdateService;
import com.example.airy.airycoolweather.util.HttpUtil;
import com.example.airy.airycoolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class WeatherActivity extends AppCompatActivity {

    private String mWeatherId;
    private LinearLayout dailyForecastLayout;
    private ScrollView weatherLayout;
    public SwipeRefreshLayout swipeRefresh;
    public DrawerLayout drawerLayout;
    //天气的界面控件

    private Button navButton;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private TextView dailyWindDir;
    private TextView dailyWindSc;
    private TextView dailyWindSpd;
    //daily forecast

    private LinearLayout hourlyForecastLayout;
    //hourly forecast

    private ImageView bingPicImg;

    @Override
    //状态栏样式沉浸
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);


        //初始化控件，在自己写的layout上找到自己写的控件并初始化
        dailyForecastLayout=(LinearLayout)findViewById(R.id.daily_forecast_layout);
        weatherLayout=(ScrollView)findViewById(R.id.weather_layout);
        titleCity=(TextView)findViewById(R.id.title_city);
        titleUpdateTime=(TextView)findViewById(R.id.title_update_time);
        degreeText=(TextView)findViewById(R.id.degree_text);
        weatherInfoText=(TextView)findViewById(R.id.weather_info_text);
        aqiText=(TextView)findViewById(R.id.aqi_text);
        pm25Text=(TextView)findViewById(R.id.pm25_text);
        comfortText=(TextView)findViewById(R.id.comfort_text);
        carWashText=(TextView)findViewById(R.id.car_wash_text);
        sportText=(TextView)findViewById(R.id.sport_text);
        hourlyForecastLayout=(LinearLayout)findViewById(R.id.hourly_forecast_layout);
        swipeRefresh= (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton= (Button) findViewById(R.id.nav_button);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        bingPicImg= (ImageView) findViewById(R.id.bing_pic_img);

        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString =preferences.getString("weather",null);
        if (weatherString!=null){
            //有缓存
            Weather weather= Utility.handlWeatherResponse(weatherString);
            mWeatherId=weather.getBasic().getId();
            showWeatherInfo(weather);
        }else{
            //无缓存
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        String bingPic=preferences.getString("bing_pic",null);
        if (bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic();
        }

    }

    //根据id请求城市天气
    public void requestWeather(final String weatherId){
        //我的api
        String weatherUrl="https://free-api.heweather.com/v5/weather?city="+weatherId+"&key=65154046739f4cd08f5d691bfcbd1c65";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息扑街",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            //回去缓存天气信息和背景图
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather= Utility.handlWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.getStatus())){
                            SharedPreferences.Editor editor= PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息扑街",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    //处理展示天气数据
    //还需要再添加，新的api
    private void showWeatherInfo(Weather weather){
        if (weather!=null&&"ok".equals(weather.getStatus())) {
            String cityName = weather.getBasic().getCity();
            String updateTime = weather.getBasic().getUpdate().getLoc();
            String degree = weather.getNow().getTmp() + "°C";
            String weatherInfo = weather.getNow().getCond().getTxt();
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            dailyForecastLayout.removeAllViews();
            hourlyForecastLayout.removeAllViews();
            for (DailyForecast dailyForecast : weather.getDaily_forecast()) {
                View view = LayoutInflater.from(this).inflate(R.layout.daily_forecast_item, dailyForecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);
                dateText.setText(dailyForecast.getDate().split("-")[2]+"日");
                infoText.setText(dailyForecast.getCond().getTxt_d() + " " + dailyForecast.getCond().getTxt_n());
                maxText.setText(dailyForecast.getTmp().getMax());
                minText.setText(dailyForecast.getTmp().getMin());
                dailyForecastLayout.addView(view);
            }
            if (weather.getHourly_forecast().size() > 0) {
                for (HourlyForecast hourlyForecast : weather.getHourly_forecast()) {
                    View view = LayoutInflater.from(this).inflate(R.layout.hourly_forecast_item, hourlyForecastLayout, false);
                    TextView hourlyText = (TextView) view.findViewById(R.id.hourly_text);
                    TextView hourlyCondText = (TextView) view.findViewById(R.id.hourly_cond_text);
                    TextView hourlyTmpText = (TextView) view.findViewById(R.id.hourly_tmp_text);
                    TextView hourlyWindDirText = (TextView) view.findViewById(R.id.hourly_wind_dir_text);
                    TextView hourlyWindSpdText = (TextView) view.findViewById(R.id.hourly_wind_sc_text);
                    hourlyText.setText(hourlyForecast.getDate().split(" ")[1]);
                    hourlyCondText.setText(hourlyForecast.getCond().getTxt());
                    hourlyTmpText.setText(hourlyForecast.getTmp() + "°C");
                    hourlyWindDirText.setText(hourlyForecast.getWind().getDir());
                    hourlyWindSpdText.setText(hourlyForecast.getWind().getSc());
                    hourlyForecastLayout.addView(view);
                }
            } else {
                hourlyForecastLayout.setVisibility(View.INVISIBLE);
            }
            if (weather.getAqi() != null) {
                aqiText.setText(weather.getAqi().getCity().getAqi());
                pm25Text.setText(weather.getAqi().getCity().getPm25());
            }
            String comfort = "舒适度：" + weather.getSuggestion().getComf().getTxt();
            String carWash = "洗车指数：" + weather.getSuggestion().getCw().getTxt();
            String sport = "运动建议：" + weather.getSuggestion().getSport().getTxt();
            comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);
            Intent i=new Intent(this, AutoUpdateService.class);
            startService(i);
        }else{
            Toast.makeText(WeatherActivity.this,"获取天气信息扑街",Toast.LENGTH_SHORT).show();
        }
    }

    //load pic加载背景图
    private void loadBingPic(){
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
}
