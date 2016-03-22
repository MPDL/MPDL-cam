package example.com.mpdlcamera.ItemDetails;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import example.com.mpdlcamera.Gallery.RemoteListDialogFragment;
import example.com.mpdlcamera.R;
import example.com.mpdlcamera.Settings.SettingsActivity;
import uk.co.senab.photoview.PhotoViewAttacher;


public class DetailActivity extends AppCompatActivity implements android.support.v7.view.ActionMode.Callback{
    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    private Activity activity = this;
    private View rootView;
    private List<String> itemPathList;
    private PhotoViewAttacher mAttacher;

    // viewPager
    private ViewPager viewPager;
    private  ViewPagerAdapter viewPagerAdapter;

    // positionSet
    public Set<Integer> positionSet = new HashSet<>();

    private android.support.v7.view.ActionMode actionMode;
    android.support.v7.view.ActionMode.Callback ActionModeCallback = this;

    //user info
    private SharedPreferences mPrefs;
    private String username;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mPrefs = this.getSharedPreferences("myPref", 0);
        username = mPrefs.getString("username", "");
        userId = mPrefs.getString("userId","");

        rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            itemPathList = extras.getStringArrayList("itemPathList");
            boolean isLocalImage = extras.getBoolean("isLocalImage");
            int positionInList = extras.getInt("positionInList");

            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();

            Point size = new Point();
            display.getSize(size);

            viewPager = (ViewPager) rootView.findViewById(R.id.view_pager_detail_image);
            viewPagerAdapter = new ViewPagerAdapter(this,size,isLocalImage,itemPathList);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent showSettingIntent = new Intent(this, SettingsActivity.class);
            startActivity(showSettingIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** upload methods **/
    public static RemoteListDialogFragment newInstance(String taskId)
    {
        RemoteListDialogFragment remoteListDialogFragment = new RemoteListDialogFragment();
        Bundle args = new Bundle();
        args.putString("taskId", taskId);
        remoteListDialogFragment.setArguments(args);
        return remoteListDialogFragment;
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
                mode.finish();
                return true;
            default:
                return false;
        }    }

    @Override
    public void onDestroyActionMode(android.support.v7.view.ActionMode mode) {
        actionMode = null;
        positionSet.clear();
        viewPagerAdapter.notifyDataSetChanged();
    }

    private void addOrRemove(int position) {

        if (positionSet.contains(position)) {
            // 如果包含，则撤销选择
            positionSet.remove(position);
        } else {
            // 如果不包含，则添加
            positionSet.add(position);
        }
        if (positionSet.size() == 0) {
            // 如果没有选中任何的item，则退出多选模式
            actionMode.finish();
        } else {
            // 设置ActionMode标题
            actionMode.setTitle(positionSet.size() + " selected");
            // 更新列表界面，否则无法显示已选的item
            viewPagerAdapter.notifyDataSetChanged();
//            mSectionedAdapter.notifyDataSetChanged();
        }
    }
}