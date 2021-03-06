package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

import android.app.Activity;
import android.app.Dialog;

import java.lang.ref.WeakReference;

import androidx.fragment.app.DialogFragment;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.DeviceMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.HueLightMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.ThermostatMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.HueLight;
import edu.mpc.utexas.arcontroller.arApplication.Core.LocationServiceManager;

public class PhilipsLightbulb extends Device{
    private static final String TAG = "PhilipsLightbulb";
    public static final String deviceType = "philipsLightbulb";


    private String uuid;
    private boolean hasInteracted;

    private HueLightMenu mMenu;
    private Dialog savedDialog;
    private HueLight mLight;

    private WeakReference<LocationServiceManager> mLocManager;

    public PhilipsLightbulb(String addr, LocationServiceManager locManager) {
        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        hasInteracted = false;
        mMenu = new HueLightMenu(mLight, new DeviceMenu.MenuHandler() {
            @Override
            public void handle(int evtId, Object data) {

            }
        });
        mMenu.setDisplayTitle("Lightbulb Controller");
    }

    public PhilipsLightbulb(String addr, LocationServiceManager locManager, int option) {

        this.uuid = addr;
        mLocManager = new WeakReference<>(locManager);
        hasInteracted = false;
        mMenu = new HueLightMenu(mLight, new DeviceMenu.MenuHandler() {
            @Override
            public void handle(int evtId, Object data) {

            }
        });
        mMenu.setDisplayTitle("Lightbulb Controller");
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
