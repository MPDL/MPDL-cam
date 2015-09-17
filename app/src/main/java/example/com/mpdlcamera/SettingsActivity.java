package example.com.mpdlcamera;

import android.app.ListActivity;
import android.content.Context;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Produce;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import example.com.mpdlcamera.Model.DataItem;
import example.com.mpdlcamera.Model.MetaData;
import example.com.mpdlcamera.Model.User;
import example.com.mpdlcamera.Retrofit.RetrofitClient;
import example.com.mpdlcamera.Utils.DeviceStatus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;


/**
 * Created by kiran on 25.09.15.
 */


public class SettingsActivity extends ListActivity {


    private final String LOG_TAG = SettingsActivity.class.getSimpleName();
    private String collectionID = DeviceStatus.collectionID;
    private String username = DeviceStatus.username;
    private String password = DeviceStatus.password;
    CustomAdapter switchAdapter = null;
    private List<DataItem> dataList = new ArrayList<DataItem>();
    private DataItem item = new DataItem();
    private MetaData meta = new MetaData();
    private User user;
    String json;

    public TypedFile typedFile;


    private CheckBox checkSyncAll;
    List<String> itemIds = new ArrayList<String>();


    Callback<DataItem> callback = new Callback<DataItem>() {
        @Override
        @Produce
        public void success(DataItem dataItem, Response response) {
            //adapter =  new CustomListAdapter(getActivity(), dataList);
            //listView.setAdapter(adapter);
            Toast.makeText(getApplicationContext(), "Uploaded Successfully", Toast.LENGTH_LONG).show();
            Log.v(LOG_TAG, dataItem.getCollectionId() + ":" + dataItem.getFilename());
            itemIds.add(dataItem.getCollectionId());

           // new Delete().from(DataItem.class).where("filename = ?", dataItem.getFilename()).execute();

            List<DataItem> tempList =  dataList;
            for(int i = 0; i<dataList.size(); i++){
                DataItem d = tempList.get(i);
                dataList.remove(d);
            }

            //You cannot modify, add/remove, a List while iterating through it.
            //The foreach loop you are using creates an Iterator object in the background.
            // Use a regular for loop if you'd like to modify the list.

//            for (DataItem item: dataList){
//                //if(item.getFilename().equals(dataItem.getFilename())){
//                    dataList.remove(item);
//                //}
//            }
            //adapter.notifyDataSetChanged();

            //begin to upload only when all the dataItem are uploaded
            if(new Select()
                    .from(DataItem.class)
                    .where("isLocal = ?", true)
                    .execute().size()<1){
                //upload a POI as Album on Imeji
               // RetrofitClient.createPOI(createNewPOI(), callbackPoi, username, password);

                //TODO produce a message event to third-party fragment to display the POI on map
                OttoSingleton.getInstance().post(
                        new UploadEvent(response.getStatus()));
            }
            //processButton.setProgress(100); // set progress to 100 or -1 to indicate complete or error state
        }

        @Override
        public void failure(RetrofitError error) {
            Toast.makeText(getApplicationContext(), "Upload failed", Toast.LENGTH_LONG).show();
            if (error == null || error.getResponse() == null) {
                OttoSingleton.getInstance().post(new UploadEvent(null));
            } else {
                OttoSingleton.getInstance().post(
                        new UploadEvent(error.getResponse().getStatus()));
            }
            //String jsonBody =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
            //Log.v(LOG_TAG, jsonBody);
            Log.v(LOG_TAG, String.valueOf(error));
           // processButton.setProgress(-1); // set progress to 0 to switch back to normal state

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Generate listView from ArrayList
        displayListView();

        user = new User();
        user.setCompleteName("Kiran");
        user.save();



    }

    /*
    Displaying the ListView by using adapter
     */
    private void displayListView() {

        String[] albums = new String[]{MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;


        final ArrayList<String> folders = new ArrayList<String>();

        final ArrayList<FolderModel> folders1 = new ArrayList<FolderModel>();
        Cursor cur = getContentResolver().query(images, albums, null, null, null);

       // CharArrayBuffer buffer = new CharArrayBuffer(29);
        //cur.copyStringToBuffer(0,buffer);
        int co = cur.getColumnCount();
        String nam = cur.getColumnName(0);
        String[] nama = cur.getColumnNames();
        int rows = cur.getCount();

        checkSyncAll = (CheckBox) SettingsActivity.this.findViewById(R.id.syncAllCheck);

        //Logging the image count
        Log.i("ListingImages", " query count=" + cur.getCount());

        if (cur.moveToFirst()) {
            String album;
            int albumLocation = cur.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

            do {
                album = cur.getString(albumLocation);
                folders.add(album);
                Log.i("ListingImages", " album=" + album);
            } while (cur.moveToNext());
        }

        ArrayList<String> imageFolders = new ArrayList<String>();
        imageFolders = new ArrayList<String>(new LinkedHashSet<String>(folders));

        ArrayList<FolderModel> folderList = new ArrayList<FolderModel>();


        Iterator<String> folderIterator = imageFolders.iterator();
        while (folderIterator.hasNext()) {
            FolderModel folderOne = new FolderModel(folderIterator.next(), false);
            folderList.add(folderOne);
        }



        switchAdapter = new CustomAdapter(this,
                R.layout.row, folderList);
        setListAdapter(switchAdapter);

        final ListView listView = getListView();

        final int size = getListAdapter().getCount();

       /* Switch mainSwitch = (Switch) findViewById(R.id.fswitch);

        mainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                System.out.println("nobds");
            }
        });
 */
        checkSyncAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSyncAll.isChecked()) {


                    for (int i = 0; i < size; i++) {
                        RelativeLayout RelativeOne = (RelativeLayout) getViewByPosition(i, listView);
                        Switch mSwitch = (Switch) RelativeOne.findViewById(R.id.fswitch);
                        mSwitch.setChecked(true);

                    }
                }
                if (!checkSyncAll.isChecked()) {

                    for (int i = 0; i < size; i++) {
                        RelativeLayout RelativeOne = (RelativeLayout) getViewByPosition(i, listView);
                        Switch mSwitch = (Switch) RelativeOne.findViewById(R.id.fswitch);
                        mSwitch.setChecked(false);

                    }
                }

            }
        });
    }


    private FolderModel getModel(int position) {
        return (((CustomAdapter) getListAdapter()).getItem(position));
    }


    private class CustomAdapter extends ArrayAdapter<FolderModel> {

        private ArrayList<FolderModel> folderList;

        public CustomAdapter(Context context, int textViewResourceId,
                             ArrayList<FolderModel> folderList) {
            super(context, textViewResourceId, folderList);
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
                LayoutInflater vi = (LayoutInflater) getSystemService(
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

                    RelativeLayout rl = (RelativeLayout) buttonView.getParent();
                    TextView tv = (TextView) rl.findViewById(R.id.folder);
                    String folder = tv.getText().toString();

                    Toast.makeText(getApplicationContext(), folder + "is now synced", Toast.LENGTH_LONG).show();

                    Uri uri;
                    Cursor cursor;
                    int column_index_data, column_index_folder_name,column_index_file_name;
                    ArrayList<String> listOfAllImages = new ArrayList<String>();
                    String absolutePathOfImage = null;
                    String absoluteFileName = null;
                    String absoluteFolderName = null;
                    uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                    String[] projection = { MediaStore.MediaColumns.DATA,
                            MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DISPLAY_NAME};

                    cursor = getContentResolver().query(uri, projection, null,
                            null, null);

                    column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);



                    column_index_folder_name = cursor
                            .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

                    column_index_file_name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

                    HashMap<String,String> namePathMap = new HashMap<String, String>();

                    while (cursor.moveToNext()) {
                        absolutePathOfImage = cursor.getString(column_index_data);

                        absoluteFolderName = cursor.getString(column_index_folder_name);

                        absoluteFileName = cursor.getString(column_index_file_name);

                        if(absoluteFolderName.equalsIgnoreCase(folder)) {


                            listOfAllImages.add(absolutePathOfImage);

                            namePathMap.put(absoluteFileName,absolutePathOfImage);
                        }
                    }


                    Iterator hashIterator = namePathMap.keySet().iterator();
                    while(hashIterator.hasNext()) {
                        String fileName = (String)hashIterator.next();
                        String filePath = (String)namePathMap.get(fileName);
                        item.setFilename(fileName);
                        meta.setTags(null);

                        meta.setAddress("blabla");

                        meta.setTitle(fileName);

                        meta.setCreator(user.getCompleteName());

                        item.setCollectionId(collectionID);

                        item.setLocalPath(filePath);

                        item.setMetadata(meta);

                        item.setCreatedBy(user);

                        meta.save();
                        item.save();

                      //  dataList.add(item);
                        upload(item);


                    }
                   /* for(String imageInfo : listOfAllImages) {

                        Bitmap bm = BitmapFactory.decodeFile(imageInfo);
                        upload();
                    } */


                   // upload(dataList);











                }
            });

            FolderModel folder = folderList.get(position);
            holder.textView.setText(folder.getFolder());

            holder.fSwitch.setChecked(folder.isSelected());
            holder.fSwitch.setTag(folder);

            return convertView;

        }
    }




    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

   private void upload(DataItem item) {
        String jsonPart1 = "\"collectionId\" : \"" +
                collectionID +
                "\"";


            Gson gson = new GsonBuilder()
                    .serializeNulls()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            item.getMetadata().setDeviceID("1");
            typedFile = new TypedFile("multipart/form-data", new File(item.getLocalPath()));

            /* String jsonPart2 = "\"metadata\":[{\"labels\":[{\"language\":\"en\",\"value\":\"title\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"20150916_141350.wav@Amalienstraße 33, 80799 München, Germany\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"author\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"Allen\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"location\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\", \"value\":{\"text\":\"20150916_141350.wav@Amalienstraße 33, 80799 München, Germany\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"accuracy\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"10.0\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"deviceID\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"1\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"tags\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"test\"}}] ";
*/
         //   json = "{" + jsonPart1 + "," + jsonPart2 + "}";
            json ="{" + jsonPart1  +"}";

            Log.v(LOG_TAG, json);
            System.out.println(json);
            RetrofitClient.uploadItem(typedFile, json, callback, username, password);


    }











  /*  public void switchActivity(View view) {


{"labels":[{"language":"en","value":"title"}],"statementUri":"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW","typeUri":"http://imeji.org/terms/metadata#text","value":{"text":"20150916_141350.wav@Amalienstraße 33, 80799 München, Germany"}}



        System.out.println("coming here");
        Switch sw = (Switch) view;
        sw.setOnCheckedChangeListener(null);
        if (sw.isChecked()) {

        09-16 14:14:03.719: V/ReadyToUploadCollectionActivity(14752): {"collectionId" : "DCQVKA8esikfRTWi","metadata": [{"labels":[{"language":"en","value":"title"}],"statementUri":"http://dev-faces.mpdl.mpg.de/imeji/statement/iNUP1SRHR9OSZGy","typeUri":"http://imeji.org/terms/metadata#text","value":{"text":"20150916_141350.wav@Amalienstraße 33, 80799 München, Germany"}},{"labels":[{"language":"en","value":"author"}],"statementUri":"http://dev-faces.mpdl.mpg.de/imeji/statement/_HTF9UJTnH4SvZRr","typeUri":"http://imeji.org/terms/metadata#text","value":{"text":"Allen"}},{"labels":[{"language":"en","value":"location"}],"statementUri":"http://dev-faces.mpdl.mpg.de/imeji/statement/vxKbKv5mNxKjueid","typeUri":"http://imeji.org/terms/metadata#geolocation","value":{"latitude":48.1480062,"longitude":11.5767977,"name":"Amalienstraße 33, 80799 München, Germany"}},{"labels":[{"language":"en","value":"accuracy"}],"statementUri":"http://dev-faces.mpdl.mpg.de/imeji/statement/DCAGIb3A1pcu1Nmj","typeUri":"http://imeji.org/terms/metadata#number","value":{"number":10.0}},{"labels":[{"language":"en","value":"deviceID"}],"statementUri":"http://dev-faces.mpdl.mpg.de/imeji/statement/ntXvuGrRf_705f_","typeUri":"http://imeji.org/terms/metadata#text","value":{"text":"1"}},{"labels":[{"language":"en","value":"tags"}],"statementUri":"http://dev-faces.mpdl.mpg.de/imeji/statement/VwC_f_NcbS8vQhjV","typeUri":"http://imeji.org/terms/metadata#text","value":{"text":"test"}}]}




            RelativeLayout linearOne = (RelativeLayout) sw.getParent();
            TextView tv = (TextView) linearOne.findViewById(R.id.folder);

            String folder = tv.getText().toString();

            Toast.makeText(getApplicationContext()," you have selected  " + folder, Toast.LENGTH_LONG).show();


            // Perform actions when the switch is on
        } else {
            sw.setOnCheckedChangeListener(null);

            // Perform actions when the switch is off
        }






"\"metadata\":[{\"labels\":[{\"language\":\"en\",\"value\":\"title\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"20150916_141350.wav@Amalienstraße 33, 80799 München, Germany\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"author\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"Allen\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"location\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\", \"value\":{\"text\":\"20150916_141350.wav@Amalienstraße 33, 80799 München, Germany\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"accuracy\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"10.0\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"deviceID\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"1\"}},{\"labels\":[{\"language\":\"en\",\"value\":\"tags\"}],\"statementUri\":\"http://dev-faces.mpdl.mpg.de/imeji/statement/IJNOnHLthFNIYWMW\",\"typeUri\":\"http://imeji.org/terms/metadata#text\",\"value\":{\"text\":\"test\"}}] ";



    }*/
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
}


