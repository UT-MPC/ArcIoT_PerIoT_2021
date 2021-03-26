package edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue.Task;

import android.os.AsyncTask;

public class HueNetworkTask extends AsyncTask<Void, Void, Object> {

    private TaskBody task;

    public HueNetworkTask(TaskBody task) {
        this.task = task;
    }

    @Override
    protected Object doInBackground(Void... voids) {
        return task.run();
    }

    @Override
    protected void onPostExecute(Object result) {
        task.onComplete(result);
    }

    public interface TaskBody {
        Object run();
        void onComplete(Object result);
    }
}