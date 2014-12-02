package com.example.android.tv.doordarshan;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;

import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;

/**
 * Created by anirudhd on 11/24/14.
 */
public class SetupActivity extends Activity {


    private ContentResolver mContentResolver;
    private TvInputManager mManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());

        new AsyncTask<ContentResolver, Void, Boolean>() {

            public ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = ProgressDialog.show(SetupActivity.this,
                        "Setting up channels", "Fetching EPG data from network");
            }

            @Override
            protected Boolean doInBackground(ContentResolver... params) {
                mContentResolver = getContentResolver();
                mManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
                for (TvInputInfo info : mManager.getTvInputList()) {
                    if (info.getServiceInfo().name.equals(DoordarshanService.class.getName())) {
                        EpgHelper.deleteChannels(mContentResolver, info);
                        EpgHelper.insertChannels(mContentResolver, info);
                        break;
                    }
                }
                return Boolean.TRUE;
            }

            @Override
            public void onPostExecute(Boolean result) {
                progressDialog.dismiss();
                startService(new Intent(SetupActivity.this, DoordarshanService.class));
                setResult(RESULT_OK);
                finish();
            }
        }.execute();


    }
}
