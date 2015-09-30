package example.com.mpdlcamera;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import example.com.mpdlcamera.Folder.MainActivity;
import example.com.mpdlcamera.Model.DataItem;
import example.com.mpdlcamera.Model.MetaData;
import example.com.mpdlcamera.Model.User;
import example.com.mpdlcamera.Utils.DeviceStatus;

/**
 * Created by kiran on 29.09.15.
 */
public class LatestImage {

private Context context;

    MainActivity just;

    public int maxId;
    private User user = new User();

    public LatestImage() {

    }
    public LatestImage(Context context) {
        this.context = context;
    }

    private String collectionID = DeviceStatus.collectionID;

    MainActivity app;
    private int latestId;

    public int getId() {


        String[] columns = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.ORIENTATION};
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(uri, columns, null, null, MediaStore.Images.Media._ID + " DESC");

        if (!cursor.moveToFirst()) {
            return -1;
        }

        latestId = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
        setMaxIdFromDatabase();
        int maxId = getMaxId();

        String orientation = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION));

        if (orientation == null) {
            setMaxId(latestId);
            return -1;
        }

        setMaxId(latestId);

        return latestId;
    }

        public DataItem getLatestItem() {

        DataItem item = null;
        MetaData meta = new MetaData();
        user.setCompleteName("Kiran");
        user.save();
        String columns[] = new String[]{ MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.MINI_THUMB_MAGIC };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        while (true) {

            Uri image = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, latestId);
            Cursor cursor = context.getContentResolver().query(image,columns,null,null,null);


            if(cursor.moveToFirst()) {


                    String folder = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));

                    if(preferences.contains(folder) && preferences.getString("uploadStatus","").equalsIgnoreCase("back")) {

                        String state = preferences.getString(folder,"");
                        if(state.equalsIgnoreCase("On")) {

                            item = new DataItem();
                            item.setFilename(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)));
                            item.setLocalPath(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
                            meta.setTags(null);
                            meta.setAddress("blabla");
                            meta.setTitle(item.getFilename());
                            meta.setCreator(user.getCompleteName());

                            item.setCollectionId(collectionID);

                            item.setMetadata(meta);

                            item.setCreatedBy(user);

                            meta.save();
                            item.save();
                            break;

                        }
                        else return item;


                    }
                    else
                        return item;

            }


        }

            return item;
    }


    private void setMaxIdFromDatabase()
    {
        String columns[] = new String[]{ MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MINI_THUMB_MAGIC };
        Cursor cursor    = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, MediaStore.Images.Media._ID + " DESC");
        maxId            = cursor.moveToFirst() ? cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID)) : -1;
    }

    public void setMaxId(int maxId)
    {
        this.maxId = maxId;
    }

    /**
     * Get highest image id
     *
     * @return Highest id
     */
    public int getMaxId()
    {
        return maxId;
    }

}