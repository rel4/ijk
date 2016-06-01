package tv.danmaku.ijk.media.sample.activities;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.sample.R;
import tv.danmaku.ijk.media.sample.widget.media.IjkVideoView;
import tv.danmaku.ijk.media.sample.widget.media.MediaController;
import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IjkMediaPlayer.loadLibrariesOnce(null);
        MediaController mediaController = new MediaController(this);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        IjkVideoView vv = (IjkVideoView) findViewById(R.id.video_view);
        vv.setMediaController(mediaController);
        vv.setVideoPath("http://f2.immiao.com/Upload/video/2016-04-14/b0e4cfd2a638b68476fab2d113ddeb13.mp4");
        vv.start();
    }
}
