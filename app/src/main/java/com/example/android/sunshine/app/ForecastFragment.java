package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by ahmed on 19-Jul-16.
 */
public  class ForecastFragment extends Fragment {

    ArrayAdapter<String> mForecastAdapter;


    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh){
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("11311");

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forecastArray = {"aaa","sss"};

        ArrayList<String> weekForecast = new ArrayList<>(Arrays.asList(forecastArray));

        mForecastAdapter =
                new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);


        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);


        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>  {

            @Override
            protected String[] doInBackground(String... params) {

                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                // Will contain the raw JSON response as a string.
                String forecastJsonStr = null;

                try {
                    // Construct the URL for the OpenWeatherMap query
                    // Possible parameters are available at OWM's forecast API page, at
                    // http://openweathermap.org/API#forecast
                    int numDays =7;
                    String units = "metric";
                    String format = "JSON";

                    Uri.Builder builder = new Uri.Builder();
                    builder.scheme("http");
                    builder.authority("api.openweathermap.org");
                    builder.path("data/2.5/forecast/daily");
                    builder.appendQueryParameter("q", params[0]);
                    builder.appendQueryParameter("cnt", Integer.toString(numDays));
                    builder.appendQueryParameter("units", units);
                    builder.appendQueryParameter("mode", format);
                    builder.appendQueryParameter("appid", "bea05a71cdae2ce48f40b8cf69388811");

                    //logging the url
                    Log.d("ForecastFragment", "url: " + builder.toString());

                    URL url = new URL(builder.toString());

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        forecastJsonStr = null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        forecastJsonStr = null;
                    }
                    forecastJsonStr = buffer.toString();
                    Log.v("ForecastFragment", forecastJsonStr);
                } catch (IOException e) {
                    Log.e("PlaceholderFragment", "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attempting
                    // to parse it.
                    forecastJsonStr = null;
                } finally{
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e("PlaceholderFragment", "Error closing stream", e);
                        }
                    }
                }

                try {

                    String[] forecastString = getWeatherDataFromJson(forecastJsonStr,7);
                    for (int i = 0; i < forecastString.length;i++){
                        Log.d("Forecast", forecastString[i]);
                        return forecastString;
                    }
                } catch(JSONException e){
                    Log.e("forecasFragment","error", e);

                };

                return null;

            }

        @Override
        protected void onPostExecute(String[] strings) {
            mForecastAdapter.clear();
            for (String s : strings){
                mForecastAdapter.add(s);
                //for honeycomb and later versions can use Adapter.addAll(Collection);
            }



        }
    };

    public  String[] getWeatherDataFromJson(String weatherJsonStr, int numDays)
            throws JSONException {

        Time dayTime = new Time();
        dayTime.setToNow();

        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        String[] dayForecast = new String[numDays];

        JSONObject forecastJson = new JSONObject(weatherJsonStr);

        JSONArray days = forecastJson.getJSONArray("list");

        for(int y = 0;y < dayForecast.length ;y++ ) {


            JSONObject dayInfo = days.getJSONObject(y);
            JSONArray weatherJson = dayInfo.getJSONArray("weather");
            JSONObject mainWeather = weatherJson.getJSONObject(0);
            String main = mainWeather.getString("main");
            JSONObject jsonTemp = dayInfo.getJSONObject("temp");
            double maxTemp = jsonTemp.getDouble("max");
            double minTemp = jsonTemp.getDouble("min");
            String temps = minMaxRounded(minTemp, maxTemp);

            long d = dayTime.setJulianDay(julianStartDay+y);

            dayForecast[y]=getReadableDateString(d)+" - "+ main +" "+ temps;


        }

        return dayForecast;
    }

    public  String minMaxRounded(double min, double max){
        String minimum = Long.toString(Math.round(min));
        String maximum = Long.toString(Math.round(max));
        return minimum + "/" + maximum;
    }

    public String getReadableDateString(long date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd");
        return dateFormat.format(date);
    }




}
