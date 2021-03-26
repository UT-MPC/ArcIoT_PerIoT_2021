package edu.mpc.utexas.arcontroller.arApplication.Core.Utility;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.LOG_FILE_DIR;

public class LogHelper {
    static boolean hasRedirected = false;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss z");

    public static void redirectLog(Context appCtx) {
//        if (hasRedirected) {
//            Log.d("LogHelper", "Logcat has already been redirected.");
//            return;
//        }

        File logDirectory = new File( appCtx.getFilesDir() + LOG_FILE_DIR );
        File logFile = new File( logDirectory , "logcat_" + sdf.format(Calendar.getInstance().getTime()) + ".txt" );

        // create log folder
        if ( !logDirectory.exists() ) {
            logDirectory.mkdir();
        }

        // clear the previous logcat and then write the new one to the file
        Log.d("LogHelper", "Start logging into " + logFile.getAbsolutePath());
        try {
            Process process = Runtime.getRuntime().exec("logcat -c");
            process = Runtime.getRuntime().exec("logcat -f " + logFile + " r.arApplicatio:S ACameraMetadata:S native:S");
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        hasRedirected = true;

    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }
}
