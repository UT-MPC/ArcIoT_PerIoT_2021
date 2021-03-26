package edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.Task.HueNetworkTask;

import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.HUE_BRIDGE_URL;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.HUE_USER_TOKEN;

public class HueBridgeManager {
    private static final String TAG = "HueBridgeManager";

    private String userToken = HUE_USER_TOKEN;
    private HueBridgeHttpService mConnectionService;
    private List<HueLight> lights;

    public HueBridgeManager() {
        lights = new ArrayList<>();
        mConnectionService = new HueBridgeHttpService(HUE_BRIDGE_URL);
        initialization();
    }

    private void initialization() {
        if (userToken == null) {
//            userToken = mConnectionService.createUser(HUE_USER_NAME);
            Log.e(TAG, "No user toekn set!!");
        }

        HueNetworkTask.TaskBody initTask = new HueNetworkTask.TaskBody() {
            @Override
            public Object run() {
                return mConnectionService.getLights(userToken);
            }

            @Override
            public void onComplete(Object result) {
                if (result instanceof List) {
                    reportLights((List<HueLight>) result);
                }
            }
        };

        new HueNetworkTask(initTask).execute();
    }

    private void reportLights(List<HueLight> lights) {
        if (lights != null) {
            this.lights = lights;
        }
//        for (HueLight light:lights) {
//            Log.d("???", light.getName() + " " + light.getUuid());
//        }
    }

    public HueLightAccessProxy getLight(String snId) {
        for (HueLight light:lights) {
            if (light.getName().contains(snId)) {
                return new HueLightAccessProxy(light, userToken, mConnectionService);
            }
        }
        return null;
    }

}
