package org.openstationmap.openstationmap.helper;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.openstationmap.openstationmap.R;

/**
 * Created by sebastian on 10/14/17.
 */

public class SaveStation extends DialogFragment {

    EditText edit;
    String folderName;
    String folderNameCache;
    String fileExtension;
    String cacheFileName;

    public void  setArguments(String folderNamei, String folderNameCachei, String fileExtensioni, String cacheFileNamei) {
        folderName = folderNamei;
        folderNameCache = folderNameCachei;
        fileExtension = fileExtensioni;
        cacheFileName = cacheFileNamei;
    }

    public SaveStation() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View inflator = inflater.inflate(R.layout.savestation, null);

        builder.setView(inflator).setPositiveButton(R.string.buttonsavestation, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                edit = (EditText)inflator.findViewById(R.id.stationName);
                //Log.v("Log",edit.getText().toString());
                String newFileName = edit.getText().toString().trim();

                String inputhPath = Environment.getExternalStorageDirectory()+ "/" + folderName + "/" + folderNameCache + "/";
                String inputFile = cacheFileName + fileExtension;
                String outputPath = Environment.getExternalStorageDirectory()+ "/" + folderName + "/";
                moveFile(inputhPath,inputFile,outputPath, newFileName, fileExtension);
                Toast.makeText( SaveStation.this.getActivity(), "Saved: " + newFileName, Toast.LENGTH_LONG).show();

            }
        }).setNegativeButton(R.string.buttondontsavestation, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //LoginDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

    private void moveFile(String inputPath, String inputFile, String outputPath, String newFileName, String fileExtension) {

        InputStream in = null;
        OutputStream out = null;
        try {

            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + newFileName+fileExtension);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            out.flush();
            out.close();
            out = null;

            new File(inputPath + inputFile).delete();
        }

        catch (FileNotFoundException fnfe1) {
            Log.e("Log", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("Log", e.getMessage());
        }
    }
}
