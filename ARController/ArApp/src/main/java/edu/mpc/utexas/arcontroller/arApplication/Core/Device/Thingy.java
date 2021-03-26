package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;

import java.lang.ref.WeakReference;

import androidx.fragment.app.DialogFragment;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.ThingyMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.LocationServiceManager;

public class Thingy extends Device{

    private static final String TAG = "Thingy";
    public static final String deviceType = "thingy";

    public static final String[] addrs = new String[]{
        "C2:84:A9:53:29:BB",
        "C0:97:27:41:8B:3D",
        "C0:6A:20:CC:85:9F",
        "E5:7C:A5:AC:80:8A",
    };

    private static final int BLE_SENDING_DELAY = 1000;

    private String uuid;
    private boolean isOn;
    private boolean isBleSending;
    private Handler bleHandler;
    private boolean hasInteracted;
    private Runnable stopAdvRun = new Runnable() {
        @Override
        public void run() {
            mLocManager.get().stopAdvertising();
            isBleSending = false;
        }
    };

    private ThingyMenu mMenu;
    private Dialog savedDialog;

    private WeakReference<LocationServiceManager> mLocManager;

    public Thingy(String addr, LocationServiceManager locManager) {
        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        isOn = false;
        hasInteracted = false;
        mMenu = new ThingyMenu(this);
    }

    public Thingy(String addr, LocationServiceManager locManager, int option) {

        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        isOn = false;
        hasInteracted = false;

        mMenu = new ThingyMenu(this, option);
    }

    @Override
    public DialogFragment showController(Activity act, int x, int y) {
        final Dialog controlDialog = new Dialog(act);
        controlDialog.setCancelable(true);
        controlDialog.setCanceledOnTouchOutside(true);
        mMenu.populateMenu(controlDialog);

//        Button b = controlDialog.findViewById(R.id.thingy_move);
//        b.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                moveDevice();
//            }
//        });
        showDialog(controlDialog, x, y);
        savedDialog = controlDialog;
        return null;
    }

    private void moveDevice() {

    }

    public void sendBleCmd(byte[] cmd) {
        if (isBleSending) {
            mLocManager.get().stopAdvertising();
            bleHandler.removeCallbacks(stopAdvRun);
        }
        mLocManager.get().startAdvertising(cmd);
        isBleSending = true;
        if (bleHandler == null) {
            bleHandler = new Handler();
        }
        bleHandler.postDelayed(stopAdvRun, BLE_SENDING_DELAY);
    }

    @Override
    public void getFeedback(boolean isPositive) {
        if (hasInteracted) {
            mLocManager.get().sendFeedback(uuid, isPositive);
        }
        hasInteracted = false;
    }

    @Override
    public String getName() {
        return  "Thingy: " + uuid;
    }

    @Override
    public int getOption() {
        return mMenu.getOption();
    }

    @Override
    public String getUuid() {return uuid;}

    @Override
    public void userInteracted() {
        hasInteracted = true;
        mLocManager.get().userInteracted(this);
        savedDialog.cancel();
    }
}
