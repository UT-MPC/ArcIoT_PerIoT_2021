package edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue;

import android.app.Activity;
import android.app.Dialog;

import java.lang.ref.WeakReference;

import androidx.fragment.app.DialogFragment;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.DeviceMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu.HueLightMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Device;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.Task.HueNetworkTask;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.HueThingyConnector;

public class HueLightAccessProxy extends Device {
    private static final String TAG = "HueLightAccessProxy";


    private HueLightMenu mMenu;
    private HueBridgeHttpService connectionService;
    private String userToken;
    private HueLight mLight;
    private boolean hasInteracted;
    private WeakReference<HueThingyConnector> mConnector;


    public HueLightAccessProxy(HueLight light, String userToken, HueBridgeHttpService service) {
        mLight = light;
        mMenu = new HueLightMenu(mLight, new DeviceMenu.MenuHandler() {
            @Override
            public void handle(int evtId, Object data) {
                handleEvt(evtId, data);
            }
        });
        connectionService = service;
        this.userToken = userToken;
    }

    private void handleEvt(int evtId, Object data) {
        userInteracted();
        if (evtId == HueLightMenu.EVT_TOGGLE_LIGHT) {
            HueLightState lightState = mLight.getState();
            if (lightState == null) lightState = new HueLightState();
            lightState.setOn(!lightState.isOn());

            sendSetStateCommand(lightState);
        }
    }

    public void populateMenu(Dialog dialog) {
        mMenu.populateMenu(dialog);
    }

    public boolean hasInteracted() {
        return hasInteracted;
    }


    public void setHasInteracted(boolean hasInteracted) {
        this.hasInteracted = hasInteracted;
    }

    @Override
    public void userInteracted() {
        if (mConnector != null) {
            mConnector.get().userInteracted();
        }
    }

    @Override
    public DialogFragment showController(Activity act, int x, int y) {
        final Dialog controlDialog = new Dialog(act);
        controlDialog.setCancelable(true);
        controlDialog.setCanceledOnTouchOutside(true);
        mMenu.populateMenu(controlDialog);

        showDialog(controlDialog, x, y);
        return null;
    }

    @Override
    public String getName() {return mLight.getName();}

    @Override
    public String getUuid() {return mLight.getUuid();}

    @Override
    public void getFeedback(boolean isPositive) {}

    private void sendSetStateCommand(final HueLightState lightState) {
        HueNetworkTask.TaskBody cmd = new HueNetworkTask.TaskBody() {
            @Override
            public Object run() {
                connectionService.putThingState(userToken, mLight, lightState);
                return null;
            }

            @Override
            public void onComplete(Object result) {

            }
        };

        new HueNetworkTask(cmd).execute();
    }

    public void registerConnector(HueThingyConnector connector) {
        mConnector = new WeakReference<>(connector);
    }


}
