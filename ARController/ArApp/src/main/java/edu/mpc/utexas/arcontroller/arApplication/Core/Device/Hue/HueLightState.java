package edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue;

import android.util.Log;

public class HueLightState {
    private static String TAG = "ThingState";

    private boolean isOn;

    private int brightness = -1;            // range 1 - 254, -1 means not set
    private int hue = -1;                   // range 0 - 65536, -1 means not set
    private int saturation = -1;            // range 0 - 254, -1 means not set

    private int red = -1;                   // range 0 - 65536, -1 means not set
    private int green = -1;                 // range 0 - 65536, -1 means not set
    private int blue = -1;                  // range 0 - 65536, -1 means not set

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        if (brightness < 1){
            this.brightness = 1;
            Log.v(TAG, String.format("setBrightness(int brightness) - Setting brightness as %s, instead of %s", this.brightness, brightness));
        }
        else if (brightness > 254) {
            Log.v(TAG, String.format("setBrightness(int brightness) - Setting brightness as %s, instead of %s", this.brightness, brightness));
            this.brightness = 254;
        }
        else {
            this.brightness = brightness;
        }
    }

    public int getHue() {
        return hue;
    }

    public void setHue(int hue) {
        if (hue < 0){
            this.hue = 0;
            Log.v(TAG, String.format("setHue(int hue) - Setting hue as %s, instead of %s", this.hue, hue));
        }
        else if (hue > 65536) {
            this.hue = 65536;
            Log.v(TAG, String.format("setHue(int hue) - Setting hue as %s, instead of %s", this.hue, hue));
        }
        else {
            this.hue = hue;
        }
    }

    public int getSaturation() {
        return saturation;
    }

    public void setSaturation(int saturation) {
        if (saturation < 0){
            this.saturation = 0;
            Log.v(TAG, String.format("setSaturation(int saturation) - Setting saturation as %s, instead of %s", this.saturation, saturation));
        }
        else if (saturation > 254) {
            this.saturation = 254;
            Log.v(TAG, String.format("setSaturation(int saturation) - Setting saturation as %s, instead of %s", this.saturation, saturation));
        }
        else {
            this.saturation = saturation;
        }
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        if (red < 0){
            this.red = 0;
            Log.v(TAG, String.format("setRed(int red) - Setting red as %s, instead of %s", this.red, red));
        }
        else if (red > 65536) {
            this.red = 65536;
            Log.v(TAG, String.format("setRed(int red) - Setting red as %s, instead of %s", this.red, red));
        }
        else {
            this.red = red;
        }
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        if (green < 0){
            this.green = 0;
            Log.v(TAG, String.format("setGreen(int green) - Setting green as %s, instead of %s", this.green, green));
        }
        else if (green > 65536) {
            this.green = 65536;
            Log.v(TAG, String.format("setGreen(int green) - Setting green as %s, instead of %s", this.green, green));
        }
        else {
            this.green = green;
        }
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        if (blue < 0){
            this.blue = 0;
            Log.v(TAG, String.format("setBlue(int blue) - Setting blue as %s, instead of %s", this.blue, blue));
        }
        else if (blue > 65536) {
            this.blue = 65536;
            Log.v(TAG, String.format("setBlue(int blue) - Setting blue as %s, instead of %s", this.blue, blue));
        }
        else {
            this.blue = blue;
        }
    }

    @Override
    public String toString() {
        String string = "";

        string += "LightState{";
        string += "isOn=" + isOn + ", ";
        if (brightness >= 0)
            string += "brightness=" + brightness + ", ";
        if (hue >= 0)
            string += "hue=" + hue + ", ";
        if (saturation >= 0)
            string += "saturation=" + saturation + ", ";
        if (red >= 0)
            string += "red=" + red + ", ";
        if (green >= 0)
            string += "green=" + green + ", ";
        if (blue >= 0)
            string += "blue=" + blue + ", ";
        string += "}";

        string = string.replace(", }", "}");

        return string;
    }
}
