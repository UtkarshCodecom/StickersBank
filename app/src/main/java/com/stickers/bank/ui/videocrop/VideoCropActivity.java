package com.stickers.bank.ui.videocrop;

import static com.arthenica.ffmpegkit.ReturnCode.isSuccess;
import static com.arthenica.ffmpegkit.ReturnCode.isCancel;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.Statistics;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.crystal.crystalrangeseekbar.utils.LogMessage;
import com.crystal.crystalrangeseekbar.utils.TrimType;
import com.crystal.crystalrangeseekbar.utils.TrimmerUtils;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.crystal.crystalrangeseekbar.widgets.CrystalSeekbar;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.R;
import com.stickers.bank.data.common.Constants;
import com.stickers.bank.ui.activity.FeaturedLanguageListActivity;
import com.stickers.bank.ui.videocrop.cropview.window.CropVideoView;
import com.stickers.bank.ui.videocrop.player.VideoPlayer;
import com.stickers.bank.ui.videocrop.view.ProgressView;
import com.stickers.bank.ui.videocrop.view.VideoSliceSeekBarH;
import com.stickers.bank.utils.ParamArgus;

import java.io.File;
import java.math.BigDecimal;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.Executors;


public class VideoCropActivity extends AppCompatActivity implements VideoPlayer.OnProgressUpdateListener, VideoSliceSeekBarH.SeekBarChangeListener {

    private static final String VIDEO_CROP_INPUT_PATH = "VIDEO_CROP_INPUT_PATH";
    private static final String VIDEO_CROP_OUTPUT_PATH = "VIDEO_CROP_OUTPUT_PATH";
    private static final String VIDEO_CROP_OUTPUT_NAME = "video_crop_output_name";
    private static final String VIDEO_CROP_IDENTIFIER = "video_crop_identifier";

    private static final int STORAGE_REQUEST = 100;
    private static final String TAG = VideoCropActivity.class.getSimpleName();

    private VideoPlayer mVideoPlayer;
    private StringBuilder formatBuilder;
    private Formatter formatter;

    private AppCompatImageView mIvPlay;
    private AppCompatImageView mIvDone;
    private CropVideoView mCropVideoView;
    private TextView mTvCropProgress;
    private ProgressView mProgressBar;

    private String inputPath;
    private String outputPath;
    private String name;
    private String identifier;

    private boolean isVideoPlaying = false;

    private Statistics statistics;
    long durationCrop;


    //Start
    private CrystalRangeSeekbar seekbar;
    private TextView txtStartDuration, txtEndDuration, txt_total;

    private int trimType;
    private boolean hidePlayerSeek, isAccurateCut, showFileLocationAlert;
    private long minFromGap, maxToGap;
    private long totalDuration;
    private ImageView[] imageViews;
    private long lastMaxValue = 0;
    private long lastMinValue = 0;
    private boolean isValidVideo = true, isVideoEnded;
    private android.os.Handler seekHandler;
    private long currentDuration;

    public static Intent createIntent(Context context, String inputPath, String outputPath, String name, String identifier) {
        Intent intent = new Intent(context, VideoCropActivity.class);
        intent.putExtra(VIDEO_CROP_INPUT_PATH, inputPath);
        intent.putExtra(VIDEO_CROP_OUTPUT_PATH, outputPath);
        intent.putExtra(VIDEO_CROP_OUTPUT_NAME, name);
        intent.putExtra(VIDEO_CROP_IDENTIFIER, identifier);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        inputPath = getIntent().getStringExtra(VIDEO_CROP_INPUT_PATH);
        outputPath = getIntent().getStringExtra(VIDEO_CROP_OUTPUT_PATH);
        name = getIntent().getStringExtra(VIDEO_CROP_OUTPUT_NAME);
        identifier = getIntent().getStringExtra(VIDEO_CROP_IDENTIFIER);

        if (TextUtils.isEmpty(inputPath) || TextUtils.isEmpty(outputPath)) {
            Toast.makeText(this, "input and output paths must be valid and not null", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }

        findViews();
        initListeners();
        enableStatisticsCallback();

        requestStoragePermission();


        //Start
        setDataInView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case STORAGE_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPlayer(inputPath);
                } else {
                    Toast.makeText(this, "You must grant a write storage permission to use this functionality", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isVideoPlaying) {
            mVideoPlayer.play(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mVideoPlayer.play(false);
    }

    @Override
    public void onDestroy() {
        mVideoPlayer.release();
        stopRepeatingTask();
        FFmpegKit.cancel();
        super.onDestroy();
    }

    @Override
    public void onFirstTimeUpdate(long duration, long currentPosition) {
        // TODO: 13-12-2022 comment here
        /*seekbar.setSeekBarChangeListener(this);
        seekbar.setMaxValue(duration);
        seekbar.setLeftProgress(0);
        seekbar.setRightProgress(duration);
        seekbar.setProgressMinDiff(5);*/
    }

    @Override
    public void onProgressUpdate(long currentPosition, long duration, long bufferedPosition) {
        //seekbar.videoPlayingProgress(currentPosition);
        // TODO: 13-12-2022 comment here
        /*if (!mVideoPlayer.isPlaying() || currentPosition >= seekbar.getSelectedMaxValue().longValue()) {
            if (mVideoPlayer.isPlaying()) {
                playPause();
            }
        }*/

        /*seekbar.setSliceBlocked(false);
        seekbar.removeVideoStatusThumb();*/

//        seekbar.setPosition(currentPosition);
//        seekbar.setBufferedPosition(bufferedPosition);
//        seekbar.setDuration(duration);
    }

    private void findViews() {
        mCropVideoView = findViewById(R.id.cropVideoView);
        mIvPlay = findViewById(R.id.ivPlay);
        mIvDone = findViewById(R.id.ivDone);
        mProgressBar = findViewById(R.id.pbCropProgress);
        mTvCropProgress = findViewById(R.id.tvCropProgress);

        seekbar = findViewById(R.id.range_seek_bar);
        txtStartDuration = findViewById(R.id.txt_start_duration);
        txtEndDuration = findViewById(R.id.txt_end_duration);
        txt_total = findViewById(R.id.txt_total);

        ImageView imageOne = findViewById(R.id.image_one);
        ImageView imageTwo = findViewById(R.id.image_two);
        ImageView imageThree = findViewById(R.id.image_three);
        ImageView imageFour = findViewById(R.id.image_four);
        ImageView imageFive = findViewById(R.id.image_five);
        ImageView imageSix = findViewById(R.id.image_six);
        ImageView imageSeven = findViewById(R.id.image_seven);
        ImageView imageEight = findViewById(R.id.image_eight);
        imageViews = new ImageView[]{imageOne, imageTwo, imageThree,
                imageFour, imageFive, imageSix, imageSeven, imageEight};
        seekHandler = new Handler();
    }

    private void initListeners() {
        mIvPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //playPause();
                onVideoClicked();
            }
        });
        mIvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isValidVideo) {
                    new MaterialAlertDialogBuilder(VideoCropActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                            .setMessage("Total video trim duration should be 10 second.")
                            .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                            }).show();
                    return;
                }
                handleCropStart();
            }
        });
    }

    private void playPause() {
        isVideoPlaying = !mVideoPlayer.isPlaying();
        if (mVideoPlayer.isPlaying()) {
            mVideoPlayer.play(!mVideoPlayer.isPlaying());
            //seekbar.setSliceBlocked(false);
            //seekbar.removeVideoStatusThumb();
            mIvPlay.setImageResource(R.drawable.ic_play);
            return;
        }
        mVideoPlayer.seekTo(seekbar.getSelectedMinValue().longValue());
        mVideoPlayer.play(!mVideoPlayer.isPlaying());
        //seekbar.videoPlayingProgress(seekbar.getLeftProgress());
        mIvPlay.setImageResource(R.drawable.ic_pause);
    }


    private void onVideoClicked() {
        try {
            if (isVideoEnded) {
                seekTo(lastMinValue);
                mVideoPlayer.getPlayer().setPlayWhenReady(true);
                return;
            }
            if ((currentDuration - lastMaxValue) > 0)
                seekTo(lastMinValue);
            mVideoPlayer.getPlayer().setPlayWhenReady(!mVideoPlayer.getPlayer().getPlayWhenReady());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayer(String uri) {
        if (!new File(uri).exists()) {
            Toast.makeText(this, "File doesn't exists", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        mVideoPlayer = new VideoPlayer(VideoCropActivity.this);
        mCropVideoView.setPlayer(mVideoPlayer.getPlayer());
        mVideoPlayer.initMediaSource(this, uri);
        mVideoPlayer.setUpdateListener(this);

        fetchVideoInfo(uri);
    }

    private void fetchVideoInfo(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(new File(uri).getAbsolutePath());
        int videoWidth = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int videoHeight = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        int rotationDegrees = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

        mCropVideoView.initBounds(videoWidth, videoHeight, rotationDegrees);
    }

    private void requestStoragePermission() {
        /*if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
        } else {
            initPlayer(inputPath);
        }*/
        initPlayer(inputPath);

    }

    private void handleCropStart() {
        Rect cropRect = mCropVideoView.getCropRect();
        long startCrop = seekbar.getSelectedMinValue().longValue();
        durationCrop = seekbar.getSelectedMaxValue().longValue() - seekbar.getSelectedMinValue().longValue();
        durationCrop = durationCrop * 1000;// as per range progress return seconds instead of millis
        String start = Util.getStringForTime(formatBuilder, formatter, startCrop);
        String duration = Util.getStringForTime(formatBuilder, formatter, durationCrop);
        start += "." + startCrop % 1000;
        duration += "." + durationCrop % 1000;

        String crop = String.format("crop=%d:%d:%d:%d:exact=0", cropRect.right, cropRect.bottom, cropRect.left, cropRect.top); //"crop=w:h:x:y"

        String[] cmd = {
                "-y",
                "-ss",
                start,
                "-i",
                inputPath,
                "-t",
                duration,
                "-vf",
                crop,
                outputPath
        };

        mIvDone.setEnabled(false);
        mIvPlay.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setProgress(0);
        mTvCropProgress.setVisibility(View.VISIBLE);
        mTvCropProgress.setText("0%");

        statistics = null;

        FFmpegKit.executeWithArgumentsAsync(cmd, session -> {

            runOnUiThread(() -> {
                mIvDone.setEnabled(true);
                mIvPlay.setEnabled(true);
                mProgressBar.setVisibility(View.INVISIBLE);
                mProgressBar.setProgress(0);
                mTvCropProgress.setVisibility(View.INVISIBLE);
                mTvCropProgress.setText("0%");
            });

            if (isSuccess(session.getReturnCode())) {
                convertWebp(outputPath);
            } else if (isCancel(session.getReturnCode())) {
                //cancelled
            } else {
                //failed
            }
        });
    }

    private void convertWebp(String inputCroppedVideo) {

        mIvDone.setEnabled(false);
        mIvPlay.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setProgress(0);
        mTvCropProgress.setVisibility(View.VISIBLE);
        mTvCropProgress.setText("0%");

        statistics = null;

        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String outputName = "sticker_" + name + ".webp";
        String outputFile = getOutputVideoFolder() + "/" + outputName;

        FFmpegKit.executeWithArgumentsAsync(getWebpCmdCompress(inputCroppedVideo, outputFile), session -> {

            runOnUiThread(() -> {
                mIvDone.setEnabled(true);
                mIvPlay.setEnabled(true);
                mProgressBar.setVisibility(View.INVISIBLE);
                mProgressBar.setProgress(0);
                mTvCropProgress.setVisibility(View.INVISIBLE);
                mTvCropProgress.setText("0%");
            });

            if (isSuccess(session.getReturnCode())) {
                runOnUiThread(() -> {
                    Intent intent = new Intent();
                    intent.putExtra(ParamArgus.PATH, outputFile);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            } else if (isCancel(session.getReturnCode())) {
                //cancelled
            } else {
                //failed
            }
        });
    }

    private String[] getWebpCmd(String inputCroppedVideo) {
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String outputName = "sticker_" + name + ".webp";
        String outputFile = getOutputVideoFolder() + "/" + outputName;

        String[] cmd = {
                "-i",
                inputCroppedVideo,
                "-vcodec",
                "libwebp",
                "-filter:v",
                "fps=fps=20",
                "-lossless",
                "1",
                "-loop",
                "0",
                "-preset",
                "default",
                "-an",
                "-vsync",
                "0",
                "-s",
                "512:512",
                outputFile
        };
        return cmd;
    }

    private String[] getWebpCmdCompress(String inputCroppedVideo, String outputFile) {

        String[] cmd = {
                "-i",
                inputCroppedVideo,
                "-vcodec",
                "libwebp",
                "-filter:v",
                "fps=fps=10",
                "-lossless",
                "0",
                "-compression_level",
                "6",
                "-q:v",
                "1",
                "-loop",
                "0",
                "-preset",
                "picture",
                "-an",
                "-vsync",
                "0",
                "-s",
                "512:512",
                outputFile
        };
        return cmd;
    }

    @Override
    public void seekBarValueChanged(long leftThumb, long rightThumb) {
       /* if (seekbar.getSelectedThumb() == 1) {
            mVideoPlayer.seekTo(leftThumb);
        }*/

        /*mTvDuration.setText(Util.getStringForTime(formatBuilder, formatter, rightThumb));
        mTvProgress.setText(Util.getStringForTime(formatBuilder, formatter, leftThumb));*/
    }

    public void enableStatisticsCallback() {
        FFmpegKitConfig.enableStatisticsCallback(newStatistics -> {
            VideoCropActivity.this.statistics = newStatistics;
            //update progress

            runOnUiThread(() -> {
                if ((100 - (int) calculateProgressNew() == 0)) return;
                int progress = (int) calculateProgressNew();
                mProgressBar.setProgress(progress);
                mTvCropProgress.setText("" + progress + "%");
            });
        });
    }

    private float calculateProgressNew() {
        if (statistics == null) {
            return 0;
        }

        int timeInMilliseconds = this.statistics.getTime();
        if (timeInMilliseconds > 0) {
            long totalVideoDuration = durationCrop;

            BigDecimal completePercentage = new BigDecimal(timeInMilliseconds).multiply(new BigDecimal(100)).divide(new BigDecimal(totalVideoDuration), 0, BigDecimal.ROUND_HALF_UP);
            return completePercentage.intValue();
        }
        return 0;
    }

    private File getOutputVideoFolder() {
        File fileSaveDir = null;
        try {
            File root = new File(getFilesDir(), Constants.STICKERS_ASSET);
            fileSaveDir = new File(root, identifier);

            if (!fileSaveDir.exists()) {
                if (!root.exists())
                    root.mkdirs();
                if (!root.exists() || !fileSaveDir.mkdirs()) {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSaveDir;
    }

    private void setDataInView() {
        try {
            Runnable fileUriRunnable = () -> {
                runOnUiThread(() -> {
                    totalDuration = TrimmerUtils.getDuration(VideoCropActivity.this, Uri.parse(inputPath));
                    initTrimData();
                    buildMediaSource();
                    loadThumbnails();
                    setUpSeekBar();
                });
            };
            Executors.newSingleThreadExecutor().execute(fileUriRunnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTrimData() {
        try {
            trimType = TrimmerUtils.getTrimType(TrimType.MIN_MAX_DURATION);
            hidePlayerSeek = false;
            isAccurateCut = true;
            if (trimType == 3) {
                minFromGap = 5;
                maxToGap = 10;
                minFromGap = minFromGap != 0 ? minFromGap : totalDuration;
                maxToGap = maxToGap != 0 ? maxToGap : totalDuration;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void buildMediaSource() {
        mVideoPlayer.getPlayer().addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_ENDED:
                        //playPause();
                        //onVideoClicked();
                        isVideoEnded = true;
                        LogMessage.e("onPlayerStateChanged: Video ended.");
                        break;
                    case Player.STATE_READY:
                        isVideoEnded = false;
                        startProgress();
                        mIvPlay.setImageResource(mVideoPlayer.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
                        LogMessage.e("onPlayerStateChanged: Ready to play.");
                        break;
                    default:
                        break;
                    case Player.STATE_BUFFERING:
                        LogMessage.e("onPlayerStateChanged: STATE_BUFFERING.");
                        break;
                    case Player.STATE_IDLE:
                        LogMessage.e("onPlayerStateChanged: STATE_IDLE.");
                        break;
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });
    }

    /*
     *  loading thumbnails
     * */
    private void loadThumbnails() {
        try {
            long diff = totalDuration / 8;
            int sec = 1;
            for (ImageView img : imageViews) {
                long interval = (diff * sec) * 1000000;
                RequestOptions options = new RequestOptions().frame(interval);
                Glide.with(this)
                        .load(inputPath)
                        .apply(options)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(img);
                if (sec < totalDuration)
                    sec++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpSeekBar() {
        seekbar.setVisibility(View.VISIBLE);
        txtStartDuration.setVisibility(View.VISIBLE);
        txtEndDuration.setVisibility(View.VISIBLE);

        /*        seekbarController.setMaxValue(totalDuration).apply();*/
        seekbar.setMaxValue(totalDuration).apply();
        seekbar.setMaxStartValue((float) totalDuration).apply();

        if (trimType == 3) {
            seekbar.setMaxStartValue((float) maxToGap);
            seekbar.setGap(minFromGap).apply();
            lastMaxValue = maxToGap;
        } else {
            seekbar.setGap(2).apply();
            lastMaxValue = totalDuration;
        }
        /*if (hidePlayerSeek)
            seekbarController.setVisibility(View.GONE);*/

        seekbar.setOnRangeSeekbarFinalValueListener((minValue, maxValue) -> {
            /*if (!hidePlayerSeek)
                seekbarController.setVisibility(View.VISIBLE);*/
        });

        seekbar.setOnRangeSeekbarChangeListener((minValue, maxValue) -> {
            long minVal = (long) minValue;
            long maxVal = (long) maxValue;
            if (lastMinValue != minVal) {
                seekTo((long) minValue);
                /*if (!hidePlayerSeek)
                    seekbarController.setVisibility(View.INVISIBLE);*/
            }
            lastMinValue = minVal;
            lastMaxValue = maxVal;
            txtStartDuration.setText(TrimmerUtils.formatSeconds(minVal));
            txtEndDuration.setText(TrimmerUtils.formatSeconds(maxVal));

            long durationCrop = seekbar.getSelectedMaxValue().longValue() - seekbar.getSelectedMinValue().longValue();
            durationCrop = durationCrop * 1000;
            String duration = Util.getStringForTime(formatBuilder, formatter, durationCrop);
            txt_total.setText("Selected " + duration);

            if (trimType == 3)
                setDoneColor(minVal, maxVal);
        });

        /*seekbarController.setOnSeekbarFinalValueListener(value -> {
            long value1 = (long) value;
            if (value1 < lastMaxValue && value1 > lastMinValue) {
                seekTo(value1);
                return;
            }
            if (value1 > lastMaxValue)
                seekbarController.setMinStartValue((int) lastMaxValue).apply();
            else if (value1 < lastMinValue) {
                seekbarController.setMinStartValue((int) lastMinValue).apply();
                if (mVideoPlayer.getPlayer().getPlayWhenReady())
                    seekTo(lastMinValue);
            }
        });*/
    }

    private void seekTo(long sec) {
        if (mVideoPlayer != null)
            mVideoPlayer.seekTo(sec * 1000);
    }

    private void setDoneColor(long minVal, long maxVal) {
        try {
            //changed value is less than maxDuration
            if ((maxVal - minVal) <= maxToGap) {
                mIvDone.setAlpha(1.0f);
                isValidVideo = true;
            } else {
                mIvDone.setAlpha(0.5f);
                isValidVideo = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Runnable updateSeekbar = new Runnable() {
        @Override
        public void run() {
            try {
                currentDuration = mVideoPlayer.getPlayer().getCurrentPosition() / 1000;
                if (!mVideoPlayer.getPlayer().getPlayWhenReady())
                    return;
                if (currentDuration <= lastMaxValue) {
                    //seekbarController.setMinStartValue((int) currentDuration).apply();
                } else {
                    mVideoPlayer.getPlayer().setPlayWhenReady(false);
                }
            } finally {
                seekHandler.postDelayed(updateSeekbar, 1000);
            }
        }
    };

    void startProgress() {
        stopRepeatingTask();
        updateSeekbar.run();
    }

    void stopRepeatingTask() {
        seekHandler.removeCallbacks(updateSeekbar);
    }
}
