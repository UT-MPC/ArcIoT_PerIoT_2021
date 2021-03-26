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

import edu.mpc.utexas.arcontroller.arApplication.Core.Device.EchoDot;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Thingy;
import edu.mpc.utexas.arcontroller.arApplication.R;

import static edu.mpc.utexas.arcontroller.arApplication.Core.Device.Utility.macStr2Byte;

public class SpeakerMenu extends DeviceMenu{
    private static final String TAG = "EchoMenu";
    private static final int DEFAULT_OPT = 0;

    private WeakReference<EchoDot> mDevice;
    private Spinner speaker_sp;
    private boolean isLightOn;

    public int getOption() {
        return option;
    }

    private int option;
    private String displayTitle = "Thermostat";

    public SpeakerMenu(EchoDot device) {
        mDevice = new WeakReference<>(device);
        isLightOn = false;
        option = DEFAULT_OPT;
    }

    public SpeakerMenu(EchoDot device, int option) {
        mDevice = new WeakReference<>(device);
        isLightOn = false;
        this.option = option;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
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
        dialog.setTitle(displayTitle);
        speakerMenu(dialog);

    }


    private void speakerControl(int selection) {
        Log.d(TAG, "Controlling speaker");
        mDevice.get().userInteracted();
    }

}
