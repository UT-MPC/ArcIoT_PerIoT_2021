package edu.mpc.utexas.arcontroller.arApplication.Core.Device.DMenu;

import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Thingy;
import edu.mpc.utexas.arcontroller.arApplication.R;

import static edu.mpc.utexas.arcontroller.arApplication.Core.Device.Utility.macStr2Byte;

public class ThingyMenu extends DeviceMenu {
    private static final String TAG = "ThingyMenu";
    private static final byte THINGY_LIGHT_CMD = 0x00;
    private static final byte THINGY_SOUND_CMD = 0x01;
    private static final byte THINGY_FLASH_CMD = 0x02;


    private static final byte THINGY_ON_CMD_BYTE = 0x00;
    private static final byte THINGY_OFF_CMD_BYTE = 0x01;
    private static final int DEFAULT_OPT = 0;

    private WeakReference<Thingy> mDevice;
    private Spinner speaker_sp;
    private boolean isLightOn;

    public int getOption() {
        return option;
    }

    private int option;

    public ThingyMenu(Thingy device) {
        mDevice = new WeakReference<>(device);
        isLightOn = false;
        option = DEFAULT_OPT;
    }

    public ThingyMenu(Thingy device, int option) {
        mDevice = new WeakReference<>(device);
        isLightOn = false;
        this.option = option;
    }

    private void lightMenu(Dialog dialog) {
        dialog.setContentView(R.layout.light_control_dialog);
        Switch s = dialog.findViewById(R.id.light_toggle_sw);
        s.setChecked(isLightOn);
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleLight();
                //Disabled to prevent accidentally click.
            }
        });

        Button btn = dialog.findViewById(R.id.thingy_flash_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashLight();
            }
        });
    }

    private void speakerMenu(Dialog dialog) {
        dialog.setContentView(R.layout.thingy_speaker_control);
        speaker_sp = dialog.findViewById(R.id.thingy_sound_sel);

        ArrayAdapter<CharSequence> sample_adapter = ArrayAdapter.createFromResource(dialog.getContext(), R.array.sample_music_array, android.R.layout.simple_spinner_item);
        sample_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        speaker_sp.setAdapter(sample_adapter);

        Button speakerPlay = dialog.findViewById(R.id.thingy_play_music);
        speakerPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakerControl(speaker_sp.getSelectedItemPosition());
            }
        });
    }

    @Override
    public void populateMenu(Dialog dialog) {
        dialog.setTitle("Control Thingy");
        switch (option) {
            case 0:
                lightMenu(dialog);
                break;

            case 1:
                speakerMenu(dialog);
                break;

            default:
                lightMenu(dialog);
                break;
        }

    }

    private void toggleLight() {
        Log.d(TAG, "Toggling the light");
        byte[] addrBytes = macStr2Byte(mDevice.get().getUuid());
        byte[] cmd = Arrays.copyOf(addrBytes, addrBytes.length + 2);
        cmd[addrBytes.length] = THINGY_LIGHT_CMD;
        cmd[addrBytes.length + 1] = isLightOn?THINGY_OFF_CMD_BYTE:THINGY_ON_CMD_BYTE;

        mDevice.get().sendBleCmd(cmd);
        mDevice.get().userInteracted();
        isLightOn = !isLightOn;
    }

    private void speakerControl(int selection) {
        Log.d(TAG, "Controlling speaker");
        byte[] addrBytes = macStr2Byte(mDevice.get().getUuid());
        byte[] cmd = Arrays.copyOf(addrBytes, addrBytes.length + 2);
        cmd[addrBytes.length] = THINGY_SOUND_CMD;
        cmd[addrBytes.length + 1] = (byte) selection;
        mDevice.get().sendBleCmd(cmd);
        mDevice.get().userInteracted();
    }

    private void flashLight() {
        Log.d(TAG, "Flashing Light");
        byte[] addrBytes = macStr2Byte(mDevice.get().getUuid());
        byte[] cmd = Arrays.copyOf(addrBytes, addrBytes.length + 1);
        cmd[addrBytes.length] = THINGY_FLASH_CMD;

        mDevice.get().sendBleCmd(cmd);
        mDevice.get().userInteracted();
    }

}
