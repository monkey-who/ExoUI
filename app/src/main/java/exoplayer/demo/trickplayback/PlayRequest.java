package exoplayer.demo.trickplayback;

import android.content.Intent;
import android.net.Uri;

/**
 * Created by mwho on 15/03/2017.
 */

public interface PlayRequest {
    void play(Uri uri);
    void play(Intent intent);
}
