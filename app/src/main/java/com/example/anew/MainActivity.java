package com.example.anew;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvLocation;
    private AutoCompleteTextView etSearch;
    private Button btnGetLocation, btnSearch;
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final String GAODE_KEY = "959f633b6e5d69dbed787c6bc7401372"; // 你的 Web Key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tv_location);
        etSearch = findViewById(R.id.et_search);
        btnGetLocation = findViewById(R.id.btn_get_location);
        btnSearch = findViewById(R.id.btn_search);

        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
        btnSearch.setOnClickListener(v -> searchLocation());

        // 请求定位权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }

        // **输入框监听，自动补全地点**
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 1) {  // 只在输入 2 个字符以上时搜索
                    fetchAutoCompleteSuggestions(s.toString());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // **获取当前位置**
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "未授予定位权限", Toast.LENGTH_SHORT).show();
            return;
        }

        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            tvLocation.setText("当前位置：\n经度: " + longitude + "\n纬度: " + latitude);
            getCityFromLocation(latitude, longitude);
        } else {
            Toast.makeText(this, "无法获取当前位置", Toast.LENGTH_SHORT).show();
        }
    }

    // **🔹新增方法：根据经纬度获取城市名称**
    private void getCityFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String cityName = addresses.get(0).getLocality();
                runOnUiThread(() -> tvLocation.append("\n城市：" + cityName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // **查询地点信息（输入地名，返回经纬度）**
    private void searchLocation() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "请输入城市名", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String urlStr = "https://restapi.amap.com/v3/geocode/geo?address=" + query + "&key=" + GAODE_KEY;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray geocodes = jsonResponse.optJSONArray("geocodes");

                if (geocodes != null && geocodes.length() > 0) {
                    JSONObject locationData = geocodes.getJSONObject(0);
                    String location = locationData.getString("location"); // 经纬度
                    runOnUiThread(() -> tvLocation.setText("地点：" + query + "\n经纬度：" + location));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "未找到地点", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "查询失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // **获取自动补全地点**
    private void fetchAutoCompleteSuggestions(String input) {
        new Thread(() -> {
            try {
                String urlStr = "https://restapi.amap.com/v3/assistant/inputtips?keywords=" + input + "&key=" + GAODE_KEY;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray tipsArray = jsonResponse.optJSONArray("tips");

                if (tipsArray != null) {
                    List<String> suggestions = new ArrayList<>();
                    for (int i = 0; i < tipsArray.length(); i++) {
                        JSONObject tip = tipsArray.getJSONObject(i);
                        String name = tip.optString("name");
                        if (!TextUtils.isEmpty(name)) {
                            suggestions.add(name);
                        }
                    }
                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions);
                        etSearch.setAdapter(adapter);
                        etSearch.showDropDown();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
