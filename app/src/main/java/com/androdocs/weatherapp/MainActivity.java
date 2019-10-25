package com.androdocs.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView updated_atTxt, statusTxt, tempTxt, temp_minTxt, temp_maxTxt, sunriseTxt,
            sunsetTxt, windTxt, pressureTxt, humidityTxt, loc;
    AutoCompleteTextView address;
    private LocationManager locationManager;
    private String provider;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updated_atTxt = findViewById(R.id.updated_at);
        statusTxt = findViewById(R.id.status);
        tempTxt = findViewById(R.id.temp);
        temp_minTxt = findViewById(R.id.temp_min);
        temp_maxTxt = findViewById(R.id.temp_max);
        sunriseTxt = findViewById(R.id.sunrise);
        sunsetTxt = findViewById(R.id.sunset);
        windTxt = findViewById(R.id.wind);
        pressureTxt = findViewById(R.id.pressure);
        humidityTxt = findViewById(R.id.humidity);
        loc = findViewById(R.id.loc);
        address = findViewById(R.id.addr);


        //new WeatherTask().execute("Pune");

        String[] cities = getResources().getStringArray(R.array.india_cities);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cities);
        address.setAdapter(adapter);
        address.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selectedCity = address.getText().toString();
                    loc.setText("Location: "+selectedCity);
                    new WeatherTask().execute("city", selectedCity);
                }
            }
        );

        //for loc
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);

        Double lon = location.getLongitude();
        Double lat = location.getLatitude();

        new WeatherTask().execute("coords", Double.toString(lon), Double.toString(lat));
        DetectLocation dl = new DetectLocation();
        dl.setContext(this);
        dl.displayAdd(location);
        loc.setText("Location: "+dl.getAdd());
    }

    class WeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            /* Showing the ProgressBar, Making the main design GONE */
            findViewById(R.id.loader).setVisibility(View.VISIBLE);
            findViewById(R.id.mainContainer).setVisibility(View.GONE);
            findViewById(R.id.errorText).setVisibility(View.GONE);
        }
        @Override
        protected String doInBackground(String... args) {
            String data="";
            WeatherHttpClient whc = new WeatherHttpClient();
            if (args[0] == "city") {
                data = whc.getWeatherData(args[0], args[1]);
            }
            else if (args[0] == "coords")
                data = whc.getWeatherData(args[0], args[1], args[2]);
            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);

            try {

                JSONObject jsonObj = new JSONObject(data);

                JSONObject main = jsonObj.getJSONObject("main");
                JSONObject sys = jsonObj.getJSONObject("sys");
                JSONObject wind = jsonObj.getJSONObject("wind");
                JSONObject weather = jsonObj.getJSONArray("weather").getJSONObject(0);

                Long updatedAt = jsonObj.getLong("dt");

                SimpleDateFormat df = new SimpleDateFormat("dd/mm/yyyy hh:mm a", Locale.ENGLISH);
                String updatedAtText = "Updated at: " + df.format(new Date(updatedAt * 1000));

                String temp = main.getString("temp") + "°C";
                String tempMin = "Min Temp: " + main.getString("temp_min") + "°C";
                String tempMax = "Max Temp: " + main.getString("temp_max") + "°C";
                String pressure = main.getString("pressure");
                String humidity = main.getString("humidity");

                Long sunrise = sys.getLong("sunrise");
                Long sunset = sys.getLong("sunset");
                String windSpeed = wind.getString("speed");
                String weatherDescription = weather.getString("description");

                updated_atTxt.setText(updatedAtText);
                statusTxt.setText(weatherDescription.toUpperCase());
                tempTxt.setText(temp);
                temp_minTxt.setText(tempMin);
                temp_maxTxt.setText(tempMax);
                sunriseTxt.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(sunrise * 1000)));
                sunsetTxt.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(sunset * 1000)));
                windTxt.setText(windSpeed);
                pressureTxt.setText(pressure);
                humidityTxt.setText(humidity);

                /* Views populated, Hiding the loader, Showing the main design */
                findViewById(R.id.loader).setVisibility(View.GONE);
                findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);

            } catch (JSONException e) {
                findViewById(R.id.loader).setVisibility(View.GONE);
                findViewById(R.id.errorText).setVisibility(View.VISIBLE);
                e.printStackTrace();
            }
        }
    }
}

class WeatherHttpClient {

    private static String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?";
    private static String APPID = "157771a599a39dabe7b641652b7f7de3";
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;


    public String getWeatherData(String... args) {
        String query="";
        if (args[0] == "city")
            query = "q="+args[1];
        else if (args[0] == "coords")
            query = "lon=" + args[1] + "&lat=" + args[2];

        HttpURLConnection con = null ;
        InputStreamReader is = null;

        try {
            URL url = new URL(BASE_URL + query + "&units=metric&appid=" + APPID);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setReadTimeout(READ_TIMEOUT);
            con.setConnectTimeout(CONNECTION_TIMEOUT);
            con.connect();

            // Let's read the response
            is = new InputStreamReader(con.getInputStream());
            BufferedReader reader = new BufferedReader(is);
            StringBuilder strBuilder = new StringBuilder();
            String line = null;

            while ( (line = reader.readLine()) != null )
                strBuilder.append(line);

            //is.close();
            //con.disconnect();
            return strBuilder.toString();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                is.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
            try {
                con.disconnect();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}

class DetectLocation extends AppCompatActivity implements LocationListener {
    //for loc
    private LocationManager locationManager;
    private String provider;
    String addr = "";
    Context c;
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void displayAdd(Location location) {
        //for loc
        if (location != null) {
            System.out.println("Provider " + provider + " has been selected.");

            onLocationChanged(location);
        } else {

            //latituteField.setText("Location not available");
            //longitudeField.setText("Location not available");
        }
    }

    //for loc
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    void setContext(Context c) {
        this.c = c;
    }

    void setAdd(String a) {
        this.addr = a;
    }

    String getAdd() {
        return this.addr;
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = (double) (location.getLatitude());
        double lon = (double) (location.getLongitude());

        Geocoder geocoder = new Geocoder(c, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lon, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String cityName = addresses.get(0).getAddressLine(0);
        //tv3.setText("loc changed");
//        TextView tv3 = findViewById(R.id.tv3);
        //tv3.setText("LAT: "+String.valueOf(lat)+" LON: "+String.valueOf(lng));
//        tv3.setText("reached here");
        //latituteField.setText(String.valueOf(lat));
        //longitudeField.setText(String.valueOf(lng));
        setAdd(cityName);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }
}