package de.mpg.mpdl.labcam.ItemDetails;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import de.mpg.mpdl.labcam.Gallery.RemoteListDialogFragment;
import de.mpg.mpdl.labcam.LocalFragment.DialogsInLocalFragment.MicrophoneDialogFragment;
import de.mpg.mpdl.labcam.LocalFragment.DialogsInLocalFragment.NoteDialogFragment;
import de.mpg.mpdl.labcam.Model.LocalModel.Image;
import de.mpg.mpdl.labcam.Model.LocalModel.Task;
import de.mpg.mpdl.labcam.R;
import de.mpg.mpdl.labcam.Utils.DBConnector;
import de.mpg.mpdl.labcam.Utils.DeviceStatus;
import de.mpg.mpdl.labcam.code.activity.LocalImageActivity;
import de.mpg.mpdl.labcam.code.common.widget.Constants;
import de.mpg.mpdl.labcam.code.rxbus.EventSubscriber;
import de.mpg.mpdl.labcam.code.rxbus.RxBus;
import de.mpg.mpdl.labcam.code.rxbus.event.NoteRefreshEvent;
import de.mpg.mpdl.labcam.code.rxbus.event.VoiceRefreshEvent;
import de.mpg.mpdl.labcam.code.utils.PreferenceUtil;

import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import rx.Subscription;

import static de.mpg.mpdl.labcam.Utils.BatchOperationUtils.addImages;
import static de.mpg.mpdl.labcam.Utils.BatchOperationUtils.noteDialogNewInstance;
import static de.mpg.mpdl.labcam.Utils.BatchOperationUtils.voiceDialogNewInstance;


public class DetailActivity extends AppCompatActivity implements android.support.v7.view.ActionMode.Callback{
    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    private Activity activity = this;
    private View rootView;
    private List<String> itemPathList;
    private PhotoViewAttacher mAttacher;

    private String serverName;

    // viewPager
    private ViewPager viewPager;
    private  ViewPagerAdapter viewPagerAdapter;
    boolean isLocalImage;
    int positionInList;

    // positionSet
    public Set<Integer> positionSet = new HashSet<>();

    private android.support.v7.view.ActionMode actionMode;
    android.support.v7.view.ActionMode.Callback ActionModeCallback = this;

    //user info
    private String username;
    private String userId;

    private Subscription mNoteRefreshEventSub;
    private Subscription mVoiceRefreshEventSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        observeNoteRefresh();
        observeVoiceRefresh();

        username = PreferenceUtil.getString(this, Constants.SHARED_PREFERENCES, Constants.USER_NAME, "");
        userId = PreferenceUtil.getString(this, Constants.SHARED_PREFERENCES, Constants.USER_ID, "");
        serverName = PreferenceUtil.getString(this, Constants.SHARED_PREFERENCES, Constants.SERVER_NAME, "");

        rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            itemPathList = extras.getStringArrayList("itemPathList");
            isLocalImage = extras.getBoolean("isLocalImage");
            positionInList = extras.getInt("positionInList");

            forceRefresh();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onCreateActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
        if (actionMode == null) {
            actionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contextual_menu_local, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(android.support.v7.view.ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_upload_local:
                Log.i(LOG_TAG, "upload");
                batchOperation(R.id.item_upload_local);
                mode.finish();
                return true;
            case R.id.item_microphone_local:
                Log.i(LOG_TAG, "microphone");
                batchOperation(R.id.item_microphone_local);
                mode.finish();
                return true;
            case R.id.item_notes_local:
                Log.i(LOG_TAG, "notes");
                batchOperation(R.id.item_notes_local);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(android.support.v7.view.ActionMode mode) {
        actionMode = null;
        positionSet.clear();
        viewPagerAdapter.notifyDataSetChanged();
    }



    /**upload methods**/
     /*
            upload the selected files
        */
    private void uploadList(List<String> fileList) {
        String currentTaskId = createTask(fileList);

        newInstance(currentTaskId).show(this.getFragmentManager(), "remoteListDialog");
    }

    public static RemoteListDialogFragment newInstance(String taskId)
    {
        RemoteListDialogFragment remoteListDialogFragment = new RemoteListDialogFragment();
        Bundle args = new Bundle();
        args.putString("taskId", taskId);
        remoteListDialogFragment.setArguments(args);
        return remoteListDialogFragment;
    }

    private String createTask(List<String> fileList){

        String uniqueID = UUID.randomUUID().toString();
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        Long now = new Date().getTime();

        Task task = new Task();
        task.setTotalItems(fileList.size());
        task.setFinishedItems(0);
        task.setTaskId(uniqueID);
        task.setUploadMode("MU");
        task.setState(String.valueOf(DeviceStatus.state.WAITING));
        task.setUserName(username);
        task.setUserId(userId);
        task.setServerName(serverName);
        task.setStartDate(String.valueOf(now));
        task.save();
        int num = addImages(fileList, task.getTaskId(), userId, serverName).size();
        task.setTotalItems(num);
        task.save();
        Log.v(LOG_TAG,"MU task"+task.getTaskId() );
        Log.v(LOG_TAG, "setTotalItems:" + num);

        return task.getTaskId();
    }

    private void addOrRemove(int position) {

        if (positionSet.contains(position)) {

            positionSet.remove(position);
        } else {

            positionSet.add(position);
        }
        if (positionSet.size() == 0) {

            actionMode.finish();
        } else {

            actionMode.setTitle(positionSet.size() + " selected");

            viewPagerAdapter.notifyDataSetChanged();

        }
    }

    private void batchOperation(int operationType){
        if(positionSet.size()!=0) {
            Log.v(LOG_TAG, " "+positionSet.size());
            List imagePathList = new ArrayList();
            for (Integer i : positionSet) {
                imagePathList.add(itemPathList.get(i));
            }

            if (imagePathList != null) {
                switch (operationType){
                    case R.id.item_upload_local:
                        uploadList(imagePathList);
                        break;
                    case R.id.item_microphone_local:
                        showVoiceDialog(imagePathList);
                        break;
                    case R.id.item_notes_local:
                        showNoteDialog(imagePathList);
                        break;
                }
            }
            imagePathList.clear();
        }
    }
    public void showVoiceDialog(List<String> imagePathList){
        voiceDialogNewInstance(imagePathList, userId, serverName).show(this.getFragmentManager(), "voiceDialogFragment");
    }

    public void showNoteDialog(List<String> imagePathList){
        noteDialogNewInstance(imagePathList, userId, serverName).show(this.getFragmentManager(),"noteDialogFragment");
    }

    private void observeNoteRefresh() {
        mNoteRefreshEventSub = RxBus.getDefault()
                .observe(NoteRefreshEvent.class)
                .subscribe(new EventSubscriber<NoteRefreshEvent>() {
                    @Override
                    public void onEvent(NoteRefreshEvent event) {
                        String imgPath = event.getImgPath();
                        if(itemPathList.contains(imgPath)){
                            positionInList = itemPathList.indexOf(imgPath);
                        }
                        forceRefresh();
                    }
                });
    }

    private void observeVoiceRefresh() {
        mVoiceRefreshEventSub = RxBus.getDefault()
                .observe(VoiceRefreshEvent.class)
                .subscribe(new EventSubscriber<VoiceRefreshEvent>() {
                    @Override
                    public void onEvent(VoiceRefreshEvent event) {
                        String imgPath = event.getImgPath();
                        if(itemPathList.contains(imgPath)){
                            positionInList = itemPathList.indexOf(imgPath);
                        }
                       forceRefresh();
                    }
                });
    }

    private void forceRefresh(){
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);

        viewPager = (ViewPager) rootView.findViewById(R.id.view_pager_detail_image);
        viewPagerAdapter = new ViewPagerAdapter(this,size,isLocalImage,itemPathList, userId, serverName);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(positionInList);
        if(isLocalImage) {
            viewPagerAdapter.setOnItemClickListener(new ViewPagerAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    if(actionMode != null) {
                        addOrRemove(position);
                        viewPagerAdapter.setPositionSet(positionSet);
                    }
                }

                @Override
                public void onItemLongClick(View view, int position) {
                    if (actionMode == null) {
                        actionMode = ((AppCompatActivity) activity).startSupportActionMode(ActionModeCallback);
                    }
                }
            });
        }else {
            viewPagerAdapter.setOnItemClickListener(null);
        }
    }
}