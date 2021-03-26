package edu.mpc.utexas.arcontroller.locationtest;

import edu.mpc.utexas.locationService.UI.DebugActivity;
import edu.mpc.utexas.locationService.utility.RSSI_Helper;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

/*
    This code is written by Jie Hua.

 */
public class TestActivity extends DebugActivity {

    private TextView mStepCountText, mUserLoc, mTargetProb;

    private TextView mDistText;
    private TextView mPosText;

    final String TAG = "TestACC";
    private Switch mServiceSwitch;
    private Switch mScanSwitch;

    private Button mStepSim;
    private Spinner mOpSel;
    private ServiceManager mServiceManager;

    private LinearLayout canvas;
    private int expStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.redirectLog(getBaseContext());
        setContentView(R.layout.activity_test);
        mStepCountText = findViewById(R.id.stepCount);
        mUserLoc = findViewById(R.id.userLoc);
        mTargetProb = findViewById(R.id.targetProb);

        mDistText = findViewById(R.id.BLE_dist);
        mPosText = findViewById(R.id.LDMK_pos);

        mServiceSwitch = findViewById(R.id.serviceSW);
        mServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                serviceSW(isChecked);
            }
        });
        mScanSwitch = findViewById(R.id.scanSW);

        mScanSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanSW(isChecked);
            }
        });
        mStepSim = findViewById(R.id.stepTrigger);
        mStepSim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepSimulate();
            }
        });

        createDropdown();
        mServiceManager = new ServiceManager(this);


        Log.d(TAG, "onCreate");
        checkPermission();
        createDraw();
//        Log.d(TAG, "" + RSSI_Helper.rssiToDistance(-53)
//                + " " + RSSI_Helper.rssiToDistance(-50)
//                + " " + RSSI_Helper.rssiToDistance(-54.5)
//                + " " + RSSI_Helper.rssiToDistance(-60)
//                + " " + RSSI_Helper.rssiToDistance(-65)
//                + " " + RSSI_Helper.rssiToDistance(-70));
        expStep = 0;
    }

    private void createDropdown() {
        mOpSel = findViewById(R.id.OpSel);

        String[] items = new String[]{"Select", "Reset", "Interact", "Feedback NEG", "Feedback POS"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        mOpSel.setAdapter(adapter);
        mOpSel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mOpSel.setSelection(0);
                Object selected = parent.getItemAtPosition(position);
                if (selected instanceof String) {
                    switch ( (String)selected) {
                        case "Reset":
                            resetService();
                            expStep = 0;
                            break;

                        case "One Step":
                            stepSimulate();
                            break;

                        case "Interact":
                            mServiceManager.interactWithDevice();
                            break;

                        case "Feedback NEG":
                            mServiceManager.getFeedback(false);
                            break;

                        case "Feedback POS":
                            mServiceManager.getFeedback(true);
                            break;

                        default:
                            break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });
    }

    private void createDraw() {
        canvas = findViewById(R.id.drawCanvas);
        canvas.addView(mServiceManager.newDrawer(this));
    }

    private void scanSW(boolean isChecked) {
        checkPermission();
        if (isChecked) {
            mServiceManager.startScan();
        } else {
            mServiceManager.stopScan();
        }
    }
    private void serviceSW(boolean isChecked) {
        checkPermission();
        if (isChecked) {
            mServiceManager.startService();
        } else {
            mServiceManager.stopService();
        }
    }

    private void stepSimulate() {
//        mServiceManager.simOneStep();
        Log.d("Exp", "One measure sep");
        expStep += 1;
        mTargetProb.setText(expStep + "");
    }
    private void checkPermission() {
        // Permission check for the pedometer.
        mServiceManager.checkPermission(getBaseContext(), TestActivity.this);
    }

    @Override
    public void updateStep(String text) {
        mStepCountText.setText(text);
    }

    @Override
    public void updateLoc(String text) {
        mUserLoc.setText(text);
    }

    @Override
    public void updatePos(String text) {
        mPosText.setText(text);
    }

    @Override
    public void updateDist(String text) {
        mDistText.setText(text);
    }

    @Override
    public void updateProb(String text) {

//        mTargetProb.setText(text);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void resetService() {
        if (mServiceManager != null) {
            mServiceManager.resetService();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected  void onDestroy() {
        mServiceManager.unBind();
        mServiceManager.stopService();
        super.onDestroy();
    }
}
