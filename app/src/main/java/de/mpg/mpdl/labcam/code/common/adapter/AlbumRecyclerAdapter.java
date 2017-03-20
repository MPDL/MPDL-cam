package de.mpg.mpdl.labcam.code.common.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.mpg.mpdl.labcam.code.activity.DetailActivity;
import de.mpg.mpdl.labcam.R;
import de.mpg.mpdl.labcam.code.activity.LocalImageActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.grantland.widget.AutofitHelper;

/**
 * Created by kiran on 22.10.15.
 */

public class AlbumRecyclerAdapter extends RecyclerView.Adapter<AlbumRecyclerAdapter.AlbumRecyclerViewHolder> {

    private final String LOG_TAG = AlbumRecyclerAdapter.class.getSimpleName();
    private Activity activity;

    // all albums
    private ArrayList<List<String[]>> galleryList;
    static ArrayList<String> itemPathList = new ArrayList<String>();
    Point size;

    // album positionSet
    public Set<Integer> albumPositionSet = new HashSet<>();

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public AlbumRecyclerAdapter(Activity activity) {
        this.activity = activity;
    }

    public AlbumRecyclerAdapter(Activity activity, ArrayList<List<String[]>> galleryList) {
        this.activity = activity;
        this.galleryList = galleryList;
    }

    @Override
    public AlbumRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.album_recycler_cell, parent, false);


        return new AlbumRecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AlbumRecyclerViewHolder holder, final int position) {
        //get display size
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        size = new Point();
        display.getSize(size);

        if (size.x > size.y) {
            holder.imageView.getLayoutParams().height = size.x / 3;
        } else {
            holder.itemView.getLayoutParams().height = size.y / 3;
        }

        final List<String[]> gallery = galleryList.get(position);

        // display first 6 photos in an album
        // if album size less than 6, display differently
        int sizeConstrain = 6;
        if (gallery.size() < 7) {
            sizeConstrain = gallery.size();
        }

        // 3 images a row
        int pixels;
        final float scale = activity.getResources().getDisplayMetrics().density;
        if (sizeConstrain < 4) {
            pixels = (int) (size.x / 3 + 80 * scale + 0.5f);
        } else {
            pixels = (int) (size.x * 2 / 3 + 80 * scale + 0.5f);
        }

        RelativeLayout.LayoutParams re_param = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, pixels);
        re_param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        holder.cell.setLayoutParams(re_param);

        ViewGroup.LayoutParams li_param = holder.Layout_layer_1.getLayoutParams();
        li_param.height = size.x / 3;
        holder.Layout_layer_1.setLayoutParams(li_param);
        ViewGroup.LayoutParams li_2_param = holder.Layout_layer_2.getLayoutParams();
        li_2_param.height = size.x / 3;
        holder.Layout_layer_2.setLayoutParams(li_2_param);

        // get current album

        final List<ImageView> imageViewList = new ArrayList<>();
        imageViewList.add(holder.imageView);
        imageViewList.add(holder.imageView_2);
        imageViewList.add(holder.imageView_3);
        imageViewList.add(holder.imageView_4);
        imageViewList.add(holder.imageView_5);
        imageViewList.add(holder.imageView_6);

        List<TextView> textViewList = new ArrayList<>();
        textViewList.add(holder.textView_num_1);
        textViewList.add(holder.textView_num_2);
        textViewList.add(holder.textView_num_3);
        textViewList.add(holder.textView_num_4);
        textViewList.add(holder.textView_num_5);
        textViewList.add(holder.textView_num_6);

        // first set everything invisible
        for (int i = 0; i <6 ; i++ ){
            textViewList.get(i).setVisibility(View.INVISIBLE);
            imageViewList.get(i).setVisibility(View.INVISIBLE);
        }

        // to the album view
        for (int i = 0; i < sizeConstrain; i++) {
            imageViewList.get(i).setVisibility(View.VISIBLE);
            if (i == 5 && gallery.size() != 6) { // if <=6 photo, no shadow on 6th photo; if >6, remain photos number begins with 2
                AutofitHelper.create(textViewList.get(i));
                textViewList.get(i).setVisibility(View.VISIBLE);
                textViewList.get(i).setText(String.valueOf(gallery.size()-5) + " >");
                textViewList.get(i).setBackgroundResource(R.color.black_shadow);
                imageViewList.get(i).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(getAlbumPath(gallery.get(0)[1])==null)
                            return;
                        Intent galleryImagesIntent = new Intent(activity, LocalImageActivity.class);
                        galleryImagesIntent.putExtra("galleryTitle", getAlbumPath(gallery.get(0)[1]));
                        activity.startActivity(galleryImagesIntent);
                    }
                });
            } else {
                textViewList.get(i).setText("");
                textViewList.get(i).setVisibility(View.INVISIBLE);
                imageViewList.get(i).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // go to photo detail
                        boolean isLocalImage = true;
                        int image_position = 0;
                        Intent showDetailIntent = new Intent(activity, DetailActivity.class);
                        itemPathList.clear();
                        for (String[] imageStr : gallery) {
                            itemPathList.add(imageStr[1]);
                        }

                        switch (view.getId()){
                            case R.id.album_pic_1:
                                image_position = 0;
                                break;
                            case R.id.album_pic_2:
                                image_position = 1;
                                break;
                            case R.id.album_pic_3:
                                image_position = 2;
                                break;
                            case R.id.album_pic_4:
                                image_position = 3;
                                break;
                            case R.id.album_pic_5:
                                image_position = 4;
                                break;
                        }

                        showDetailIntent.putStringArrayListExtra("itemPathList", itemPathList);
                        Log.e(LOG_TAG, itemPathList.size()+"");
                        showDetailIntent.putExtra("positionInList",image_position);
                        showDetailIntent.putExtra("isLocalImage", isLocalImage);
                        activity.startActivity(showDetailIntent);
                    }
                });

            }
        }

        for (int i = 0; i < sizeConstrain; i++) {
            String galleryPath = gallery.get(i)[1];

            File imageFile = new File(galleryPath);

            Uri uri = Uri.fromFile(imageFile);

            if (imageFile.exists()) {
                Picasso.with(activity)
                        .load(uri)
                        .resize(size.x / 3, size.x / 3)
                        .centerCrop()
                        .into(imageViewList.get(i));
            }
        }


        if (gallery.size() > 0) {
            holder.title.setText(gallery.get(0)[0]);
            holder.title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(getAlbumPath(gallery.get(0)[1])==null)
                        return;
                    Intent galleryImagesIntent = new Intent(activity, LocalImageActivity.class);
                    galleryImagesIntent.putExtra("galleryTitle", getAlbumPath(gallery.get(0)[1]));
                    activity.startActivity(galleryImagesIntent);
                }
            });
        }

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(v, position);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemClickListener.onItemLongClick(v, position);
                    return false;
                }
            });
        }

        // checkMark
        if (albumPositionSet.contains(position)) {
            holder.checkMark.setVisibility(View.VISIBLE);
        } else {
            holder.checkMark.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return galleryList.size();
    }

    public static class AlbumRecyclerViewHolder extends RecyclerView.ViewHolder {

        protected ImageView imageView;
        protected ImageView imageView_2;
        protected ImageView imageView_3;
        protected ImageView imageView_4;
        protected ImageView imageView_5;
        protected ImageView imageView_6;

        protected TextView textView_num_1;
        protected TextView textView_num_2;
        protected TextView textView_num_3;
        protected TextView textView_num_4;
        protected TextView textView_num_5;
        protected TextView textView_num_6;

        protected RelativeLayout cell;
        protected LinearLayout Layout_layer_1;
        protected LinearLayout Layout_layer_2;


        protected ImageView checkMark;
        //        protected TextView number;
        protected TextView title;
//        protected TextView date;


        public AlbumRecyclerViewHolder(View itemView) {
            super(itemView);
            cell = (RelativeLayout) itemView.findViewById(R.id.relOne);
            Layout_layer_1 = (LinearLayout) itemView.findViewById(R.id.layout_first_layer_text);
            Layout_layer_2 = (LinearLayout) itemView.findViewById(R.id.layout_second_layer_text);

            imageView = (ImageView) itemView.findViewById(R.id.album_pic_1);
            imageView_2 = (ImageView) itemView.findViewById(R.id.album_pic_2);
            imageView_3 = (ImageView) itemView.findViewById(R.id.album_pic_3);
            imageView_4 = (ImageView) itemView.findViewById(R.id.album_pic_4);
            imageView_5 = (ImageView) itemView.findViewById(R.id.album_pic_5);
            imageView_6 = (ImageView) itemView.findViewById(R.id.album_pic_6);

            textView_num_1 = (TextView) itemView.findViewById(R.id.album_tv_1);
            textView_num_2 = (TextView) itemView.findViewById(R.id.album_tv_2);
            textView_num_3 = (TextView) itemView.findViewById(R.id.album_tv_3);
            textView_num_4 = (TextView) itemView.findViewById(R.id.album_tv_4);
            textView_num_5 = (TextView) itemView.findViewById(R.id.album_tv_5);
            textView_num_6 = (TextView) itemView.findViewById(R.id.album_tv_6);

            checkMark = (ImageView) itemView.findViewById(R.id.album_check_mark);
//            number = (TextView) itemView.findViewById(R.id.list_item_num);
            title = (TextView) itemView.findViewById(R.id.tv_album_title);
//            date = (TextView) itemView.findViewById(R.id.list_item_date);


        }
    }

    public void setPositionSet(Set<Integer> positionSet) {
        this.albumPositionSet = positionSet;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    private String getAlbumPath(String str) {
        if(null!=str&&str.length()>0)
        {
            int endIndex = str.lastIndexOf("/");
            if (endIndex != -1) {
                String newstr = str.substring(0, endIndex); // not forgot to put check if(endIndex != -1)
                return newstr;
            }else return null;
        }else return null;

    }
}