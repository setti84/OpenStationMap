package org.openstationmap.openstationmap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.geojson.GeoJsonLayer;

import org.openstationmap.openstationmap.helper.LoadStation;
import org.openstationmap.openstationmap.helper.SaveStation;
import org.openstationmap.openstationmap.helper.SeeStations;
import org.openstationmap.openstationmap.objects.Station;



public class MainActivity extends FragmentActivity
implements OnMapReadyCallback {

    private GoogleMap mMap;
    GeoJsonLayer layer;
    Station stat;
    String fragmentStat = "stationNames";
    String fileExtension = ".json";
    String folderNameCache = "cache";
    String folderName = "stationdata";
    String cacheFileName = "downloadCache";
    int oldButtonSelected = 0;
    long downloadID;
    View view;
    int selectedLevel = 0;
    Marker whatsAppMark;

    public void setLayer(GeoJsonLayer layer) {
        this.layer = layer;
    }

    public void setStation(Station stat) {
        this.stat = stat;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = findViewById(android.R.id.content);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final Button button = (Button) findViewById(R.id.downloadData);
        button.setOnClickListener(buttonClickListener);

        final Button button2 = (Button) findViewById(R.id.getStations);
        button2.setOnClickListener(buttonClickListener);

        final Button button3 = (Button) findViewById(R.id.saveData);
        button3.setOnClickListener(buttonClickListener);

        final Switch switch2 = (Switch) findViewById(R.id.switch2);
        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (stat != null) {
                    if (isChecked) {
                        LoadStation.setStyleSncf(stat.getData());

                    } else {
                        LoadStation.setStyleBahn(stat.getData());
                    }

                } else {
                    Toast.makeText(MainActivity.this, "load Station first", Toast.LENGTH_LONG).show();
                }


            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        setMapOptions(mMap);

        IntentFilter downloadCompleteIntentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver downloadCompleteReceiver = broadDown();
        this.registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);

        getWhatsAppIntent();
    }

    private void setMapOptions(final GoogleMap mMap) {

        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.setTrafficEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // disable buisness points
        // https://developers.google.com/maps/documentation/android-api/hiding-features
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));

        LatLng berlin = new LatLng(52.51022, 13.43477);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(berlin, 18.0f));

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {

                if(mMap.getCameraPosition().zoom < 16 && layer != null && layer.isLayerOnMap()){
                    layer.removeLayerFromMap();
                    ListView listview = (ListView) findViewById(R.id.levelListe);
                    listview.setVisibility(View.INVISIBLE);
                }
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                whatsAppMark = mMap.addMarker(new MarkerOptions().position(latLng));
                onClickWhatsApp(latLng);
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(whatsAppMark != null){
                    whatsAppMark.remove();
                }

            }
        });
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.downloadData:
                    if (mMap.getCameraPosition().zoom > 16){

                        LatLngBounds coord = mMap.getProjection().getVisibleRegion().latLngBounds;
                        downloadData(coord);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Please zoom in for download", Toast.LENGTH_LONG).show();
                    }
                    break;

                case R.id.getStations:
                    SeeStations listOfStations = new SeeStations();
                    listOfStations.setArguments(MainActivity.this, fragmentStat, fileExtension, folderName, mMap, layer, folderNameCache);
                    listOfStations.show(getFragmentManager(),fragmentStat);

                    break;

                case R.id.saveData:
                    SaveStation namefield = new SaveStation();
                    namefield.setArguments(folderName, folderNameCache, fileExtension, cacheFileName);
                    namefield.show(getFragmentManager(),"save");
                    break;
            }
        }
    };

    public BroadcastReceiver broadDown (){

        BroadcastReceiver broad = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v("Log2", String.valueOf("download triggered3.5"));
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                if (id != downloadID) {
                    Log.v("Main", "Ingnoring unrelated download " + id);
                    return;
                }

                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor cursor = downloadManager.query(query);

                if (!cursor.moveToFirst()) {
                    Log.e("Main", "Empty row");
                    return;
                }

                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
                    Log.e("Main", "Download Failed");
                    Toast.makeText(MainActivity.this, "Download Failed", Toast.LENGTH_LONG).show();
                    return;
                }

                int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                String pathDownFile = cursor.getString(uriIndex);
                String name = pathDownFile.substring(pathDownFile.lastIndexOf("/")+1, pathDownFile.lastIndexOf("."));
                Log.v("Log2", String.valueOf("download triggered4"));
                new LoadStation(MainActivity.this, mMap, fileExtension, folderName + "/" + folderNameCache, layer).execute(name);
            }
        };
        return broad;
    }

    public void LevelList(final Station stat){

        ArrayList level = new ArrayList<>(stat.getData().keySet());

        Collections.sort(level);
        Collections.reverse(level);
        Log.e("Zoom1", String.valueOf(level));

        final ListView listview = (ListView) findViewById(R.id.levelListe);
        listview.setBackgroundColor(Color.WHITE);

        final ArrayAdapter adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, level) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                text.setGravity(Gravity.CENTER);
                return view;
            }
        };

        listview.setAdapter(adapter);
        listview.setVisibility(View.VISIBLE);
        listview.bringToFront();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView textOld = (TextView) parent.getChildAt(oldButtonSelected);
                textOld.setBackgroundColor(Color.WHITE);
                oldButtonSelected = position;

                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setBackgroundColor(Color.GRAY);
                selectedLevel = Integer.parseInt(String.valueOf(text.getText()));
                layer.removeLayerFromMap();
                layer = (GeoJsonLayer) stat.getData().get(Integer.parseInt(String.valueOf(text.getText())));
                layer.addLayerToMap();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v("me","Permission: "+permissions[0]+ " was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    public boolean isStoragePermissionGranted() {

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission( android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("me","Permission is granted");
                return true;
            } else {
                Log.v("me","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else {
            Log.v("me","Permission is granted");
            return true;
        }
    }

    public void downloadData(LatLngBounds coords){

        Log.v("Log2", String.valueOf("download triggered2"));

        // delete all old cached files
        File dir =  new File(Environment.getExternalStorageDirectory()+ "/" + folderName + "/" + folderNameCache);
        if(dir.listFiles() != null){
            for(File file: dir.listFiles()){
                if(!file.isDirectory()){
                    Log.e("deleted", String.valueOf(file));
                    file.delete();
                }
            }
        }


        String url = "http://85.214.126.95:8000/geoData/"
                + coords.southwest.longitude + "/"
                + coords.southwest.latitude + "/"
                + coords.northeast.longitude + "/"
                + coords.northeast.latitude;

        if(isStoragePermissionGranted()){

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }

            // set user notification, in this case nothing shows up
            //request.setNotificationVisibility(request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
            request.setTitle(String.valueOf(R.string.titleDownload));
            request.setDestinationInExternalPublicDir(folderName + "/" + folderNameCache, cacheFileName + fileExtension);
            DownloadManager manager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadID =  manager.enqueue(request);
            Log.v("Log2", String.valueOf("download triggered3"));
        }
    }

    private void getWhatsAppIntent() {

        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
        // handle incomming String
        if(appLinkData != null){
            Log.e("Log2", String.valueOf("bin in if"));
            String[] url = String.valueOf(appLinkData).split("/");
            for(String part : url){
                Log.e("Log2", String.valueOf(part));
            }
            LatLng position = new LatLng(Double.parseDouble(url[4]), Double.parseDouble(url[5]));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 18.0f));
            // download data and visualise
            whatsAppMark = mMap.addMarker(new MarkerOptions().position(position).title("Level: " + url[8]).snippet("Meet me here!"));
        }

    }

    public void onClickWhatsApp(LatLng latLng) {

        PackageManager pm=getPackageManager();
        try {

            Intent waIntent = new Intent(Intent.ACTION_SEND);
            waIntent.setType("text/plain");

            String text = "https://openstationmap.org/#18/" + String.valueOf(latLng.latitude).substring(0,9)
                    + "/" + String.valueOf(latLng.longitude).substring(0,9) + "/0/0/" + String.valueOf(selectedLevel);


            PackageInfo info = pm.getPackageInfo("com.whatsapp", PackageManager.GET_META_DATA);
            waIntent.setPackage("com.whatsapp");

            waIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(waIntent, "Share with"));

        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "WhatsApp not Installed", Toast.LENGTH_SHORT).show();
        }

    }
}

