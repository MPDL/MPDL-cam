package example.com.mpdlcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Produce;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import example.com.mpdlcamera.Model.DataItem;
import example.com.mpdlcamera.Model.FolderModel;
import example.com.mpdlcamera.Model.MetaData;
import example.com.mpdlcamera.Model.User;
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
 * Created by kiran on 23.09.15.
 */
public class CustomAdapter extends ArrayAdapter<FolderModel> {

    Context mContext;

   /* public CustomAdapter(Context mContext,int resource,ArrayList<FolderModel> list ) {
        super(mContext,resource);
        this.mContext = mContext;
    } */

    ArrayList<String> permFolder = new ArrayList<String>();
    private final String LOG_TAG = CustomAdapter.class.getSimpleName();
    private String username;
    private String password;
    String prefOption;
    String networkStatus;
    private DataItem item = new DataItem();
    private MetaData meta = new MetaData();
    private String collectionID = DeviceStatus.collectionID;
    private User user;
    public TypedFile typedFile;
    String json;

    private ArrayList<FolderModel> folderList;

    public CustomAdapter(Context context, int textViewResourceId,
                         ArrayList<FolderModel> folderList) {
        super(context, textViewResourceId, folderList);
        this.mContext = context;
        this.folderList = new ArrayList<FolderModel>();
        this.folderList.addAll(folderList);
    }

    private class ViewHolder {
        TextView textView;
        Switch fSwitch;

    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        Log.v("ConvertView", String.valueOf(position));

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.row, null);

            holder = new ViewHolder();
            holder.textView = (TextView) convertView.findViewById(R.id.folder);
            holder.fSwitch = (Switch) convertView.findViewById(R.id.fswitch);


            convertView.setTag(holder);

            holder.fSwitch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Switch sw = (Switch) v;
                    FolderModel folder = (FolderModel) sw.getTag();

                    folder.setSelected(sw.isChecked());
                }
            });
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.fSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!isChecked) {

                    RelativeLayout rl = (RelativeLayout) buttonView.getParent();
                    TextView tv = (TextView) rl.findViewById(R.id.folder);
                    String folder = tv.getText().toString();

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(folder, "Off");
                    editor.commit();


                }

                else if(isChecked)
                {

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);

                    ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    String networkStatus;
                    networkStatus = networkInfo.getTypeName();


                    prefOption = settings.getString("status", "");


                    if (prefOption.equalsIgnoreCase("both") || (prefOption.equalsIgnoreCase("Wifi") && (networkStatus.equalsIgnoreCase("wifi")))) {
                        RelativeLayout rl = (RelativeLayout) buttonView.getParent();
                        TextView tv = (TextView) rl.findViewById(R.id.folder);
                        String folder = tv.getText().toString();

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(folder, "On");
                        editor.commit();


                        }


                    } else {
                        Toast.makeText(mContext.getApplicationContext(), "Please Switch On Wifi or change your Network Preference", Toast.LENGTH_LONG).show();
                    }

                }


            });



        FolderModel folder = folderList.get(position);
        holder.textView.setText(folder.getFolder());

        holder.fSwitch.setChecked(folder.isSelected());
        holder.fSwitch.setTag(folder);

        return convertView;

    }


}