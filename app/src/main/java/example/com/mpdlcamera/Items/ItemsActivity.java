package example.com.mpdlcamera.Items;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.activeandroid.ActiveAndroid;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import example.com.mpdlcamera.Folder.FolderListAdapter;
import example.com.mpdlcamera.Model.DataItem;
import example.com.mpdlcamera.Model.ImejiFolder;
import example.com.mpdlcamera.R;
import example.com.mpdlcamera.Retrofit.RetrofitClient;
import example.com.mpdlcamera.Utils.DeviceStatus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by allen on 03/09/15.
 */
public class ItemsActivity extends AppCompatActivity {


    private List<DataItem> dataList = new ArrayList<DataItem>();
    public  ItemsGridAdapter adapter;
    private GridView gridView;
    private View rootView;
    private String dataCollectionId;
    private Activity activity = this;
    private final String LOG_TAG = ItemsActivity.class.getSimpleName();
    SharedPreferences mPrefs;
    private String username;
    private String password;


    Toolbar toolbar;

    Callback<JsonObject> callback_Items = new Callback<JsonObject>() {
        @Override
        public void success(JsonObject jsonObject, Response response) {
            JsonArray array;
            List<DataItem> dataList = new ArrayList<>();

            array = jsonObject.getAsJsonArray("results");
            Log.i("results", array.toString());
            Gson gson = new Gson();
            for(int i = 0 ; i < array.size() ; i++){
                DataItem dataItem = gson.fromJson(array.get(i), DataItem.class);
                dataList.add(dataItem);
            }

            //load all data from imeji
            //adapter =  new CustomListAdapter(getActivity(), dataList);
            List<DataItem> dataListLocal = new ArrayList<DataItem>();

            ActiveAndroid.beginTransaction();
            try {
                // here get the string of Metadata Json
                for (DataItem item : dataList) {
                    dataListLocal.add(item);
                    //item.save();
                }
                ActiveAndroid.setTransactionSuccessful();
            } finally{
                ActiveAndroid.endTransaction();

                adapter =  new ItemsGridAdapter(activity, dataListLocal);
                gridView.setAdapter(adapter);

                adapter.notifyDataSetChanged();

            }

        }

        @Override
        public void failure(RetrofitError error) {
            Log.v(LOG_TAG, "get DataItem failed");
            Log.v(LOG_TAG, error.toString());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.items_grid_view);
        rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView titleView = (TextView) findViewById(R.id.title);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPrefs = activity.getSharedPreferences("myPref", 0);
        username = mPrefs.getString("username", "");
        password = mPrefs.getString("password", "");

        Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            dataCollectionId = intent.getStringExtra(Intent.EXTRA_TEXT);
            String title = intent.getStringExtra("folderTitle");

            getFolderItems(dataCollectionId);


            titleView.setText(title);
            //adapter =  new CustomListAdapter(getActivity(), dataList);
            adapter = new ItemsGridAdapter(activity, dataList);


            //rootView = inflater.inflate(R.layout.fragment_section_list_swipe, container, false);
            gridView = (GridView) findViewById(R.id.item_gridView);
            //listView = (SwipeMenuListView) rootView.findViewById(R.id.listView);
            gridView.setAdapter(adapter);

            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    DataItem dataItem = (DataItem) adapter.getItem(position);

                    Intent showDetailIntent = new Intent(activity, DetailActivity.class);
                    showDetailIntent.putExtra("itemPath", dataItem.getFileUrl());
                    startActivity(showDetailIntent);
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    private void getFolderItems(String collectionId){
        RetrofitClient.getCollectionItems(collectionId, callback_Items, username, password);
    }


}
