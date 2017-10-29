package org.openstationmap.openstationmap.helper;

import java.io.File;
import java.util.ArrayList;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.data.geojson.GeoJsonLayer;

import org.openstationmap.openstationmap.MainActivity;
import org.openstationmap.openstationmap.R;

/**
 * Created by sebastian on 10/4/17.
 */

public class SeeStations extends DialogFragment {

    MainActivity main;
    String fragmentStat;
    ArrayList<String> listItems;
    ArrayAdapter<String> adapter;
    File[] dirFiles = Environment.getExternalStorageDirectory().listFiles();
    String fileExtension;
    String folderName;
    String folderNameCache;
    GoogleMap mMAp;
    GeoJsonLayer layer;

    public void setArguments(MainActivity maini , String fragmentStati, String fileExtensioni, String folderNamei, GoogleMap mMapi, GeoJsonLayer layeri, String folderNameCachei) {
        main = maini;
        fragmentStat = fragmentStati;
        fileExtension = fileExtensioni;
        folderName =  folderNamei;
        mMAp = mMapi;
        layer = layeri;
        folderNameCache = folderNameCachei;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("Log ", String.valueOf("okay"));
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View inflator = inflater.inflate(R.layout.seestations, null);
        final ListView listview = (ListView) inflator.findViewById(R.id.liste);
        listItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, listItems);
        getFileNames(listview);

        listview.setOnItemClickListener(clickShort);
        listview.setOnItemLongClickListener(clickLong);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(inflator);

        return builder.create();
    }


    public void getFileNames (ListView listview) {

        String file;
        adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listItems);

        if(dirFiles != null){
            for(int j = 0; j<dirFiles.length;j++){
                if(dirFiles[j].getName().equals(folderName)){
                    for (int i = 0; i< dirFiles[j].list().length; i++){
                        file = dirFiles[j].list()[i].trim();
                        // get only name of station not the full extension
                        if(!file.equals(folderNameCache)){
                            file = dirFiles[j].list()[i].substring(0,dirFiles[j].list()[i].lastIndexOf("."));
                            adapter.add(file);
                        }
                    }
                }
            }
        }
        listview.setAdapter(adapter);
    }

    public AdapterView.OnItemClickListener clickShort = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String name = (String)adapter.getItem(position);

            new LoadStation(main, mMAp, fileExtension, folderName, layer).execute(name);

            Fragment frg = main.getFragmentManager().findFragmentByTag(fragmentStat);
            final FragmentTransaction ft = main.getFragmentManager().beginTransaction();
            ft.detach(frg);
            ft.attach(frg);
            ft.commit();
        }
    };

    public AdapterView.OnItemLongClickListener clickLong = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            File deleteFile;
            boolean deleted = false;
            String name = (String)adapter.getItem(position);
            String filename = name + fileExtension;

            for(int j = 0; j<dirFiles.length;j++){
                if(dirFiles[j].getName().equals(folderName)){
                    for (int i = 0; i< dirFiles[j].list().length; i++){
                        if(dirFiles[j].list()[i].equals(filename)){
                            deleteFile = new File(dirFiles[j].getAbsolutePath()+ "/" + filename);

                            deleted = deleteFile.delete();
                        }
                    }
                }
            }

            if(deleted){
                Toast.makeText(main, "Station " + name + " is deleted", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(main, "Station " + name + " is not deleted", Toast.LENGTH_LONG).show();
            }
            adapter.notifyDataSetChanged();

            Fragment frg = main.getFragmentManager().findFragmentByTag(fragmentStat);
            final FragmentTransaction ft = main.getFragmentManager().beginTransaction();
            ft.detach(frg);
            ft.attach(frg);
            ft.commit();
            // return true for deactivating the short click listener
            return true;
        }
    };
}
