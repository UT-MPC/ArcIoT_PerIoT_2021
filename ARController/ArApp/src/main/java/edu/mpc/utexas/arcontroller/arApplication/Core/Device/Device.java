package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.DialogFragment;
import edu.mpc.utexas.arcontroller.arApplication.Core.ui.SampleAppMenu.SampleAppMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.ui.SampleAppMenu.SampleAppMenuGroup;

public abstract class Device {
    public static AlertDialog.Builder invalidDeviceDialog(Context ctx) {
        return new AlertDialog.Builder(ctx)
                .setMessage("The location of the device is not initialized. \nPlease stay close to the device and try again later.")
                .setNegativeButton("Cancel", null);
    }

    public int getOption() {
        return 0;
    }
//    public abstract SampleAppMenuGroup populateMenu(SampleAppMenuGroup menu);
    public abstract DialogFragment showController(Activity act, int x, int y);

    public void showDialog(Dialog dialog, int x, int y) {
        dialog.show();

        Window window = dialog.getWindow();
        window.setGravity(Gravity.TOP|Gravity.LEFT);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = x;
        params.y = y;
        window.setAttributes(params);
        window.setLayout(700, WindowManager.LayoutParams.WRAP_CONTENT);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        ColorDrawable c = new ColorDrawable(Color.WHITE);
        c.setAlpha(100);
        window.setBackgroundDrawable(c);
    }

    public abstract String getName();

    public abstract void getFeedback(boolean isPositive);

    public abstract String getUuid();

    public abstract void userInteracted();
}