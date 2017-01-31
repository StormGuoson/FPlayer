package com.liu.finalplayer.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liu.finalplayer.R;
import com.liu.finalplayer.database.DbContent;
import com.liu.finalplayer.database.FileDatabase;
import com.liu.finalplayer.widget.AutoHiddenTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.vov.vitamio.MediaFormat;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.VideoView;


/**
 * Created by StormGuoson on 2017/1/11.
 */

public class AtyMainVideo extends AppCompatActivity implements MediaPlayer.OnPreparedListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener, MediaPlayer.OnTimedTextListener {
    public static final String TAG = "VideoView";
    VideoView videoView;                                                                            //主视频
    View layout_ctrl, volume_brightness;                                                            //控制，音量·亮度布局
    TextView tvDuration, tvCurrent, tvTitle, tvDate, tvZoom, tvSwipeCurrent, tvSwipeChanged,
            tvBuffered, tvSpeed, tvEmSub;                                                                     //总时长，播放时长，标题，系统时间，视频比例提示，手势播放时长，手势变化时长，缓冲，网速，解码
    ImageView ivPlay, ivNext, ivPrevious, ivRotate, ivVolAndBriSwitch, ivQuit, ivZoom, ivAudio,
            ivMenu;                                                                                 //播放\暂停,下一集，上一集，旋转屏幕，亮度和声音交换，退出，视频比例变换，音轨
    ProgressBar pb_volume_bright;                                                                   //声音\亮度
    SeekBar sbTo;                                                                                   //定位
    SimpleDateFormat format;                                                                        //格式化时间
    AudioManager audioManager;                                                                      //音频管理
    GestureDetector detector;                                                                       //手势
    FileDatabase database;
    SQLiteDatabase write;
    AlertDialog.Builder audio, emSub;
    Dialog audioDialog, emSubDialog;                                                                 //音轨dialog,字幕dialog
    boolean lToR = true;                                                                            //左右滑动
    boolean uTod = true;                                                                            //上下滑动
    private int width, height;                                                                      //屏幕宽高
    private float maxVolume;                                                                        //最大音量
    private int mLayout = VideoView.VIDEO_LAYOUT_ZOOM;                                              //视频比例
    private int mPosition;                                                                          //当前点
    private String filePath, fileUrl;                                                               //视频路径
    boolean isOver;                                                                                 //结束
    int startVolume;                                                                                //滑动前音量
    long startDuration;                                                                             //滑动前时间点
    long current;                                                                                   //滑动时时间点

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: ", null);
        setContentView(R.layout.lay_video);
        init();
        initButton();
        initVideo();
    }

    /**
     * 初始化控件
     */
    void init() {
        width = getWindowManager().getDefaultDisplay().getWidth();
        height = getWindowManager().getDefaultDisplay().getHeight();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        detector = new GestureDetector(this, new MyGesture());
        handler.sendEmptyMessageDelayed(2, 2000);
        database = new FileDatabase(this, DbContent.DATABASE_NAME, null, 1);
        write = database.getWritableDatabase();
    }

    void initButton() {
        pb_volume_bright = (ProgressBar) findViewById(R.id.pb_volume_bright);
        layout_ctrl = findViewById(R.id.layout_controller);
        volume_brightness = findViewById(R.id.operation_volume_brightness);
        tvDuration = (TextView) findViewById(R.id.tv_video_length);
        tvCurrent = (TextView) findViewById(R.id.tv_video_current);
        tvDate = (TextView) findViewById(R.id.tv_video_currentDate);
        tvTitle = (TextView) findViewById(R.id.tv_video_title);
        tvZoom = (AutoHiddenTextView) findViewById(R.id.tv_zoom_hidden);
        tvSwipeChanged = (TextView) findViewById(R.id.tv_video_swipeChanged);
        tvSwipeCurrent = (TextView) findViewById(R.id.tv_video_swipeCurrent);
        tvBuffered = (TextView) findViewById(R.id.tvBuffered);
        tvSpeed = (TextView) findViewById(R.id.tvSpeed);
        tvEmSub = (TextView) findViewById(R.id.tv_video_emSub);
        ivPlay = (ImageView) findViewById(R.id.iv_video_play);
        ivNext = (ImageView) findViewById(R.id.iv_video_next);
        ivQuit = (ImageView) findViewById(R.id.iv_video_quit);
        ivZoom = (ImageView) findViewById(R.id.iv_video_zoom);
        ivAudio = (ImageView) findViewById(R.id.iv_video_audio);
        ivPrevious = (ImageView) findViewById(R.id.iv_video_previous);
        ivMenu = (ImageView) findViewById(R.id.iv_video_menu);
        ivRotate = (ImageView) findViewById(R.id.iv_video_rotate);
        ivVolAndBriSwitch = (ImageView) findViewById(R.id.iv_volAndBri_bg);
        sbTo = (SeekBar) findViewById(R.id.seekBar);


        ivPlay.setOnClickListener(this);
        ivNext.setOnClickListener(this);
        ivPrevious.setOnClickListener(this);
        ivRotate.setOnClickListener(this);
        ivQuit.setOnClickListener(this);
        ivZoom.setOnClickListener(this);
        ivAudio.setOnClickListener(this);
        ivMenu.setOnClickListener(this);
    }

    void initVideo() {
        filePath = getIntent().getStringExtra("video");
        fileUrl = getIntent().getStringExtra("netVideo");
        videoView = (VideoView) findViewById(R.id.mainView);
        if (filePath != null) {
            videoView.setVideoPath(filePath);
            mPosition = getIntent().getIntExtra("watch", 0);
            tvTitle.setText(getIntent().getStringExtra("title"));
        } else if (fileUrl != null) {
            videoView.setVideoURI(Uri.parse(fileUrl));
            videoView.setOnBufferingUpdateListener(this);
            videoView.setOnInfoListener(this);
        } else {
            String string = getIntent().getDataString();
            videoView.setVideoPath(string);
//            File file = new File(string);
//            tvTitle.setText(file.getName().toString());
        }

        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnTimedTextListener(this);
        if (filePath != null)
            recentVideo();
    }

    /**
     * 视频长度
     */
    String getVideoLength(long length) {
        long duration = length / 1000;
        long h = duration / 3600;
        long m = duration % 3600 / 60;
        long s = duration % 3600 % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    String getVideoChanged(long l) {
        if (l >= 0) {
            long duration = l / 1000;
            long h = duration / 3600;
            long m = duration % 3600 / 60;
            long s = duration % 3600 % 60;
            return h > 0 ? String.format("+%d:%02d:%02d", h, m, s) : String.format("+%02d:%02d", m, s);
        } else {
            long duration = -l / 1000;
            long h = duration / 3600;
            long m = duration % 3600 / 60;
            long s = duration % 3600 % 60;
            return h > 0 ? String.format("-%d:%02d:%02d", h, m, s) : String.format("-%02d:%02d", m, s);
        }
    }


    /**
     * 屏幕旋转
     */
    void setScreenOrientation() {
        int h = videoView.getVideoHeight();
        int w = videoView.getVideoWidth();
        if (w > h) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }

    /**
     * 获取系统时间
     */
    void getSystemDate() {
        if (format == null)
            format = new SimpleDateFormat("HH:mm", Locale.CHINA);
        tvDate.setText(format.format(new Date()));
    }

    /**
     * 实时更新播放进程
     */
    void thread_currentDuration() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    try {
                        sleep(1000);
                        handler.sendEmptyMessage(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * 最后一次播放
     */
    void recentVideo() {
        String file;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbContent.LAST, 0);
        ContentValues contentValues1 = new ContentValues();
        contentValues1.put(DbContent.LAST, 1);
        Cursor cursor = write.query(DbContent.TABLE_NAME, new String[]{DbContent.FILE}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            file = cursor.getString(cursor.getColumnIndex(DbContent.FILE));
            if (!file.equals(filePath))
                write.update(DbContent.TABLE_NAME, contentValues, DbContent.FILE + "=?", new String[]{file});
            else
                write.update(DbContent.TABLE_NAME, contentValues1, DbContent.FILE + "=?", new String[]{file});
        }
        cursor.close();
        contentValues.clear();
        contentValues1.clear();
    }

    /**
     * 进度条
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch (seekBar.getId()) {
            case R.id.seekBar:
                if (b) {
                    tvSwipeChanged.setVisibility(View.VISIBLE);
                    handler.removeMessages(2);
                    videoView.seekTo(i);
                    current = videoView.getCurrentPosition();
                    tvSwipeCurrent.setText(getVideoLength(current));
                    tvSwipeChanged.setText(getVideoChanged(current - startDuration));
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.seekBar:
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                startDuration = videoView.getCurrentPosition();
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                tvSwipeCurrent.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.seekBar:
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, startVolume, 0);
                tvSwipeCurrent.setText(getVideoLength(current));
                tvSwipeChanged.setText(getVideoChanged(current - startDuration));
                handler.sendEmptyMessageDelayed(2, 2000);
                tvSwipeCurrent.setVisibility(View.GONE);
                tvSwipeChanged.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        handler.removeMessages(2);
        switch (view.getId()) {
            case R.id.iv_video_play:
                if (videoView.isPlaying()) {
                    videoView.pause();
                    ivPlay.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    videoView.start();
                    ivPlay.setImageResource(android.R.drawable.ic_media_pause);
                }
                break;
            case R.id.iv_video_rotate:
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case R.id.iv_video_quit:
                finish();
                break;
            case R.id.iv_video_zoom:
                switchLayout();
                break;
            case R.id.iv_video_next:
                break;
            case R.id.iv_video_previous:
                break;
            case R.id.iv_video_audio:
                if (audio == null)
                    createAudioDialog();
                audioDialog.show();
                videoView.pause();
                break;
            case R.id.iv_video_menu:
                showPopMenu(view);
                break;
        }
        handler.sendEmptyMessageDelayed(2, 2000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                showPopMenu(ivMenu);
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 选项菜单
     */
    void showPopMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.video_menu, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.mu_video_emSub:
                        if (emSub == null)
                            createEmSubDialog();
                        if (emSubDialog != null) {
                            emSubDialog.show();
                            videoView.pause();
                        } else
                            Toast.makeText(AtyMainVideo.this, R.string.noEmSub, Toast.LENGTH_SHORT).show();
                        break;
                }
                return false;
            }
        });
    }

    /**
     * 视频尺寸
     */
    void switchLayout() {
        switch (mLayout) {
            case 0:
                ++mLayout;
                tvZoom.setText("适应屏幕");
                break;
            case 1:
                ++mLayout;
                tvZoom.setText("拉伸");
                break;
            case 2:
                ++mLayout;
                tvZoom.setText("剪切");
                break;
            case 3:
                mLayout = 0;
                tvZoom.setText("原始大小");
                break;
        }
        ((AutoHiddenTextView) tvZoom).start();
        videoView.setVideoLayout(mLayout, 0);
    }

    /**
     * 切换音轨
     */
    void createAudioDialog() {
        final int old = videoView.getAudioTrack();
        SparseArray<MediaFormat> array = videoView.getAudioTrackMap("utf-8");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            list.add("音轨 #" + (i + 1));
        }
        audio = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_audio_track, null);
        audio.setView(view);
        ListView listView = (ListView) view.findViewById(R.id.lv_audioTrack);
        ArrayAdapter<String> audioTrackAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(audioTrackAdapter);
        audioDialog = audio.create();
        WindowManager.LayoutParams params = audioDialog.getWindow().getAttributes();
        params.alpha = 0.93f;
        audioDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                videoView.start();
            }
        });
        audioDialog.getWindow().setAttributes(params);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int index = i + 1;
                if (index == 1)
                    videoView.setAudioTrack(old);
                else
                    videoView.setAudioTrack(index);
                audioDialog.dismiss();
            }
        });
    }

    /**
     * 切换内嵌字幕
     */
    void createEmSubDialog() {
        final SparseArray<MediaFormat> array = videoView.getSubTrackMap("utf-8");
        if (array.size() > 0) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < array.size(); i++)
                list.add("字幕 #" + (i + 1));
            emSub = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_em_sub, null);
            emSub.setView(view);
            ListView listView = (ListView) view.findViewById(R.id.lv_emSub);
            ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
            listView.setAdapter(subAdapter);
            emSubDialog = emSub.create();
            WindowManager.LayoutParams params = emSubDialog.getWindow().getAttributes();
            params.alpha = 0.93f;
            emSubDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    videoView.start();
                }
            });
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    int index = i + 1;
                    videoView.setTimedTextShown(true);
                    videoView.setTimedTextEncoding("utf-8");
                    emSubDialog.dismiss();
                }
            });
        }
    }

    /**
     * 手势
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (detector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startDuration = videoView.getCurrentPosition();
                current = startDuration;
                break;

            case MotionEvent.ACTION_UP:

                lToR = true;
                uTod = true;
                volume_brightness.setVisibility(View.GONE);
                handler.sendEmptyMessage(3);
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 字幕
     */
    @Override
    public void onTimedText(String text) {
    }

    @Override
    public void onTimedTextUpdate(byte[] pixels, int width, int height) {
    }

    private class MyGesture extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (videoView.isPlaying()) {
                videoView.pause();
                ivPlay.setImageResource(android.R.drawable.ic_media_play);
            } else {
                videoView.start();
                ivPlay.setImageResource(android.R.drawable.ic_media_pause);
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            getSystemDate();
            handler.removeMessages(2);
            if (layout_ctrl.getVisibility() == View.VISIBLE)
                layout_ctrl.setVisibility(View.GONE);
            else layout_ctrl.setVisibility(View.VISIBLE);
            handler.sendEmptyMessageDelayed(2, 2000);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float x1 = e1.getX(), y1 = e1.getY();
            if (Math.abs(distanceY) > Math.abs(distanceX) + 2) {
                if (uTod) {
                    lToR = false;
                    if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        if (x1 > width / 2 && y1 > height / 5) {
                            volumeSlide(distanceY);
                        } else if (y1 > height / 5) {
                            brightnessSlide(distanceY);
                        }
                    } else {
                        if (x1 > height / 2 && y1 > height / 5) {
                            volumeSlide(distanceY);
                        } else if (y1 > height / 5) {
                            brightnessSlide(distanceY);
                        }
                    }
                }
            } else if (Math.abs(distanceX) > Math.abs(distanceY) + 2) {
                if (lToR) {
                    uTod = false;
                    forwardOrBack(-distanceX);
                    return false;
                }
            }
            return true;
        }

        void volumeSlide(float per) {
            volume_brightness.setVisibility(View.VISIBLE);
            pb_volume_bright.setMax((int) maxVolume * 100);
            ivVolAndBriSwitch.setImageResource(R.drawable.video_volume_bg);
            float currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            currentVol = currentVol + Math.round(per / 25);
            if (currentVol > maxVolume)
                currentVol = maxVolume;
            else if (currentVol < 0)
                currentVol = 0;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) currentVol, 0);
            pb_volume_bright.setProgress((int) (currentVol * 100f));
        }

        void brightnessSlide(float per) {
            volume_brightness.setVisibility(View.VISIBLE);
            pb_volume_bright.setMax(100);
            ivVolAndBriSwitch.setImageResource(R.drawable.video_brightness_bg);
            float currentBright = getWindow().getAttributes().screenBrightness;
            currentBright = (float) (currentBright + per / 250.0);
            if (currentBright > 1f)
                currentBright = 1f;
            else if (currentBright < 0)
                currentBright = 0f;
            WindowManager.LayoutParams lpa = getWindow().getAttributes();
            lpa.screenBrightness = currentBright;
            getWindow().setAttributes(lpa);
            pb_volume_bright.setProgress((int) (currentBright * 100f));
        }

        void forwardOrBack(float f) {
            tvSwipeCurrent.setVisibility(View.VISIBLE);
            tvSwipeChanged.setVisibility(View.VISIBLE);
            current = (long) (current + 500 * Math.signum(f));
            videoView.seekTo(current);
            tvSwipeCurrent.setText(getVideoLength(current));
            tvSwipeChanged.setText(getVideoChanged(current - startDuration));
        }

    }

    /**
     * 缓冲更新
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        tvBuffered.setText(getString(R.string.hasBuffered) + percent + "%");
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                tvBuffered.setVisibility(View.VISIBLE);
                tvSpeed.setVisibility(View.VISIBLE);
                mp.pause();
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                tvBuffered.setVisibility(View.GONE);
                tvSpeed.setVisibility(View.GONE);
                mp.start();
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                tvSpeed.setText(getString(R.string.netSpeed) + extra + "kb/s");
        }
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        sbTo.setMax((int) videoView.getDuration());
        sbTo.setProgress(mPosition);
        videoView.seekTo(mPosition);
        sbTo.setOnSeekBarChangeListener(this);
        tvDuration.setText(getVideoLength(videoView.getDuration()));
        thread_currentDuration();
        videoView.start();
        setScreenOrientation();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isOver = true;
        videoView.stopPlayback();
        ContentValues values = new ContentValues();
        values.put(DbContent.DataWATCH, 0);
        values.put(DbContent.WATCH, getString(R.string.watchOver));
        write.update(DbContent.TABLE_NAME, values, DbContent.FILE + "=?", new String[]{filePath});
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPosition = (int) videoView.getCurrentPosition();
        if (!isOver) {
            ContentValues values = new ContentValues();
            values.put(DbContent.DataWATCH, mPosition);
            values.put(DbContent.WATCH, getVideoLength(mPosition));
            write.update(DbContent.TABLE_NAME, values, DbContent.FILE + "=?", new String[]{filePath});
        }
        if (videoView.isPlaying())
            videoView.pause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        videoView.seekTo(mPosition);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.stopPlayback();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    tvCurrent.setText(getVideoLength(videoView.getCurrentPosition()));
                    sbTo.setProgress((int) videoView.getCurrentPosition());
                    break;
                case 2:
                    layout_ctrl.setVisibility(View.GONE);
                    break;
                case 3:
                    tvSwipeCurrent.setVisibility(View.GONE);
                    tvSwipeChanged.setVisibility(View.GONE);
            }
        }
    };
}
