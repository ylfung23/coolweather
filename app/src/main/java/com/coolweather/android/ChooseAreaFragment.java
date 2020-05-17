package com.coolweather.android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;



import static android.app.AlertDialog.*;

/**
 * Created by Administrator on 17/5/2020.
 */

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY =2;
    private AlertDialog alertDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    // Province Table
    private List<Province> provinceList;

    // City Table
    private List<City> cityList;

    // County Table
    private List<County> countyList;

    // selected Province
    private Province selectedProvince;

    // selected City
    private City selectedCity;

    // current selected level
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    } // end of onCreateView()

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    } // end of onActivityCreated()

        // Check country province, city and county
        private void queryProvinces(){
            titleText.setText("China");
            backButton.setVisibility(View.GONE);
            provinceList = DataSupport.findAll(Province.class);
            if (provinceList.size() > 0){
                dataList.clear();
                for (Province province : provinceList){
                    dataList.add(province.getProvinceName());
                }
                adapter.notifyDataSetChanged();
                listView.setSelection(0);
                currentLevel = LEVEL_PROVINCE;
            } else {
                String address = "http://guolin.tech/api/china";
                queryFromServer(address, "province");
            }
        } // end of queryProvinces()

    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0){
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    } // end of queryCities()

    // Check selected city within all county
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0){
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
            } else {
                int provinceCode = selectedProvince.getProvinceCode();
                int cityCode = selectedCity.getCityCode();
                String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
                queryFromServer(address, "county");
            }
    } // end of queryCounties()

    // Due to input address and type from server to check for province,city and county
    private void queryFromServer(String address, final String type) {
        //showAlertDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {

           @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            alertDialog.dismiss();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            } // end of onResponse()

           // @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        alertDialog.dismiss();
                        Toast.makeText(getContext(), "Fault", Toast.LENGTH_SHORT).show();
                    }
                });
            } // end of onFailure()
        });  // end of sendOkHttpRequest()
      } // end of queryFromServer()

    private void showAlertDialog(){
        if (alertDialog == null){
            alertDialog.show();
            alertDialog.setMessage("Loading...");
            alertDialog.setCanceledOnTouchOutside(false);
        }
        alertDialog.show();
    }

    private void closeAlertDialog(){
        if (alertDialog != null){
            alertDialog.dismiss();
        }
    }


} // end of class ChooseAreaFragment
