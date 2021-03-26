package edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu;

import android.app.Dialog;

import java.lang.ref.WeakReference;

import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Device;

public abstract class DeviceMenu {
    public interface MenuHandler {
        void handle(int evtId, Object data);
    }
    public abstract void populateMenu(Dialog dialog);

    public abstract int getOption();
}
