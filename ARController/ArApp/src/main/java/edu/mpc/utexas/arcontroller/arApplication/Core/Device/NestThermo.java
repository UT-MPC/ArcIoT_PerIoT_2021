package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;

import java.lang.ref.WeakReference;

import androidx.fragment.app.DialogFragment;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.SpeakerMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.ThermostatMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.LocationServiceManager;

public class NestThermo extends Device {

    private static final String TAG = "NestThermo";
    public static final String deviceType = "nest";


    private String uuid;
    private boolean hasInteracted;

    private ThermostatMenu mMenu;
    private Dialog savedDialog;

    private WeakReference<LocationServiceManager> mLocManager;

    public NestThermo(String addr, LocationServiceManager locManager) {
        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        hasInteracted = false;
        mMenu = new ThermostatMenu(this);
        mMenu.setDisplayTitle("Nest Thermostat Controller");
    }

    public NestThermo(String addr, LocationServiceManager locManager, int option) {

        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        hasInteracted = false;

        mMenu = new ThermostatMenu(this, option);
        mMenu.setDisplayTitle("Nest Thermostat Controller");
    }

    @Override
    public DialogFragment showController(Activity act, int x, int y) {
        final Dialog controlDialog = new Dialog(act);
        controlDialog.setCancelable(true);
        controlDialog.setCanceledOnTouchOutside(true);
        mMenu.populateMenu(controlDialog);

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
        return  "Nest_thermo: " + uuid;
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
