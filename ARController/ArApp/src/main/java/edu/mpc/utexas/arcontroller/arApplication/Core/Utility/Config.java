package edu.mpc.utexas.arcontroller.arApplication.Core.Utility;

import java.util.HashMap;
import java.util.Map;

public class Config {    // Menu options
    public final static int CMD_BACK = -1;
    public final static int CMD_START_SERVICE = 1;
    public final static int CMD_RESET_SERVICE = 2;
    public final static int CMD_FEEDBACK = 3;
    public final static int CMD_DEVICE_NULL = 4;
    public final static int CMD_AUTO_FEEDBACK = 5;
    public final static int CMD_SAVE_SNAPSHOT = 6;
    public final static int CMD_RESTORE_SNAPSHOT = 7;
    public final static int CMD_INITIALIZATION_TEST = 8;



    public final static int DEVICE_MENU_BASE = 20;

    public final static Map<String, float[]> MODEL_ADJUST = new HashMap<String, float[]>() {{
        put("thingy3", new float[]{1.0f/5.0f, 1.0f/3.5f, 1.0f/8.0f});
        put("echo", new float[]{1.0f/3.0f, 1.0f/2.0f, 1.0f/4.0f});
        put("nest4", new float[]{1.0f/3.0f,1.0f/2.0f, 1.0f/4.0f});
        put("philipsLightbulb", new float[]{1.0f/2.4f,1.0f/4.0f, 1.0f/4.0f});
    }};
    public final static float THINGY_MODEL_SCALE_X = 1.0f/5.0f;
    public final static float THINGY_MODEL_SCALE_Y = 1.0f/3.5f;
    public final static float THINGY_MODEL_SCALE_Z = 1.0f/8.0f;


    public final static float ECHO_MODEL_SCALE_X = 1.0f/3f;
    public final static float ECHO_MODEL_SCALE_Y = 1.0f/2f;
    public final static float ECHO_MODEL_SCALE_Z = 1.0f/4.0f;

    public final static String HUE_BRIDGE_URL = "http://192.168.1.124/";
    public final static String HUE_USER_NAME = "ARController_App";
    public final static String HUE_USER_TOKEN = "xGCk-0h-wmpBOQ6mrqM6UHt1vYzHOg8KOvqbilWl";

    public final static String LOG_FILE_DIR = "/logs";

}
