package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.Button;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.DialogFragment;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.HueLightAccessProxy;
import edu.mpc.utexas.arcontroller.arApplication.Core.LocationServiceManager;
import edu.mpc.utexas.arcontroller.arApplication.R;

public class HueThingyConnector extends Device{

    public static final Map<String, String> idMap = new HashMap<String, String>(){{
//        put("C0:6A:20:CC:85:9F","0F1751");
//        put("D9:01:D4:2D:E2:AA","815F9E");
    }};

    Thingy locThingy;
    HueLightAccessProxy hueLight;
    boolean hasInteracted;
    private WeakReference<LocationServiceManager> mLocManager;
    private Dialog savedDialog;


    public HueThingyConnector(Thingy thingy, HueLightAccessProxy light, LocationServiceManager manager) {
        locThingy = thingy;
        hueLight = light;
        hueLight.registerConnector(this);
        mLocManager = new WeakReference<>(manager);
    }

    @Override
    public DialogFragment showController(Activity act, int x, int y) {
        final Dialog controlDialog = new Dialog(act);
        controlDialog.setCancelable(true);
        controlDialog.setCanceledOnTouchOutside(true);
        hueLight.populateMenu(controlDialog);

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
    public String getUuid() {
        return locThingy.getUuid() + "##"  + hueLight.getUuid();
    }

    @Override
    public void getFeedback(boolean isPositive) {
        if (hasInteracted) {
            mLocManager.get().sendFeedback(locThingy.getUuid(), isPositive);
        }
        hasInteracted = false;
    }

    @Override
    public String getName() {
        return "Connector: " + locThingy.getName() + " and " + hueLight.getName();
    }

    @Override
    public int getOption() {
        return locThingy.getOption();
    }

    @Override
    public void userInteracted() {
        hasInteracted = true;
        mLocManager.get().userInteracted(this);
        savedDialog.cancel();

    }
}
