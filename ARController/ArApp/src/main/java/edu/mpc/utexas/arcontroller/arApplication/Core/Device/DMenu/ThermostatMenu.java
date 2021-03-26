package edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu;

import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Device;
import edu.mpc.utexas.arcontroller.arApplication.R;

public class ThermostatMenu extends DeviceMenu{
    private static final String TAG = "ThermostatMenu";
    private static final int DEFAULT_OPT = 0;

    private WeakReference<Device> mDevice;
    private NumberPicker setTempPicker;
    private boolean isLightOn;

    private String displayTitle = "Thermostat";

    private int maxTemp = 85, minTemp=45;
    private int currTemp = 70, currSet = 70;

    public int getOption() {
        return option;
    }

    private int option;

    public ThermostatMenu(Device device) {
        mDevice = new WeakReference<>(device);
        isLightOn = false;
        option = DEFAULT_OPT;
    }

    public ThermostatMenu(Device device, int option) {
        mDevice = new WeakReference<>(device);
        isLightOn = false;
        this.option = option;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public int getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(int maxTemp) {
        this.maxTemp = maxTemp;
    }

    public int getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(int minTemp) {
        this.minTemp = minTemp;
    }

    public int getCurrTemp() {
        return currTemp;
    }

    public void setCurrTemp(int currTemp) {
        this.currTemp = currTemp;
    }

    private void speakerMenu(Dialog dialog) {
        dialog.setContentView(R.layout.thermostat_dialog);
        TextView curr = dialog.findViewById(R.id.curTempText);
        curr.setText(currTemp + " °F");

        setTempPicker = dialog.findViewById(R.id.setTempPicker);
        setTempPicker.setMaxValue(maxTemp);
        setTempPicker.setMinValue(minTemp);
        setTempPicker.setValue(currSet);

        Button confirmBtn = dialog.findViewById(R.id.confirmBtn);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmed(v);
            }
        });

    }

    @Override
    public void populateMenu(Dialog dialog) {
        dialog.setTitle(displayTitle);
        speakerMenu(dialog);

    }


    private void confirmed(View v) {
        Log.d(TAG, "Thermostat confirmed");
        mDevice.get().userInteracted();
        currSet = setTempPicker.getValue();
    }
}
