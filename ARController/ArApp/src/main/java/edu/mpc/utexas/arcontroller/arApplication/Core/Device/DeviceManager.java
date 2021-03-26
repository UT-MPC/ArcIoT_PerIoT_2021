package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.HueBridgeManager;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.HueLightAccessProxy;
import edu.mpc.utexas.arcontroller.arApplication.Core.LocationServiceManager;

public class DeviceManager {
    private static final String TAG = "DeviceManager";

    private Map<String, Set> devicePool;

    private WeakReference<LocationServiceManager> mLocManager;

    private Map<String, Device> savedDevice;
    private Map<String, Integer> savedOption;

    private HueBridgeManager hueManager;


    public DeviceManager(LocationServiceManager locManager) {
        devicePool = new HashMap<>();
        devicePool.put("Thingy", new HashSet<>(Arrays.asList(Thingy.addrs)));
        devicePool.put("HueThingy", HueThingyConnector.idMap.keySet());
        mLocManager = new WeakReference<>(locManager);
        savedDevice = new HashMap<>();
        hueManager = new HueBridgeManager();
        savedOption = new HashMap<>();

    }

    private Device createByName(String name, String addr, Integer option) {
        if (devicePool.get("HueThingy").contains(addr)) {
            Thingy locThingy = new Thingy(addr, mLocManager.get(), option);
            HueLightAccessProxy light = hueManager.getLight(HueThingyConnector.idMap.get(addr));
            if (light == null) return null;
            return new HueThingyConnector(locThingy, light, mLocManager.get());
        }

        if (name.contains(Thingy.deviceType)) {
            return option ==null ? new Thingy(addr, mLocManager.get()): new Thingy(addr, mLocManager.get(), option);
        }

        if (name.contains(EchoDot.deviceType)) {
            return new EchoDot(addr, mLocManager.get());
        }

        if (name.contains(NestThermo.deviceType)) {
            return new NestThermo(addr, mLocManager.get());
        }

        if (name.contains(PhilipsLightbulb.deviceType)){
            return new PhilipsLightbulb(addr, mLocManager.get());
        }

        return null;
    }

    public Device getDevice(String deviceType, String addr){
        if (savedOption.containsKey(addr)) {
            return getDevice(deviceType, addr, savedOption.get(addr));
        }
        return getDevice(deviceType, addr, null);
    }

    public Device getDevice(String deviceType, String addr, Integer option) {
        Log.d(TAG, "building device " + deviceType + " for addr " + addr);
        if (savedDevice.containsKey(addr)) {
            Log.d(TAG, "Device already existed" );
            return savedDevice.get(addr);
        }
        Device d = createByName(deviceType, addr, option);
        if (d != null) {
            savedDevice.put(addr, d);
        }
        return d;
    }

    public void updateDevice(String addr, Integer option) {
        if (savedDevice.containsKey(addr)) {
            if (savedDevice.get(addr).getOption() != option) {
                savedDevice.remove(addr);
            }
        }
        savedOption.put(addr, option);
    }
}
