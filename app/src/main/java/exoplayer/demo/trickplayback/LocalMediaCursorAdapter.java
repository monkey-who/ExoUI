package exoplayer.demo.trickplayback;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by mwho on 13/03/2017.
 */

public class LocalMediaCursorAdapter extends CursorAdapter {

    public static final String TAG = "LocalMediaAdapter";
    private ContentResolver mResolver;
    private PlayRequest mCallback;
    public LocalMediaCursorAdapter(Context context, Cursor c, int flags, PlayRequest callback) {
        super(context, c, flags);
        mResolver = context.getContentResolver();
        mCallback = callback;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_local_media, parent, false);
    }

    private View mLastView;
    @Override
    public void bindView(final View view, Context context, Cursor cursor) {
        final ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
        TextView name = (TextView) view.findViewById(R.id.video_name);
        TextView resolution = (TextView) view.findViewById(R.id.video_resolution);
        TextView duration = (TextView) view.findViewById(R.id.video_duration);

        int nameIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME);
        int resIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.RESOLUTION);
        int durationIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION);
        int videoIdIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID);

        name.setText(cursor.getString(nameIdx));
        resolution.setText(cursor.getString(resIdx));
        duration.setText(parseDuration(cursor.getLong(durationIdx)));

        final int videoId = cursor.getInt(videoIdIdx);

        if (view != mLastView) {
            mLastView = view;
            new ThumbnailTask().execute(thumbnail, videoId);
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI , Integer.toString(videoId));
                mCallback.play(uri);
            }
        });
    }

    private String parseDuration(long durationInMs) {
        String time;
        durationInMs = durationInMs / 1000; /*in second*/
        time = ":" + durationInMs % 60;
        durationInMs = durationInMs / 60; /*in minute*/
        time = ":" + durationInMs % 60 + time;
        durationInMs = durationInMs / 60; /*in hour*/
        time = durationInMs + time;

        return time;
    }

    private class ThumbnailTask extends AsyncTask {

        private ImageView imageView;
        @Override
        protected Bitmap doInBackground(final Object[] params) {
            try {
                imageView = (ImageView) params[0];
                int videoID = (int) params[1];
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inDither = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                return MediaStore.Video.Thumbnails.getThumbnail(mResolver, videoID,
                        MediaStore.Video.Thumbnails.MICRO_KIND, options);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap((Bitmap) bitmap);
            }
        }
    }
}
