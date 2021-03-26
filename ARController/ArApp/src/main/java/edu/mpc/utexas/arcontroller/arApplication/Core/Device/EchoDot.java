package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;

import java.lang.ref.WeakReference;

import androidx.fragment.app.DialogFragment;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.SpeakerMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.LocationServiceManager;

public class EchoDot extends Device{


    private static final String TAG = "EchoDot";
    public static final String deviceType = "echo";

    private String uuid;
    private boolean isOn;
    private boolean hasInteracted;

    private SpeakerMenu mMenu;
    private Dialog savedDialog;

    private WeakReference<LocationServiceManager> mLocManager;

    public EchoDot(String addr, LocationServiceManager locManager) {
        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        isOn = false;
        hasInteracted = false;
        mMenu = new SpeakerMenu(this);
        mMenu.setDisplayTitle("Echo Dot Controller");
    }

    public EchoDot(String addr, LocationServiceManager locManager, int option) {

        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        isOn = false;
        hasInteracted = false;

        mMenu = new SpeakerMenu(this, option);
        mMenu.setDisplayTitle("Echo Dot Controller");
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

    @Override
    public void getFeedback(boolean isPositive) {
        hasInteracted = false;
    }

    @Override
    public String getName() {
        return  "Echo_dot: " + uuid;
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
