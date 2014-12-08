package me.dewani.doordarshan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.widget.EditText;
import android.widget.LinearLayout;

import static android.widget.LinearLayout.*;

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

        final EditText input = new EditText(this);
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(50, 50, 50, 50);
        layout.setLayoutParams(llp);
        input.setLayoutParams(llp);
        input.setMinimumWidth(100);
        layout.addView(input);
        layout.setOrientation(LinearLayout.VERTICAL);


        // Set an EditText view to get user input
        new AlertDialog.Builder(this)
                .setTitle("API Token")
                .setMessage("Leave blank if you don't know")
                .setView(layout)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Editable token = input.getText();
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
                                        EpgHelper.insertChannels(mContentResolver, getApplicationContext(), info, token.toString());
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
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
    }
}
