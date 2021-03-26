package edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu;


import android.app.Dialog;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Device;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.HueLight;
import edu.mpc.utexas.arcontroller.arApplication.R;

public class HueLightMenu extends DeviceMenu {

    private HueLight light;
    private MenuHandler handler;

    public static final int EVT_TOGGLE_LIGHT = 0;
    private String displayTitle = "Light Controller";
    private int option;

    public HueLightMenu(HueLight light, MenuHandler handler) {
        this.light = light;
        this.handler = handler;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    @Override
    public void populateMenu(Dialog dialog) {
        dialog.setTitle(displayTitle);
        dialog.setContentView(R.layout.light_control_dialog);
        Switch s = dialog.findViewById(R.id.light_toggle_sw);
        if (light != null) s.setChecked(light.getState().isOn());
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("???", "??? Click to turn on " + System.currentTimeMillis());
                handler.handle(EVT_TOGGLE_LIGHT, null);
            }
        });
    }

    public int getOption() {
        return option;
    }


}
