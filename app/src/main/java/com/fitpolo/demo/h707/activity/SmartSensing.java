package com.fitpolo.demo.h707.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.service.autofill.BatchUpdates;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fitpolo.demo.h707.R;
import com.fitpolo.demo.h707.service.MokoService;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.entity.BleDevice;
import com.fitpolo.support.entity.DailyStep;
import com.fitpolo.support.entity.HeartRate;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.task.ZOpenStepListenerTask;
import com.fitpolo.support.task.ZReadVersionTask;
import com.fitpolo.support.task.ZWriteCommonMessageTask;
import com.fitpolo.support.task.ZWriteShakeTask;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.ButterKnife;

public class SmartSensing extends BaseActivity  {

    private static final String TAG = "SendOrderActivity";
    private MokoService mService;
    private BleDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smart_sensing_layout);
        ButterKnife.bind(this);
        mDevice = (BleDevice) getIntent().getSerializableExtra("device");
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
        startSmartSensing();
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
            filter.addAction(MokoConstants.ACTION_DISCOVER_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_ORDER_RESULT);
            filter.addAction(MokoConstants.ACTION_ORDER_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_ORDER_FINISH);
            filter.addAction(MokoConstants.ACTION_CURRENT_DATA);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.setPriority(200);
            registerReceiver(mReceiver, filter);
            MokoSupport.getInstance().sendOrder(new ZReadVersionTask(mService));

            //register on step change listener
            MokoSupport.getInstance().sendOrder(new ZOpenStepListenerTask(mService));

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
        handler.removeMessages(0);
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF:
                            SmartSensing.this.finish();
                            break;
                    }
                }
                if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
                    SmartSensing.this.finish();
                }

                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    switch (orderEnum) {
                        case Z_READ_SPORTS_HEART_RATE:
                            Log.d(TAG,"Heartrate:");

                            ArrayList<HeartRate> lastestSportsHeartRate = MokoSupport.getInstance().getSportsHeartRates();
                            if (lastestSportsHeartRate == null || lastestSportsHeartRate.isEmpty()) {
                                return;
                            }
                            for (HeartRate heartRate : lastestSportsHeartRate) {
                                LogModule.i(heartRate.toString());
                                Log.d(TAG,heartRate.toString());

                            }
                            break;
                    }
                }
                if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                    OrderEnum orderEnum = (OrderEnum) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_CURRENT_DATA_TYPE);
                    switch (orderEnum) {
                        case Z_STEPS_CHANGES_LISTENER:
                            DailyStep dailyStep = MokoSupport.getInstance().getDailyStep();
                            DAILY_STEPS = dailyStep;
                            Calendar c =  Calendar.getInstance();
                            DAILY_STEPS_TIME= c.getTimeInMillis();
                            updateSensing();
                            break;
                    }
                }
            }
        }
    };


    public void shakeMyBand(View view) {
        //        int new_shake_strength = 5;
        LogModule.i("shakeBand: ");
        MokoSupport.getInstance().sendDirectOrder(new ZWriteShakeTask(mService));
    }


    private static final int SPORTS_SENSING = 0; // 0： turn off normal sesnig and activate sports sensing
    private static final int NORMAL_SENSING = 1; // 1： 10mins；
    private static final int SLEEP_SENSING =  3; // 3： 30mins

    private static final int SLEEP_HOUR_START = 23; // sleep period start
    private static final int SLEEP_HOUR_END = 6; // sleep period end


    private static final int CARDIO_ZONE_1_START = 1; //steps per second
    private static final int CARDIO_ZONE_2_START  = 3;  //CARDIO ZONE 2: 120 - 140 BPM - INTERMEDIATE INTENSITY,
    private static final int CARDIO_ZONE_3_START  = 6;  //CARDIO ZONE 3: 140 - 160 BPM - PERFORMANCE TRAINING,
    private static final int CARDIO_ZONE_4_START  = 9;  //DANGER ZONE 4: OVER 180 BPM - DANGER ZONE, ONLY FOR ATHLETES
    private static final int BAND_UPDATE_INTERVAL  = 30;  //ONCE PER x seconds


    private static final String CARDIO_ZONE_0_MSG = "VERY LOW INTENSITY.";
    private static final String CARDIO_ZONE_1_MSG = "LOW INTENSITY.";
    private static final String CARDIO_ZONE_2_MSG = "INTERMEDIATE INTENSITY.";
    private static final String CARDIO_ZONE_3_MSG = "VIGOROUS INTENSITY.";
    private static final String CARDIO_ZONE_4_MSG = "EXTREME INTENSITY.";


    private int SENSE_INTERVAL;
    private String CARDIO_ZONE_MSG;
    private Long BAND_UPDATE_LAST ;
    private DailyStep DAILY_STEPS;
    private long DAILY_STEPS_TIME;
    private DailyStep PREV_DAILY_STEPS;
    private long PREV_DAILY_STEPS_TIME;
    private Double STEPS_PER_SECOND;
    private Handler handler;


    //initialization
    public void startSmartSensing()
    {
        SENSE_INTERVAL = NORMAL_SENSING;
        CARDIO_ZONE_MSG="";
        DAILY_STEPS=null;
        DAILY_STEPS=null;
        PREV_DAILY_STEPS=null;
        handler = new Handler();
        updateSensing();
    }


    private void updateSensing()
    {
        if (isSleepingTime()) {
            SENSE_INTERVAL = SLEEP_SENSING; //Check whether a user is sleeping, if so sense less often
        }
        else {
            CalculateCardioZone();
        }
        UpdateUI();
        UpdateBand();
    }

    private boolean isSleepingTime() {
        Calendar c =  Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return (hour>SLEEP_HOUR_END && hour<SLEEP_HOUR_START);
    }

    private void CalculateCardioZone()
    {
        if (DAILY_STEPS == null && PREV_DAILY_STEPS==null)
            return;

        if (DAILY_STEPS != null && PREV_DAILY_STEPS==null) {
            PREV_DAILY_STEPS = DAILY_STEPS;
            PREV_DAILY_STEPS_TIME = DAILY_STEPS_TIME;
            return;
        }

        //calculate steps per second
        double time_passed = (DAILY_STEPS_TIME-PREV_DAILY_STEPS_TIME)/1000.0; //miliseconds to seconds
        STEPS_PER_SECOND = (Double.valueOf(DAILY_STEPS.count)-Double.valueOf(PREV_DAILY_STEPS.count))/time_passed;
        PREV_DAILY_STEPS = DAILY_STEPS;
        PREV_DAILY_STEPS_TIME = DAILY_STEPS_TIME;

        if (STEPS_PER_SECOND<CARDIO_ZONE_1_START)
            CARDIO_ZONE_MSG = CARDIO_ZONE_0_MSG;

        else if (STEPS_PER_SECOND>=CARDIO_ZONE_1_START && STEPS_PER_SECOND<CARDIO_ZONE_2_START)
            CARDIO_ZONE_MSG = CARDIO_ZONE_1_MSG;

        else if (STEPS_PER_SECOND>=CARDIO_ZONE_2_START && STEPS_PER_SECOND<CARDIO_ZONE_3_START)
            CARDIO_ZONE_MSG = CARDIO_ZONE_2_MSG;

        else if (STEPS_PER_SECOND>=CARDIO_ZONE_3_START && STEPS_PER_SECOND<CARDIO_ZONE_4_START)
            CARDIO_ZONE_MSG = CARDIO_ZONE_3_MSG;

        else if (STEPS_PER_SECOND>=CARDIO_ZONE_4_START)
            CARDIO_ZONE_MSG = CARDIO_ZONE_4_MSG;

        SENSE_INTERVAL=SPORTS_SENSING;

        handler.removeMessages(0);//remove any previous calls
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearCardioInfo();
            }
        }, 5000);
    }


    public void clearCardioInfo()
    {
        STEPS_PER_SECOND=0.0;
        CARDIO_ZONE_MSG = "";
        SENSE_INTERVAL=NORMAL_SENSING;
        UpdateUI();
    }


    private void UpdateBand()
    {
        if (BAND_UPDATE_LAST==null && !CARDIO_ZONE_MSG.equals(""))//if there is a message ready, send it (one message per minute)
        {
            MokoSupport.getInstance().sendOrder(new ZWriteCommonMessageTask(mService, false, CARDIO_ZONE_MSG, true));
            Calendar c =  Calendar.getInstance();
            BAND_UPDATE_LAST =c.getTimeInMillis();
        }
        if (BAND_UPDATE_LAST!=null)
        {
            Calendar c =  Calendar.getInstance();
            Long elapsed_seconds = (c.getTimeInMillis()- BAND_UPDATE_LAST)/1000;
            if (elapsed_seconds>= BAND_UPDATE_INTERVAL) {
                BAND_UPDATE_LAST = c.getTimeInMillis();
                MokoSupport.getInstance().sendOrder(new ZWriteCommonMessageTask(mService, false, CARDIO_ZONE_MSG, true));
            }
        }

    }

    private void UpdateUI()
    {
        Log.d(TAG,"updateUI...");
        if (DAILY_STEPS == null || PREV_DAILY_STEPS==null)
            return;
        TextView sensing = (TextView)this.findViewById(R.id.sensing_value);
        TextView cardio = (TextView)this.findViewById(R.id.cardio_value);
        TextView steps = (TextView)this.findViewById(R.id.steps_value);
        TextView steps_sec = (TextView)this.findViewById(R.id.steps_second_value);

        sensing.setText(String.valueOf(SENSE_INTERVAL));
        cardio.setText(String.valueOf(CARDIO_ZONE_MSG));
        steps.setText(String.valueOf(DAILY_STEPS.count));
        steps_sec.setText(String.valueOf(STEPS_PER_SECOND));


        //change color to green if the SENSE_INTERVAL==SPORTS_SENSING
        LinearLayout layout=(LinearLayout)findViewById(R.id.parentLayout);
        if (SENSE_INTERVAL==SPORTS_SENSING)
            layout.setBackgroundColor(Color.GREEN);
        else
            layout.setBackgroundColor(Color.WHITE);
    }



}
