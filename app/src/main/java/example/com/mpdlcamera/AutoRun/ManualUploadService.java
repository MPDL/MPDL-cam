package example.com.mpdlcamera.AutoRun;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.squareup.otto.Produce;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import example.com.mpdlcamera.Model.DataItem;
import example.com.mpdlcamera.Model.LocalModel.Image;
import example.com.mpdlcamera.Model.LocalModel.Task;
import example.com.mpdlcamera.Otto.OttoSingleton;
import example.com.mpdlcamera.Otto.UploadEvent;
import example.com.mpdlcamera.Retrofit.RetrofitClient;
import example.com.mpdlcamera.Utils.DeviceStatus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedFile;

/**
 * Created by yingli on 2/3/16.
 */
public class ManualUploadService extends Service {

    private static final String TAG = ManualUploadService.class.getSimpleName();

    List<Image> waitingImages = null;
    List<Image> finishedImages = null;

    //  position in waitingImage list

    String currentImageId;
    Task task;
    String collectionID;

    // pass currentTaskId to service
    private String currentTaskId;
    private List<String> currentTaskList = new ArrayList<>();

    // SharedPreferences
    private SharedPreferences mPrefs;
    private String apiKey;

    //
    private TypedFile typedFile;
    private String json;
//    private String collectionID;
    private Context activity = this;

    // handler for toast
    private Handler handler = new Handler();


    public class ServiceBinder extends Binder {

        public ManualUploadService getService() {

            return ManualUploadService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service onBind");
        return new ServiceBinder();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service onStartComman");

        //prepare apiKey
        mPrefs = activity.getSharedPreferences("myPref", 0);
        apiKey = mPrefs.getString("apiKey", "");
        String email = mPrefs.getString("email", "");

        // prepare taskId
        try {
            currentTaskId = intent.getStringExtra("currentTaskId");
            Log.i(TAG, "get currentTaskId" + currentTaskId);
        }catch (Exception e){
//            Log.i(TAG, e.getMessage());
              Log.i(TAG, "e~~~");
        }
        /** add taskId to currentTask list **/
        currentTaskList.add(currentTaskId);

        for(final String taskId:currentTaskList) {
                    Task task = new Select().from(Task.class).where("taskId = ?", taskId).executeSingle();
                    //prepare collectionId
                    String collectionID = task.getCollectionId();
                    Log.e(TAG,collectionID);

                    if (!taskIsStopped()) {
                        Log.v(TAG, "not stopped");
                        Log.v(TAG, task.getState());
                        Log.i(TAG, "Thread --> startUpload()");
                        startUpload(taskId, collectionID);
                    }
                }
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
    }


    private void startUpload(String taskId, String collectionID) {

        if(!taskIsStopped()){

            /** WAITING, FINISHED **/
            waitingImages = new Select().from(Image.class).where("taskId = ?", taskId).where("state = ?", String.valueOf(DeviceStatus.state.WAITING)).orderBy("RANDOM()").execute();

        if (waitingImages!=null && waitingImages.size() > 0) {

                //TODO: fix error here
                Image image = new Select().from(Image.class).where("taskId = ?", taskId).where("state = ?",String.valueOf(DeviceStatus.state.WAITING)).orderBy("RANDOM()").executeSingle();
                String imageState = image.getState();
                String filePath = image.getImagePath();
                currentImageId = image.getImageId();
                Log.e(TAG,currentImageId);


            if (imageState.equalsIgnoreCase(String.valueOf(DeviceStatus.state.WAITING))) {
                    // TODO:upload image
                    String jsonPart1 = "\"collectionId\" : \"" +
                            collectionID +
                            "\"";

                    typedFile = new TypedFile("multipart/form-data", new File(filePath));
                    json = "{" + jsonPart1 + "}";
                    Log.v(TAG, "start uploading: " + filePath);
                    RetrofitClient.uploadItem(typedFile, json, callback, apiKey);

            }else {
                    Log.e(TAG,"illegal imageState:"+imageState);
                }
            }
        }
    }

    private void upload(Image image){

        if(!taskIsStopped()){

            //get collectionID

            //upload image
            String imageState = image.getState();
            String filePath = image.getImagePath();
            currentImageId = image.getImageId();
            Log.e(TAG, "upload" + currentImageId);

            if (imageState.equalsIgnoreCase(String.valueOf(DeviceStatus.state.WAITING))) {
                // TODO:upload image
                String jsonPart1 = "\"collectionId\" : \"" +
                        collectionID +
                        "\"";

                typedFile = new TypedFile("multipart/form-data", new File(filePath));
                json = "{" + jsonPart1 + "}";
                Log.v(TAG, "start uploading: " + filePath);
                RetrofitClient.uploadItem(typedFile, json, callback, apiKey);
            }
        }
    }


    Callback<DataItem> callback = new Callback<DataItem>() {
        @Override
        @Produce
        public void success(DataItem dataItem, Response response) {

            if(!taskIsStopped()){
            handler=new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                }
            });

            Log.v(TAG, dataItem.getCollectionId() + ":" + dataItem.getFilename());

            Log.e(TAG,currentImageId);
            Image currentImage = new Select().from(Image.class).where("imageId = ?",currentImageId).executeSingle();
            currentImage.setState(String.valueOf(DeviceStatus.state.FINISHED));
            currentImage.save();

            //TODO: ReWrite "remove after upload" function later
            mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

            /*
                Delete the file if the setting "Remove the photos after upload" is On
             */
            if(mPrefs.contains("RemovePhotosAfterUpload")) {
                if(mPrefs.getBoolean("RemovePhotosAfterUpload",true)) {

                    File file = typedFile.file();
                    Boolean deleted = file.delete();
                    Log.v(TAG, "deleted:" +deleted);
                }
            }
            //TODO: remove picture
//            adapter.notifyDataSetChanged();

                try {
                    /** WAITING, FINISHED **/
                    finishedImages = new Select().from(Image.class).where("taskId = ?", currentTaskId).where("state = ?", String.valueOf(DeviceStatus.state.FINISHED)).orderBy("RANDOM()").execute();
                } catch (Exception e) {
                }

                //DELETE TESTING
                Log.i(TAG, "finishedImages " + finishedImages.size());
                Log.i(TAG, "totalImages: " + task.getTotalItems());

            task.setFinishedItems(finishedImages.size());
            task.save();

            /** move on to next **/
            int finishedNum = task.getFinishedItems();
            int totalNum = task.getTotalItems();
            if(totalNum>finishedNum){
                Image image = new Select().from(Image.class).where("taskId = ?", currentTaskId).where("state = ?",String.valueOf(DeviceStatus.state.WAITING)).orderBy("RANDOM()").executeSingle();
                if(image!=null){
                upload(image);
                }
            }else {
                task.setState(String.valueOf(DeviceStatus.state.FINISHED));
                Log.i(TAG,"task finished");
            }
            }
        }

        @Override
        public void failure(RetrofitError error) {

            // set Task "INTERRUPTED" while 403||422
//            if(error.getResponse().getStatus() == 403 || error.getResponse().getStatus() == 422 ) {
//                task.setState(String.valueOf(DeviceStatus.state.INTERRUPTED));
//                task.save();
//                Log.e(TAG, "task: " + task.getTaskId());
//                Log.e(TAG, task.getState());
//                Log.e(TAG, error.getResponse().getStatus()+"");
//                return;
//            }

            if (!taskIsStopped()) {
            Image currentImage = new Select().from(Image.class).where("imageId = ?",currentImageId).executeSingle();
            //change state not saved here

//                    currentImage.setState(String.valueOf(DeviceStatus.state.INTERRUPTED));
//                    currentImage.setLog(error.getKind().name());
//                    Log.e(TAG, error.getKind().name());

            //


            if (error == null || error.getResponse() == null) {
                OttoSingleton.getInstance().post(new UploadEvent(null));
                if(error.getKind().name().equalsIgnoreCase("NETWORK")) {


                    handler=new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
//                            Toast.makeText(activity, "Please Check your Network Connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                }else {

                    handler=new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(activity, "Upload failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                OttoSingleton.getInstance().post(
                        new UploadEvent(error.getResponse().getStatus()));
                String jsonBody = new String(((TypedByteArray) error.getResponse().getBody()).getBytes());
                if (jsonBody.contains("already exists")) {

                    handler=new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
//                            Toast.makeText(activity, "Photo already exists", Toast.LENGTH_SHORT).show();
                        }
                    });
                    currentImage.setLog(error.getKind().name() + " already exists");
                    currentImage.setState(String.valueOf(DeviceStatus.state.FINISHED));
                    currentImage.save();

                }
                else {

                    handler=new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(activity, "Upload failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            // save state changes
            currentImage.save();
            Log.v(TAG, String.valueOf(error));

            //TODO: continue upload


                try {
                    /** WAITING, INTERRUPTED, STARTED, (FINISHED + STOPPED) **/
                    finishedImages = new Select().from(Image.class).where("taskId = ?", currentTaskId).where("state = ?", String.valueOf(DeviceStatus.state.FINISHED)).orderBy("RANDOM()").execute();
                } catch (Exception e) {
                }

                //DELETE TESTING
                Log.i(TAG, "finishedImages " + finishedImages.size());
                Log.i(TAG, "totalImages: " + task.getTotalItems());

            task.setFinishedItems(finishedImages.size());
            task.save();

            /** move on to next **/
            int finishedNum = task.getFinishedItems();
            int totalNum = task.getTotalItems();
            if(totalNum>finishedNum){
                // continue anyway
                Image image = new Select().from(Image.class).where("taskId = ?", currentTaskId).where("state = ?",String.valueOf(DeviceStatus.state.WAITING)).orderBy("RANDOM()").executeSingle();
                if(image!=null){
                upload(image);
                }
            }else {
                Log.i(TAG,"task finished");
            }
            }
        }
    };

    private boolean taskIsStopped (){
        task = new Select().from(Task.class).where("taskId = ?", currentTaskId).executeSingle();

        try{
            if(task.getState().equalsIgnoreCase(String.valueOf(DeviceStatus.state.STOPPED))){
                Log.v(TAG,"taskIsStopped");
                return true;
            }else{
                Log.v(TAG,"task is not stopped");
                return false;
            }}catch (Exception e){
            Log.e(TAG,"taskIsStopped exception");
            return false;
        }


    }
}