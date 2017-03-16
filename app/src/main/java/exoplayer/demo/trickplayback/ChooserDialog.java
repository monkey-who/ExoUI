package exoplayer.demo.trickplayback;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by mwho on 13/03/2017.
 */

final class ChooserDialog extends AlertDialog {
    public static final String TAG = "ChooserDialog";

    ChooserDialog(Context context) {
        super(context);
    }

    private View mNaviPanel;
    private TextView mLocation;
    private ListView mLocalList;
    private ExpandableListView mSampleList;
    private enum State {MAIN, LOCAL_MEDIA, SAMPLES}
    private State mState = State.MAIN;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_main);
        mNaviPanel = findViewById(R.id.navi_panel);
        mLocalList = (ListView) findViewById(R.id.local_list);
        mSampleList = (ExpandableListView) findViewById(R.id.sample_list);
        mLocation = (TextView) findViewById(R.id.location);
        initNaviButtons();

        mLocalList.setAdapter(mLocalAdapter);
    }

    private LocalMediaCursorAdapter mLocalAdapter;
    private SampleAdapter mSampleAdapter;
    private PlayRequest mCallback;
    void setAdapter(LocalMediaCursorAdapter adapter) {
        mLocalAdapter = adapter;
        mLocalList.setAdapter(mLocalAdapter);
    }

    void setCallback(PlayRequest callback) { mCallback = callback; }

    private void initNaviButtons() {
        View naviHome = findViewById(R.id.navi_home);
        View naviLocal = findViewById(R.id.navi_local);
        View naviSample = findViewById(R.id.navi_sample);

        naviHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mState != State.MAIN)
                    switchToHome();
            }
        });

        naviLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mState != State.LOCAL_MEDIA)
                    switchToLocal();
            }
        });

        naviSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mState != State.SAMPLES)
                    switchToSample();
            }
        });

    }

    private void switchToHome() {
        mState = State.MAIN;
        mLocation.setVisibility(View.GONE);
        mLocalList.setVisibility(View.GONE);
        mSampleList.setVisibility(View.GONE);
        mNaviPanel.setVisibility(View.VISIBLE);
    }

    private void switchToLocal() {
        mState = State.LOCAL_MEDIA;
        mLocation.setVisibility(View.VISIBLE);
        mLocation.setText(R.string.navi_local);
        mLocalList.setVisibility(View.VISIBLE);
        mSampleList.setVisibility(View.GONE);
        mNaviPanel.setVisibility(View.GONE);
    }

    private void switchToSample() {
        mState = State.SAMPLES;
        mLocation.setVisibility(View.VISIBLE);
        mLocation.setText(R.string.navi_samples);
        mLocalList.setVisibility(View.GONE);
        mSampleList.setVisibility(View.VISIBLE);
        mNaviPanel.setVisibility(View.GONE);

        if (mSampleAdapter == null)
            initSampleList();

        mSampleList.setAdapter(mSampleAdapter);
    }

    private void initSampleList() {
        ArrayList<String> uriList = new ArrayList<>();
        AssetManager assetManager = getContext().getAssets();
        try {
            for (String asset : assetManager.list("")) {
                if (asset.endsWith(".exolist.json")) {
                    uriList.add("asset:///" + asset);
                }
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
                    .show();
        }
        String[] uris = new String[uriList.size()];
        uriList.toArray(uris);
        Arrays.sort(uris);
        SampleListLoader loaderTask = new SampleListLoader();
        loaderTask.execute(uris);
    }

    @Override
    public void dismiss() {
        switchToHome();
        super.dismiss();
    }

    private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
        if (sawError) {
            Toast.makeText(getContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
                    .show();
        }
        mSampleAdapter = new SampleAdapter(getContext(), groups);
        mSampleList.setAdapter(mSampleAdapter);
        mSampleList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
                                        int childPosition, long id) {
                onSampleSelected(groups.get(groupPosition).samples.get(childPosition));
                return true;
            }
        });
    }

    private void onSampleSelected(Sample sample) {
        if (mCallback != null)
            mCallback.play(sample.buildIntent(this.getContext()));
    }

    private final class SampleListLoader extends AsyncTask<String, Void, List<SampleGroup>> {

        private boolean sawError;

        @Override
        protected List<SampleGroup> doInBackground(String... uris) {
            List<SampleGroup> result = new ArrayList<>();
            Context context = getContext();
            String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
            DataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
            for (String uri : uris) {
                DataSpec dataSpec = new DataSpec(Uri.parse(uri));
                InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
                try {
                    readSampleGroups(new JsonReader(new InputStreamReader(inputStream, "UTF-8")), result);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sample list: " + uri, e);
                    sawError = true;
                } finally {
                    Util.closeQuietly(dataSource);
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<SampleGroup> result) {
            onSampleGroups(result, sawError);
        }

        private void readSampleGroups(JsonReader reader, List<SampleGroup> groups) throws IOException {
            reader.beginArray();
            while (reader.hasNext()) {
                readSampleGroup(reader, groups);
            }
            reader.endArray();
        }

        private void readSampleGroup(JsonReader reader, List<SampleGroup> groups) throws IOException {
            String groupName = "";
            ArrayList<Sample> samples = new ArrayList<>();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        groupName = reader.nextString();
                        break;
                    case "samples":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            samples.add(readEntry(reader, false));
                        }
                        reader.endArray();
                        break;
                    case "_comment":
                        reader.nextString(); // Ignore.
                        break;
                    default:
                        throw new ParserException("Unsupported name: " + name);
                }
            }
            reader.endObject();

            SampleGroup group = getGroup(groupName, groups);
            group.samples.addAll(samples);
        }

        private Sample readEntry(JsonReader reader, boolean insidePlaylist) throws IOException {
            String sampleName = null;
            String uri = null;
            String extension = null;
            UUID drmUuid = null;
            String drmLicenseUrl = null;
            String[] drmKeyRequestProperties = null;
            boolean preferExtensionDecoders = false;
            ArrayList<UriSample> playlistSamples = null;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        sampleName = reader.nextString();
                        break;
                    case "uri":
                        uri = reader.nextString();
                        break;
                    case "extension":
                        extension = reader.nextString();
                        break;
                    case "drm_scheme":
                        Assertions.checkState(!insidePlaylist, "Invalid attribute on nested item: drm_scheme");
                        drmUuid = getDrmUuid(reader.nextString());
                        break;
                    case "drm_license_url":
                        Assertions.checkState(!insidePlaylist,
                                "Invalid attribute on nested item: drm_license_url");
                        drmLicenseUrl = reader.nextString();
                        break;
                    case "drm_key_request_properties":
                        Assertions.checkState(!insidePlaylist,
                                "Invalid attribute on nested item: drm_key_request_properties");
                        ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            drmKeyRequestPropertiesList.add(reader.nextName());
                            drmKeyRequestPropertiesList.add(reader.nextString());
                        }
                        reader.endObject();
                        drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
                        break;
                    case "prefer_extension_decoders":
                        Assertions.checkState(!insidePlaylist,
                                "Invalid attribute on nested item: prefer_extension_decoders");
                        preferExtensionDecoders = reader.nextBoolean();
                        break;
                    case "playlist":
                        Assertions.checkState(!insidePlaylist, "Invalid nesting of playlists");
                        playlistSamples = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            playlistSamples.add((UriSample) readEntry(reader, true));
                        }
                        reader.endArray();
                        break;
                    default:
                        throw new ParserException("Unsupported attribute name: " + name);
                }
            }
            reader.endObject();

            if (playlistSamples != null) {
                UriSample[] playlistSamplesArray = playlistSamples.toArray(
                        new UriSample[playlistSamples.size()]);
                return new PlaylistSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
                        preferExtensionDecoders, playlistSamplesArray);
            } else {
                return new UriSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
                        preferExtensionDecoders, uri, extension);
            }
        }

        private SampleGroup getGroup(String groupName, List<SampleGroup> groups) {
            for (int i = 0; i < groups.size(); i++) {
                if (Util.areEqual(groupName, groups.get(i).title)) {
                    return groups.get(i);
                }
            }
            SampleGroup group = new SampleGroup(groupName);
            groups.add(group);
            return group;
        }

        private UUID getDrmUuid(String typeString) throws ParserException {
            switch (typeString.toLowerCase()) {
                case "widevine":
                    return C.WIDEVINE_UUID;
                case "playready":
                    return C.PLAYREADY_UUID;
                default:
                    try {
                        return UUID.fromString(typeString);
                    } catch (RuntimeException e) {
                        throw new ParserException("Unsupported drm type: " + typeString);
                    }
            }
        }

    }

    private static final class SampleAdapter extends BaseExpandableListAdapter {

        private final Context context;
        private final List<SampleGroup> sampleGroups;

        SampleAdapter(Context context, List<SampleGroup> sampleGroups) {
            this.context = context;
            this.sampleGroups = sampleGroups;
        }

        @Override
        public Sample getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).samples.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent,
                        false);
            }
            ((TextView) view).setText(getChild(groupPosition, childPosition).name);
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return getGroup(groupPosition).samples.size();
        }

        @Override
        public SampleGroup getGroup(int groupPosition) {
            return sampleGroups.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1,
                        parent, false);
            }
            ((TextView) view).setText(getGroup(groupPosition).title);
            return view;
        }

        @Override
        public int getGroupCount() {
            return sampleGroups.size();
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

    private static final class SampleGroup {

        public final String title;
        public final List<Sample> samples;

        public SampleGroup(String title) {
            this.title = title;
            this.samples = new ArrayList<>();
        }

    }

    private abstract static class Sample {

        public final String name;
        public final boolean preferExtensionDecoders;
        public final UUID drmSchemeUuid;
        public final String drmLicenseUrl;
        public final String[] drmKeyRequestProperties;

        public Sample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
                      String[] drmKeyRequestProperties, boolean preferExtensionDecoders) {
            this.name = name;
            this.drmSchemeUuid = drmSchemeUuid;
            this.drmLicenseUrl = drmLicenseUrl;
            this.drmKeyRequestProperties = drmKeyRequestProperties;
            this.preferExtensionDecoders = preferExtensionDecoders;
        }

        public Intent buildIntent(Context context) {
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS, preferExtensionDecoders);
            if (drmSchemeUuid != null) {
                intent.putExtra(PlayerActivity.DRM_SCHEME_UUID_EXTRA, drmSchemeUuid.toString());
                intent.putExtra(PlayerActivity.DRM_LICENSE_URL, drmLicenseUrl);
                intent.putExtra(PlayerActivity.DRM_KEY_REQUEST_PROPERTIES, drmKeyRequestProperties);
            }
            return intent;
        }

    }

    private static final class UriSample extends Sample {

        public final String uri;
        public final String extension;

        public UriSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
                         String[] drmKeyRequestProperties, boolean preferExtensionDecoders, String uri,
                         String extension) {
            super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties, preferExtensionDecoders);
            this.uri = uri;
            this.extension = extension;
        }

        @Override
        public Intent buildIntent(Context context) {
            return super.buildIntent(context)
                    .setData(Uri.parse(uri))
                    .putExtra(PlayerActivity.EXTENSION_EXTRA, extension)
                    .setAction(PlayerActivity.ACTION_VIEW);
        }

    }

    private static final class PlaylistSample extends Sample {

        public final UriSample[] children;

        public PlaylistSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
                              String[] drmKeyRequestProperties, boolean preferExtensionDecoders,
                              UriSample... children) {
            super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties, preferExtensionDecoders);
            this.children = children;
        }

        @Override
        public Intent buildIntent(Context context) {
            String[] uris = new String[children.length];
            String[] extensions = new String[children.length];
            for (int i = 0; i < children.length; i++) {
                uris[i] = children[i].uri;
                extensions[i] = children[i].extension;
            }
            return super.buildIntent(context)
                    .putExtra(PlayerActivity.URI_LIST_EXTRA, uris)
                    .putExtra(PlayerActivity.EXTENSION_LIST_EXTRA, extensions)
                    .setAction(PlayerActivity.ACTION_VIEW_LIST);
        }

    }
}
