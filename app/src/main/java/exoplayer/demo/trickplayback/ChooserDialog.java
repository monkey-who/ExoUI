package exoplayer.demo.trickplayback;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by mwho on 13/03/2017.
 */

final class ChooserDialog extends AlertDialog {

    ChooserDialog(Context context) {
        super(context);
    }

    private View mNaviPanel;
    private TextView mLocation;
    private ListView mListPanel;
    private enum State {MAIN, LOCAL_MEDIA, SAMPLES}
    private State mState = State.MAIN;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_main);
        mNaviPanel = findViewById(R.id.navi_panel);
        mListPanel = (ListView) findViewById(R.id.dialog_list);
        mLocation = (TextView) findViewById(R.id.location);
        initNaviButtons();

        mListPanel.setAdapter(mAdapter);
    }

    private LocalMediaCursorAdapter mAdapter;
    void setAdapter(LocalMediaCursorAdapter adapter) {
        mAdapter = adapter;
        mListPanel.setAdapter(mAdapter);
    }

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
        mListPanel.setVisibility(View.GONE);
        mNaviPanel.setVisibility(View.VISIBLE);
    }

    private void switchToLocal() {
        mState = State.LOCAL_MEDIA;
        mLocation.setVisibility(View.VISIBLE);
        mLocation.setText(R.string.navi_local);
        mListPanel.setVisibility(View.VISIBLE);
        mNaviPanel.setVisibility(View.GONE);
    }

    private void switchToSample() {
        mState = State.SAMPLES;
        mLocation.setVisibility(View.VISIBLE);
        mLocation.setText(R.string.navi_samples);
        mListPanel.setVisibility(View.VISIBLE);
        mNaviPanel.setVisibility(View.GONE);
    }

    @Override
    public void dismiss() {
        switchToHome();
        super.dismiss();
    }
}
