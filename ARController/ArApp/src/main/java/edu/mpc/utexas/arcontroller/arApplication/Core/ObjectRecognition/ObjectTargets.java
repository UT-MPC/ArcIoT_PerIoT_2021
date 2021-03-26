/*===============================================================================
Copyright (c) 2020 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.

This file is provided as a sample App by Vuforia and modified by Jie Hua, MPC Lab, The Univ of Texas.
===============================================================================*/

package edu.mpc.utexas.arcontroller.arApplication.Core.ObjectRecognition;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.DeviceTrackableResult;
import com.vuforia.Matrix34F;
import com.vuforia.ObjectTargetResult;
import com.vuforia.ObjectTracker;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.TrackableList;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vec3F;
import com.vuforia.Vuforia;

import java.util.ArrayList;
import java.util.Vector;

import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.SampleActivityBase;
import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.SampleApplicationControl;
import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.SampleApplicationException;
import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.SampleApplicationSession;
import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.utils.LoadingDialogHandler;
import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.utils.SampleAppTimer;
import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.utils.SampleApplicationGLView;
import edu.mpc.utexas.arcontroller.arApplication.BaseApplication.utils.Texture;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Device;
import edu.mpc.utexas.arcontroller.arApplication.Core.LocationServiceManager;
import edu.mpc.utexas.arcontroller.arApplication.Core.Utility.LogHelper;
import edu.mpc.utexas.arcontroller.arApplication.Core.ui.SampleAppMenu.SampleAppMenu;
import edu.mpc.utexas.arcontroller.arApplication.Core.ui.SampleAppMenu.SampleAppMenuGroup;
import edu.mpc.utexas.arcontroller.arApplication.Core.ui.SampleAppMenu.SampleAppMenuInterface;
import edu.mpc.utexas.arcontroller.arApplication.Core.ui.SampleAppMessage;
import edu.mpc.utexas.arcontroller.arApplication.R;

import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_AUTO_FEEDBACK;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_BACK;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_FEEDBACK;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_INITIALIZATION_TEST;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_RESET_SERVICE;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_RESTORE_SNAPSHOT;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_SAVE_SNAPSHOT;
import static edu.mpc.utexas.arcontroller.arApplication.Core.Utility.Config.CMD_START_SERVICE;

/**
 * The main activity for the ObjectTargets sample.
 * Object Targets allows users to create 3D targets for detection and tracking
 * To create your own Object Target, download the Vuforia Object Scanner tool from the
 * Vuforia developer website
 *
 * This class does high-level handling of the Vuforia lifecycle and any UI updates
 *
 * For ObjectTarget-specific rendering, check out ObjectTargetRenderer.java
 * For the low-level Vuforia lifecycle code, check out SampleApplicationSession.java
 */
public class ObjectTargets extends SampleActivityBase implements SampleApplicationControl,
        SampleAppMenuInterface
{
    private static final String TAG = "ObjectRecognition";
    
    private SampleApplicationSession vuforiaAppSession;
    
    private DataSet mCurrentDataset;

    private SampleApplicationGLView mGlView;

    private ObjectTargetRenderer mRenderer;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    
    private GestureDetector mGestureDetector;
    
    private boolean mLocServiceStarted = false;
    

    private RelativeLayout mUILayout;
    
    private SampleAppMenu mSampleAppMenu;
    ArrayList<View> mSettingsAdditionalViews = new ArrayList<>();

    private SampleAppMessage mSampleAppMessage;
    private SampleAppTimer mRelocalizationTimer;
    private SampleAppTimer mStatusDelayTimer;

    private int mCurrentStatusInfo;
    
    final LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    
    private boolean mIsDroidDevice = false;

    private LocationServiceManager mLocManager;
    private SampleAppMenuGroup deviceControlMenu;
    private Device menuDevice;
    private Device interactedDevice;
    Handler showFeedbackHandler;
    private boolean deviceTrackedScreen;

    private boolean feedbackMode;
    private boolean showOnTrack;

    private int[] expOrder = new int[]{
        1,2,3,
        2,1,3,
        1,3,2,
        3,2,1,
        3,1,2,
        1,3,2,
        1,2,3,
    };
    private int currentInteract = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        LogHelper.redirectLog(this);

        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        vuforiaAppSession = new SampleApplicationSession(this);
        
        startLoadingAnimation();
        
        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Load any sample specific textures:
        mTextures = new Vector<>();
        loadTextures();
        
        mGestureDetector = new GestureDetector(this, new GestureListener());
        
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
            "droid");

        // Relocalization timer and message
        mSampleAppMessage = new SampleAppMessage(this, mUILayout, mUILayout.findViewById(R.id.topbar_layout), false);
        mRelocalizationTimer = new SampleAppTimer(10000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (vuforiaAppSession != null)
                {
                    vuforiaAppSession.resetDeviceTracker();
                }

                super.onFinish();
            }
        };

        mStatusDelayTimer = new SampleAppTimer(1000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (mRenderer.isTargetCurrentlyTracked())
                {
                    super.onFinish();
                    return;
                }

                if (!mRelocalizationTimer.isRunning())
                {
                    mRelocalizationTimer.startTimer();
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mSampleAppMessage.show(getString(R.string.instruct_relocalize));
                    }
                });

                super.onFinish();
            }
        };

        mLocManager = new LocationServiceManager(this);
        showFeedbackHandler = new Handler();
        deviceTrackedScreen = false;
        feedbackMode = false;
        showOnTrack = false;

        // For experiment only.
        mLocManager.startService();
        mLocServiceStarted = true;
    }


    // Load specific textures from the APK, which we will later use for rendering.
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk(
            "ObjectRecognition/CubeWireframe.png", getAssets()));
    }
    

    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();

        showProgressIndicator(true);
        
        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        vuforiaAppSession.onResume();
    }
    
    
    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();
    }
    

    @Override
    protected void onPause()
    {
        Log.d(TAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        vuforiaAppSession.onPause();
    }
    

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mLocManager.onDestory();
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(TAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }
    

    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        
        mRenderer = new ObjectTargetRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
        mGlView.setPreserveEGLContextOnPause(true);

        setRendererReference(mRenderer);
    }
    
    
    private void startLoadingAnimation()
    {
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay, null);
        
        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        RelativeLayout topbarLayout = mUILayout.findViewById(R.id.topbar_layout);
        topbarLayout.setVisibility(View.VISIBLE);

        TextView title = mUILayout.findViewById(R.id.topbar_title);
        title.setText(getText(R.string.app_name));

        mSettingsAdditionalViews.add(topbarLayout);

        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);
        
        // Shows the loading indicator at start
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
    }
    

    @Override
    public boolean doLoadTrackersData()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        
        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();
        
        if (mCurrentDataset == null)
            return false;
        
        if (!mCurrentDataset.load("ObjectRecognition/AR_tracking_OT.xml",
            STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;
        
        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;
        
        TrackableList trackableList = mCurrentDataset.getTrackables();
        for (Trackable trackable : trackableList)
        {
            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d(TAG, "UserData:Set the following user data "
                + trackable.getUserData());
        }
        
        return true;
    }
    
    
    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;
        
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        
        if (mCurrentDataset != null && mCurrentDataset.isActive())
        {
            if (objectTracker.getActiveDataSets().at(0).equals(mCurrentDataset)
                && !objectTracker.deactivateDataSet(mCurrentDataset))
            {
                result = false;
            }
            else if (!objectTracker.destroyDataSet(mCurrentDataset))
            {
                result = false;
            }
            
            mCurrentDataset = null;
        }
        
        return result;
    }


    @Override
    public void onVuforiaResumed()
    {
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }


    // Called once Vuforia has been initialized or
    // an error has caused Vuforia initialization to stop
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            mSampleAppMenu = new SampleAppMenu(this, this, "ArcIoT",
                mGlView, mUILayout, mSettingsAdditionalViews);
            setSampleAppMenuSettings();

            vuforiaAppSession.startAR();

        }
        else
        {
            Log.e(TAG, exception.getString());
            if(exception.getCode() == SampleApplicationException.LOADING_TRACKERS_FAILURE)
            {
                showInitializationErrorMessage( 
                    getString(R.string.INIT_OBJECT_DATASET_NOT_FOUND_TITLE),
                    getString(R.string.INIT_OBJECT_DATASET_NOT_FOUND));
            }
            else
            {
                showInitializationErrorMessage( getString(R.string.INIT_ERROR),
                    exception.getString() );
            }
        }
    }


    @Override
    public void onVuforiaStarted()
    {
        mRenderer.updateRenderingPrimitives();

        // Set camera focus mode
        if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
        {
            // If continuous autofocus mode fails, attempt to set to a different mode
            if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
            {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            }
        }

        showProgressIndicator(false);
    }


    private void showProgressIndicator(boolean show)
    {
        if (show)
        {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        }
        else
        {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }


    private void showInitializationErrorMessage(String title, String message)
    {
        final String errorMessage = message;
        final String messageTitle = title;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }
                
                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    ObjectTargets.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(messageTitle)
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(getString(R.string.button_OK),
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }

    private boolean setIsTargetCurrentlyTracked(TrackableResultList trackableResultList)
    {
        for(TrackableResult result : trackableResultList)
        {
            // Check the tracking status for result types
            // other than DeviceTrackableResult. ie: ImageTargetResult
            if (!result.isOfType(DeviceTrackableResult.getClassType()))
            {
                int currentStatus = result.getStatus();
                int currentStatusInfo = result.getStatusInfo();

                // The target is currently being tracked if the status is TRACKED|NORMAL
                if (currentStatus == TrackableResult.STATUS.TRACKED
                        || currentStatusInfo == TrackableResult.STATUS_INFO.NORMAL)
                {
                    return true;
                }
            }
        }

        return false;
    }
    // Called every frame
    @Override
    public void onVuforiaUpdate(State state)
    {
        TrackableResultList trackableResultList = state.getTrackableResults();
        Vec3F pos = null;
        String deviceType = null;

        for (TrackableResult result : trackableResultList) {
            if (result.isOfType(ObjectTargetResult.getClassType()) && result.getStatus() != TrackableResult.STATUS.LIMITED) {
                Trackable trackable = result.getTrackable();

                deviceType = trackable.getName();
                Matrix34F modelMatrix = result.getPose();
                // the position matrix is sorted as [x, y, -z].
                // x is the distance to the bottom of the center of the camera
                // y is the distance to the right of the center of the camera
                pos = new Vec3F(modelMatrix.getData()[3], modelMatrix.getData()[7], modelMatrix.getData()[11]);
            }
        }
        if (pos != null) {
            deviceTrackedScreen = true;
            mLocManager.trackPosUpdate(deviceType, pos.getData()[1], -pos.getData()[0], pos.getData()[2]);
        } else {
            deviceTrackedScreen = false;
            mLocManager.lostTrack();
        }
    }
    
    
    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Initialize the Object Tracker
        tracker = tManager.initTracker(ObjectTracker.getClassType());

        if (tracker == null)
        {
            Log.e(
                    TAG,
                "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        }
        else
        {
            Log.i(TAG, "Tracker successfully initialized");
        }

        // Initialize the Positional Device Tracker
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker)
                tManager.initTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null)
        {
            Log.i(TAG, "Successfully initialized Device Tracker");
        }
        else
        {
            Log.e(TAG, "Failed to initialize Device Tracker");
        }

        return result;
    }
    
    
    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null && objectTracker.start())
        {
            Log.i(TAG, "Successfully started Object Tracker");
        }
        else
        {
            Log.e(TAG, "Failed to start Object Tracker");
            result = false;
        }
        
        return result;
    }
    
    
    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker != null)
        {
            objectTracker.stop();
            Log.i(TAG, "Successfully stopped object tracker");
        }
        else
        {
            Log.e(TAG, "Failed to stop object tracker");
            result = false;
        }
        
        return result;
    }
    
    
    @Override
    public boolean doDeinitTrackers()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());
        
        return result;
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        return (mSampleAppMenu != null && mSampleAppMenu.processEvent(event)
                || mGestureDetector.onTouchEvent(event));
    }



    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;
        
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);

        group = mSampleAppMenu.addGroup(getString(R.string.menu_locService_title), true);
        group.addSelectionItem("Device Tracking Service",
                CMD_START_SERVICE, mLocManager.isBind());
        group.addTextItem("Reset Service", CMD_RESET_SERVICE);
        group.addTextItem("Provide feedback", CMD_FEEDBACK);


        group = mSampleAppMenu.addGroup("Home Experiment", true);


        group.addSelectionItem("Enable auto feedback",
                CMD_AUTO_FEEDBACK, feedbackMode);
        group.addTextItem("Save Snapshot", CMD_SAVE_SNAPSHOT);
        group.addTextItem("Restore Snapshot", CMD_RESTORE_SNAPSHOT);

        group.addSelectionItem("Initialization Mode", CMD_INITIALIZATION_TEST, showOnTrack);

//        deviceControlMenu = mSampleAppMenu.addGroup(
//                getString(R.string.menu_deviceController_title), true);
//        deviceControlMenu.addTextItem(
//                getString(R.string.menu_deviceController_noDevice), CMD_DEVICE_NULL);

        mSampleAppMenu.attachMenu();
    }

    private void resetService() {
        if (!mLocServiceStarted) {
            showToast("You have to start the service first");
        } else {
            mLocManager.resetService();
        }
        currentInteract = 0;
    }


    // In this function you can define the desired behavior for each menu option
    // Each case corresponds to a menu option
    @Override
    public boolean menuProcess(int command)
    {
        switch (command)
        {
            case CMD_BACK:
                finish();
                return true;
            
            case CMD_START_SERVICE:
                if (mLocServiceStarted) {
                    mLocManager.stopService();
                } else {
                    mLocManager.checkPermission(getBaseContext(), this);
                    mLocManager.startService();
                }
                mLocServiceStarted = !mLocServiceStarted;
                return true;

            case CMD_RESET_SERVICE:
                resetService();
                return true;

            case CMD_FEEDBACK:
                showFeedback();
                return true;

            case CMD_AUTO_FEEDBACK:
                feedbackMode = !feedbackMode;
                return true;

            case CMD_SAVE_SNAPSHOT:
                if (!mLocServiceStarted) {
                    showToast("You have to start the service first");
                } else {
                    Log.d(TAG, "Save the snapshot");
                    mLocManager.saveSnapshot();
                }
                return true;

            case CMD_RESTORE_SNAPSHOT:
                if (!mLocServiceStarted) {
                    showToast("You have to start the service first");
                } else {
                    Log.d(TAG, "Restoring the snapshot");
                    mLocManager.restoreSnapshot();
                }
                return true;

            case CMD_INITIALIZATION_TEST:
                if (showOnTrack) {
                    Log.d(TAG, "Stop the initialization test!!");
                } else {
                    Log.d(TAG, "Start the initialization test!");
                    // For experiment only
                    resetService();
                }
                showOnTrack = !showOnTrack;
                return true;

            default:
//                if (menuDevice != null) {
//                    return menuDevice.handleEvent(command);
//                } else {
//                    return false;
//                }
                return false;
        }
    }


    public void checkForRelocalization(final int statusInfo)
    {
        if (mCurrentStatusInfo == statusInfo)
        {
            return;
        }

        mCurrentStatusInfo = statusInfo;

        if (mCurrentStatusInfo == TrackableResult.STATUS_INFO.RELOCALIZING)
        {
            // If the status is RELOCALIZING, start the timer
            if (!mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.startTimer();
            }
        }
        else
        {
            // If the status is not RELOCALIZING, stop the timers and hide the message
            if (mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.stopTimer();
            }

            if (mRelocalizationTimer.isRunning())
            {
                mRelocalizationTimer.stopTimer();
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mSampleAppMessage.hide();
                }
            });
        }
    }


    private void clearSampleAppMessage()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (mSampleAppMessage != null)
                {
                    mSampleAppMessage.hide();
                }
            }
        });
    }

    
    private void showToast(String text)
    {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("Permission Granted!");
                } else {
                    showToast("Permission Denied!");
                }
        }
    }

    private Dialog getInvalidDialog() {
        return null;
    }

    private boolean displayInteractToast() {
        return false;
//        if (currentInteract == expOrder.length) {
//            showToast("Interactions finished, you can start the next experiment.");
//            Log.d(TAG, "A experiment has finished.");
//            return true;
//        } else {
//            showToast("Please interact with Thingy " + expOrder[(currentInteract % expOrder.length)] + " next");
//            return false;
//        }
    }

    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();


        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }


        // Process Single Tap event to trigger autofocus
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            Log.d(TAG, "one tap");
            // For experiment only
            if (displayInteractToast()) {
                currentInteract = 0;
                return true;
            }

            int x = (int) e.getX();
            int y = (int) e.getY();

            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus");

            // Generates a Handler to trigger continuous auto-focus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (!autofocusResult)
                        Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus");
                }
            }, 1000L);


            Log.d(TAG, "User request to interact!!!");
            if (!deviceTrackedScreen) {
                noDeviceOnScreenDialog().create().show();
                return true;
            }
            if (mLocManager.isTracked()) {
                menuDevice = mLocManager.getTrackedDevice();
                menuDevice.showController(ObjectTargets.this, x,y);
//                Log.d(TAG, "A device interface is displayed??? " + System.currentTimeMillis());

//                mSampleAppMenu.removeGroup(deviceControlMenu);
//                deviceControlMenu = mSampleAppMenu.addGroup(
//                        getString(R.string.menu_deviceController_title), true);
//                menuDevice.populateMenu(deviceControlMenu);
//                mSampleAppMenu.attachMenu();
            } else {
                Log.d(TAG, "Fail to track the user clicked object.");

                menuDevice = null;
                Dialog tmp = Device.invalidDeviceDialog(ObjectTargets.this).show();
//                mSampleAppMenu.removeGroup(deviceControlMenu);
//                deviceControlMenu = mSampleAppMenu.addGroup(
//                        getString(R.string.menu_deviceController_title), true);
//                deviceControlMenu.addTextItem(
//                        getString(R.string.menu_deviceController_noDevice), CMD_DEVICE_NULL);
//                mSampleAppMenu.attachMenu();
            }
            return true;
        }
    }


    private void sendFeedback(boolean isPositive) {
        if (interactedDevice != null) {
            interactedDevice.getFeedback(isPositive);
        }
    }

    private AlertDialog.Builder feedbackDiaglog() {
        return new AlertDialog.Builder(ObjectTargets.this)
                .setMessage("Did the device on screen flash?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "User provided a negative feedback");
                        sendFeedback(false);
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "User provided a positive feedback");
                        sendFeedback(true);
                    }
                });
    }

    private AlertDialog.Builder noDeviceOnScreenDialog() {
        return new AlertDialog.Builder(ObjectTargets.this)
                .setMessage("No device tracked on the screen. Please point at the device closer or a different angle.\n " +
                        "Try again when you see a green bounding box around the object.")
                .setNegativeButton("Cancel", null);
    }

    public boolean renderModel() {
        return (!showOnTrack) || mLocManager.isTracked();
    }

    private void showFeedback() {
        if (interactedDevice == null) {
            showToast("You have to interact with a device first");
        } else {
            Log.d(TAG, "Feedback dialog is shown to the user.");
            feedbackDiaglog().create().show();
        }
    }

    public void userInteractedWithDevice(Device interactedDevice) {
        Log.d(TAG, "User issued a interaction with device " + interactedDevice.getUuid());
        this.interactedDevice = interactedDevice;
        currentInteract += 1;
        displayInteractToast();
        if (feedbackMode) {
            showFeedbackHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showFeedback();
                }
            }, 1000L);
        }
    }

}
