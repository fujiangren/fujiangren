package my.progressdemo;

import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.nio.ShortBuffer;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private Handler mHandler = null;
    private RecorderState currentRecorderState = RecorderState.PRESS;

    //判断是否需要录制，手指按下继续，抬起时暂停
    boolean recording = false;

    //判断是否开始了录制，第一次按下屏幕时设置为true
    boolean isRecordingStarted = false;

    //第一次按下屏幕时记录的时间
    long firstTime = 0;

    //每次按下手指和抬起之间的暂停时间
    long totalPauseTime = 0;
    //手指抬起是的时间
    long pausedTime = 0;

    //总的暂停时间
    long stopPauseTime = 0;

    //手指抬起是的时间
    long startPauseTime = 0;

    //判断是否需要录制，点击下一步时暂停录制
    private boolean rec = false;

    //进度条
    private ProgressView progressView;

    //预览的宽高和屏幕宽高
    private int previewWidth = 480, screenWidth = 480;
    private int previewHeight = 480, screenHeight = 800;

    //录制的有效总时间
    long totalTime = 0;

    //视频帧率
    private int frameRate = 30;
    //录制的最长时间
    private int recordingTime = 10000;
    //录制的最短时间
    private int recordingMinimumTime = 6000;
    //提示换个场景
    private int recordingChangeTime = 3000;
    private LinearLayout llytTestTop = null;

    boolean recordFinish = false;

    //视频时间戳
    private long mVideoTimestamp = 0L;
    //时候保存过视频文件
    private boolean isRecordingSaved = false;
    private boolean isFinalizing = false;

    private byte[] firstData = null;

    private boolean initSuccess = false;

    //调用系统的录制音频类
    private AudioRecord audioRecord;
    private Thread audioThread;
    //开启和停止录制音频的标记
    volatile boolean runAudioThread = true;

    private volatile long mAudioTimeRecorded;
    private Button btnTest = null;
    private boolean isBeginRecord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        //Find screen dimensions
        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;

        btnTest = findViewById(R.id.btnTest);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        initHandler();
        initLayout();
        initThread();
    }

    private void initThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.d("isBeginRecord","isBeginRecord119:"+isBeginRecord);
                while (isBeginRecord){

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }

            }
        }).start();
    }


    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                /*case 1:
					final byte[] data = (byte[]) msg.obj;
					ThreadPoolUtils.execute(new Runnable() {

						@Override
						public void run() {
							getFirstCapture(data);
						}
					});
					break;*/
                    case 2:
                        int resId = 0;
                        if (currentRecorderState == RecorderState.PRESS) {
                            resId = R.drawable.video_text01;
                        } else if (currentRecorderState == RecorderState.LOOSEN) {
                            resId = R.drawable.video_text02;
                        } else if (currentRecorderState == RecorderState.CHANGE) {
                            resId = R.drawable.video_text03;
                        } else if (currentRecorderState == RecorderState.SUCCESS) {
                            resId = R.drawable.video_text04;
                        }
//                        stateImageView.setImageResource(resId);
                        break;
                    case 3:
                        if (!recording)
                            initiateRecording(true);
                        else {
                            //更新暂停的时间
                            stopPauseTime = System.currentTimeMillis();
                            totalPauseTime = stopPauseTime - startPauseTime;
                            pausedTime += totalPauseTime;
                        }
                        rec = true;
                        //开始进度条增长
                        progressView.setCurrentState(ProgressView.State.START);
                        //setTotalVideoTime();
                        break;
                    case 4:
                        //设置进度条暂停状态
                        progressView.setCurrentState(ProgressView.State.PAUSE);
                        //将暂停的时间戳添加到进度条的队列中
                        progressView.putProgressList((int) totalTime);
                        rec = false;
                        startPauseTime = System.currentTimeMillis();
                        if (totalTime >= recordingMinimumTime) {
                            currentRecorderState = RecorderState.SUCCESS;
                            mHandler.sendEmptyMessage(2);
                        } else if (totalTime >= recordingChangeTime) {
                            currentRecorderState = RecorderState.CHANGE;
                            mHandler.sendEmptyMessage(2);
                        }
                        break;
                    case 5:
                        currentRecorderState = RecorderState.SUCCESS;
                        mHandler.sendEmptyMessage(2);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (!recordFinish) {
            if (totalTime < recordingTime) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //如果MediaRecorder没有被初始化
                        //执行初始化
                        mHandler.removeMessages(3);
                        mHandler.removeMessages(4);
                        mHandler.sendEmptyMessageDelayed(3, 300);
                        isBeginRecord = true;
                        break;
                    case MotionEvent.ACTION_UP:

                        isBeginRecord = false;
                        totalTime = System.currentTimeMillis() - firstTime - pausedTime;
                        mHandler.removeMessages(3);
                        mHandler.removeMessages(4);
                        if (rec)
                            mHandler.sendEmptyMessage(4);

                        break;
                }
            } else {
                //如果录制时间超过最大时间，保存视频
                rec = false;
            }
        }
        return true;
    }

    public enum RecorderState {
        PRESS(1), LOOSEN(2), CHANGE(3), SUCCESS(4);

        static RecorderState mapIntToValue(final int stateInt) {
            for (RecorderState value : RecorderState.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return PRESS;
        }

        private int mIntValue;

        RecorderState(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
    }


    /**
     * 第一次按下时，初始化录制数据
     *
     * @param isActionDown
     */
    private void initiateRecording(boolean isActionDown) {
        isRecordingStarted = true;
        firstTime = System.currentTimeMillis();

        recording = true;
        totalPauseTime = 0;
        pausedTime = 0;

        //txtTimer.setVisibility(View.VISIBLE);
        //handler.removeCallbacks(mUpdateTimeTask);
        //handler.postDelayed(mUpdateTimeTask, 100);
    }

    private void initLayout() {

        progressView = findViewById(R.id.recorder_progress);
        llytTestTop = findViewById(R.id.llytTestTop);
        llytTestTop.setOnTouchListener(this);

        initCameraLayout();

    }

    /**
     * 停止录制
     *
     * @author QD
     */
    public class AsyncStopRecording extends AsyncTask<Void, Integer, Void> {

        private ProgressBar bar;
        private TextView progress;

        @Override
        protected void onPreExecute() {
            isFinalizing = true;
            recordFinish = true;
            runAudioThread = false;

            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setText(values[0] + "%");
            bar.setProgress(values[0]);
        }


        @Override
        protected Void doInBackground(Void... params) {

            isFinalizing = false;
            if (recording) {
                recording = false;
                releaseResources();
            }
            publishProgress(100);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

    }

    /**
     * 释放资源，停止录制视频和音频
     */
    private void releaseResources() {
        isRecordingSaved = true;

        //停止刷新进度
        progressView.setCurrentState(ProgressView.State.PAUSE);
    }

    private void initCameraLayout() {
        new AsyncTask<String, Integer, Boolean>() {

            @Override
            protected Boolean doInBackground(String... params) {

                if (!initSuccess) {

                    startRecording();

                    initSuccess = true;
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {

            }

        }.execute("start");
    }




    private void startRecording() {

    }

}
