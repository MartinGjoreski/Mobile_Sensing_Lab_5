package com.fitpolo.demo.h707.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Switch;
import android.widget.Toast;

import com.fitpolo.demo.h707.AppConstants;
import com.fitpolo.demo.h707.R;
import com.fitpolo.demo.h707.service.DfuService;
import com.fitpolo.demo.h707.service.MokoService;
import com.fitpolo.demo.h707.utils.FileUtils;
import com.fitpolo.demo.h707.utils.Utils;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.entity.AutoLighten;
import com.fitpolo.support.entity.BandAlarm;
import com.fitpolo.support.entity.BleDevice;
import com.fitpolo.support.entity.DailyDetailStep;
import com.fitpolo.support.entity.DailySleep;
import com.fitpolo.support.entity.DailyStep;
import com.fitpolo.support.entity.HeartRate;
import com.fitpolo.support.entity.NoDisturb;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.entity.SitAlert;
import com.fitpolo.support.entity.SportData;
import com.fitpolo.support.entity.UserInfo;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.task.ZOpenStepListenerTask;
import com.fitpolo.support.task.ZReadAlarmsTask;
import com.fitpolo.support.task.ZReadAutoLightenTask;
import com.fitpolo.support.task.ZReadBatteryTask;
import com.fitpolo.support.task.ZReadCustomSortScreenTask;
import com.fitpolo.support.task.ZReadDateFormatTask;
import com.fitpolo.support.task.ZReadDialTask;
import com.fitpolo.support.task.ZReadHeartRateIntervalTask;
import com.fitpolo.support.task.ZReadHeartRateTask;
import com.fitpolo.support.task.ZReadLastChargeTimeTask;
import com.fitpolo.support.task.ZReadLastScreenTask;
import com.fitpolo.support.task.ZReadNoDisturbTask;
import com.fitpolo.support.task.ZReadParamsTask;
import com.fitpolo.support.task.ZReadShakeStrengthTask;
import com.fitpolo.support.task.ZReadSitAlertTask;
import com.fitpolo.support.task.ZReadSleepGeneralTask;
import com.fitpolo.support.task.ZReadSportsHeartRateTask;
import com.fitpolo.support.task.ZReadSportsTask;
import com.fitpolo.support.task.ZReadStepIntervalTask;
import com.fitpolo.support.task.ZReadStepTargetTask;
import com.fitpolo.support.task.ZReadStepTask;
import com.fitpolo.support.task.ZReadTimeFormatTask;
import com.fitpolo.support.task.ZReadUnitTypeTask;
import com.fitpolo.support.task.ZReadUserInfoTask;
import com.fitpolo.support.task.ZReadVersionTask;
import com.fitpolo.support.task.ZWriteAlarmsTask;
import com.fitpolo.support.task.ZWriteAutoLightenTask;
import com.fitpolo.support.task.ZWriteCloseTask;
import com.fitpolo.support.task.ZWriteCommonMessageTask;
import com.fitpolo.support.task.ZWriteCustomSortScreenTask;
import com.fitpolo.support.task.ZWriteDateFormatTask;
import com.fitpolo.support.task.ZWriteDialTask;
import com.fitpolo.support.task.ZWriteFindPhoneTask;
import com.fitpolo.support.task.ZWriteHeartRateIntervalTask;
import com.fitpolo.support.task.ZWriteLanguageTask;
import com.fitpolo.support.task.ZWriteLastScreenTask;
import com.fitpolo.support.task.ZWriteNoDisturbTask;
import com.fitpolo.support.task.ZWriteResetTask;
import com.fitpolo.support.task.ZWriteShakeStrengthTask;
import com.fitpolo.support.task.ZWriteShakeTask;
import com.fitpolo.support.task.ZWriteSitAlertTask;
import com.fitpolo.support.task.ZWriteStepIntervalTask;
import com.fitpolo.support.task.ZWriteStepTargetTask;
import com.fitpolo.support.task.ZWriteSystemTimeTask;
import com.fitpolo.support.task.ZWriteTimeFormatTask;
import com.fitpolo.support.task.ZWriteUnitTypeTask;
import com.fitpolo.support.task.ZWriteUserInfoTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import butterknife.ButterKnife;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description
 */

public class SendOrderActivity extends BaseActivity {
    private static final String TAG = "SendOrderActivity";
    private MokoService mService;
    private BleDevice mDevice;
    private boolean mIsUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_order_layout);
        ButterKnife.bind(this);
        mDevice = (BleDevice) getIntent().getSerializableExtra("device");
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
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
                            SendOrderActivity.this.finish();
                            break;
                    }
                }
                if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
                    abortBroadcast();
                    if (!mIsUpgrade) {
                        Toast.makeText(SendOrderActivity.this, "Connect failed", Toast.LENGTH_SHORT).show();
                        SendOrderActivity.this.finish();
                    }
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    switch (orderEnum) {
                        case Z_READ_VERSION:
                            LogModule.i("Version code：" + MokoSupport.versionCode);
                            LogModule.i("Should upgrade：" + MokoSupport.canUpgrade);
                            break;
                        case Z_READ_USER_INFO:
                            UserInfo userInfo = MokoSupport.getInstance().getUserInfo();
                            LogModule.i(userInfo.toString());
                            break;
                        case Z_READ_ALARMS:
                            ArrayList<BandAlarm> bandAlarms = MokoSupport.getInstance().getAlarms();
                            if (bandAlarms.size() == 0) {
                                return;
                            }
                            for (BandAlarm bandAlarm : bandAlarms) {
                                LogModule.i(bandAlarm.toString());
                            }
                            break;
                        case Z_READ_UNIT_TYPE:
                            boolean unitType = MokoSupport.getInstance().getUnitTypeBritish();
                            LogModule.i("Unit type british:" + unitType);
                            break;
                        case Z_READ_TIME_FORMAT:
                            int timeFormat = MokoSupport.getInstance().getTimeFormat();
                            LogModule.i("Time format:" + timeFormat);
                            break;
                        case Z_READ_AUTO_LIGHTEN:
                            AutoLighten autoLighten = MokoSupport.getInstance().getAutoLighten();
                            LogModule.i(autoLighten.toString());
                            break;
                        case Z_READ_SIT_ALERT:
                            SitAlert sitAlert = MokoSupport.getInstance().getSitAlert();
                            LogModule.i(sitAlert.toString());
                            break;
                        case Z_READ_LAST_SCREEN:
                            boolean lastScreen = MokoSupport.getInstance().getLastScreen();
                            LogModule.i("Last screen:" + lastScreen);
                            break;
                        case Z_READ_HEART_RATE_INTERVAL:
                            int interval = MokoSupport.getInstance().getHeartRateInterval();
                            LogModule.i("Heart rate interval:" + interval);
                            break;
                        case Z_READ_CUSTOM_SORT_SCREEN:
                            ArrayList<Integer> shownScreens = MokoSupport.getInstance().getCustomSortScreen();
                            if (shownScreens != null && !shownScreens.isEmpty()) {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (Integer shownScreen : shownScreens) {
                                    if (shownScreen == 0) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Activity"));
                                    } else if (shownScreen == 1) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Sport"));
                                    } else if (shownScreen == 2) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Stopwatch"));
                                    } else if (shownScreen == 3) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Timer"));
                                    } else if (shownScreen == 4) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Heart Rate"));
                                    } else if (shownScreen == 5) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Breath"));
                                    } else if (shownScreen == 6) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Sleep"));
                                    } else if (shownScreen == 7) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "More"));
                                    } else if (shownScreen == 8) {
                                        stringBuilder.append(String.format("%d:%s,", shownScreen, "Pairing code"));
                                    }
                                }
                                LogModule.i(stringBuilder.toString());
                            }

                            break;
                        case Z_READ_STEPS:
                            ArrayList<DailyStep> lastestSteps = MokoSupport.getInstance().getDailySteps();
                            if (lastestSteps == null || lastestSteps.isEmpty()) {
                                return;
                            }
                            for (DailyStep step : lastestSteps) {
                                LogModule.i(step.toString());
                            }
                            break;
                        case Z_READ_SLEEP_GENERAL:
                            ArrayList<DailySleep> lastestSleeps = MokoSupport.getInstance().getDailySleeps();
                            if (lastestSleeps == null || lastestSleeps.isEmpty()) {
                                return;
                            }
                            for (DailySleep sleep : lastestSleeps) {
                                LogModule.i(sleep.toString());
                            }
                            break;
                        case Z_READ_HEART_RATE:
                            ArrayList<HeartRate> lastestHeartRate = MokoSupport.getInstance().getHeartRates();
                            if (lastestHeartRate == null || lastestHeartRate.isEmpty()) {
                                return;
                            }
                            for (HeartRate heartRate : lastestHeartRate) {

                                LogModule.i("leatest HeartRate"+heartRate.toString());
                            }
                            break;
                        case Z_READ_STEP_TARGET:
                            LogModule.i("Step target:" + MokoSupport.getInstance().getStepTarget());
                            break;
                        case Z_READ_DIAL:
                            LogModule.i("Dial:" + MokoSupport.getInstance().getDial());
                            break;
                        case Z_READ_NODISTURB:
                            LogModule.i(MokoSupport.getInstance().getNodisturb().toString());
                            break;
                        case Z_READ_PARAMS:
                            LogModule.i("Product batch：" + MokoSupport.getInstance().getProductBatch());
                            LogModule.i("Params：" + MokoSupport.getInstance().getParams().toString());
                            break;
                        case Z_READ_LAST_CHARGE_TIME:
                            LogModule.i("Last charge time：" + MokoSupport.getInstance().getLastChargeTime());
                            break;
                        case Z_READ_BATTERY:
                            LogModule.i("Battery：" + MokoSupport.getInstance().getBatteryQuantity());
                            break;
                        case Z_READ_SHAKE_STRENGTH:
                            LogModule.i("Shake Strength：" + MokoSupport.getInstance().getShakeStrength());
                            break;
                        case Z_READ_DATE_FORMAT:
                            int dateFormat = MokoSupport.getInstance().getDateFormat();
                            LogModule.i("Date Format：" + (dateFormat == 0 ? "D/M" : "M/D"));
                            break;
                        case Z_READ_STEP_INTERVAL:
                            ArrayList<DailyDetailStep> lastestDetaileSteps = MokoSupport.getInstance().getDailyDetailSteps();
                            if (lastestDetaileSteps == null || lastestDetaileSteps.isEmpty()) {
                                return;
                            }
                            for (DailyDetailStep step : lastestDetaileSteps) {
                                LogModule.i(step.toString());
                            }
                            break;
                        case Z_READ_SPORTS:
                            ArrayList<SportData> sportDatas = MokoSupport.getInstance().getSportDatas();
                            if (sportDatas == null || sportDatas.isEmpty()) {
                                return;
                            }
                            for (SportData sportData : sportDatas) {
                                LogModule.i(sportData.toString());
                            }
                            break;
                        case Z_READ_SPORTS_HEART_RATE:
                            ArrayList<HeartRate> lastestSportsHeartRate = MokoSupport.getInstance().getSportsHeartRates();
                            if (lastestSportsHeartRate == null || lastestSportsHeartRate.isEmpty()) {
                                return;
                            }
                            for (HeartRate heartRate : lastestSportsHeartRate) {
                                LogModule.i(heartRate.toString());
                            }
                            break;
                    }

                }
                if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                    Toast.makeText(SendOrderActivity.this, "Timeout", Toast.LENGTH_SHORT).show();
                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                    Toast.makeText(SendOrderActivity.this, "Success", Toast.LENGTH_SHORT).show();
                }
                if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                    OrderEnum orderEnum = (OrderEnum) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_CURRENT_DATA_TYPE);
                    switch (orderEnum) {
                        case Z_STEPS_CHANGES_LISTENER:
                            DailyStep dailyStep = MokoSupport.getInstance().getDailyStep();
                            LogModule.i(dailyStep.toString());
//                            MokoSupport.getInstance().sendOrder(new ZWriteCommonMessageTask(mService, false, "common\nasd", true));

                            break;
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
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
            // first
            MokoSupport.getInstance().sendOrder(new ZReadVersionTask(mService));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };


    public void getInnerVersion(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadVersionTask(mService));
    }

    public void setSystemTime(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteSystemTimeTask(mService));
    }

    public void setUserInfo(View view) {
        UserInfo userInfo = new UserInfo();
        userInfo.age = 23;
        userInfo.gender = 0;
        userInfo.height = 170;
        userInfo.weight = 80;
        userInfo.birthdayMonth = 6;
        userInfo.birthdayDay = 1;
        userInfo.stepExtent = (int) Math.floor(userInfo.height * 0.45);
        MokoSupport.getInstance().sendOrder(new ZWriteUserInfoTask(mService, userInfo));
    }

    public void getUserInfo(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadUserInfoTask(mService));
    }

    public void setAllAlarms(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteAlarmsTask(mService, new ArrayList<BandAlarm>()));
    }

    public void getAllAlarms(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadAlarmsTask(mService));
    }

    public void setUnitType(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteUnitTypeTask(mService, 0));
    }


    public void getUnitType(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadUnitTypeTask(mService));
    }

    public void setTimeFormat(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteTimeFormatTask(mService, 0));
    }

    public void getTimeFormat(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadTimeFormatTask(mService));
    }

    public void setAutoLigten(View view) {
        AutoLighten autoLighten = new AutoLighten();
        autoLighten.autoLighten = 1;
        autoLighten.startTime = "08:00";
        autoLighten.endTime = "23:00";
        MokoSupport.getInstance().sendOrder(new ZWriteAutoLightenTask(mService, autoLighten));
    }

    public void getAutoLigten(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadAutoLightenTask(mService));

    }

    public void setSitAlert(View view) {
        SitAlert alert = new SitAlert();
        alert.alertSwitch = 0;
        alert.startTime = "11:00";
        alert.endTime = "18:00";
        MokoSupport.getInstance().sendOrder(new ZWriteSitAlertTask(mService, alert));
    }

    public void getSitAlert(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadSitAlertTask(mService));
    }

    public void setLastScreen(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteLastScreenTask(mService, 0));
    }

    public void getLastScreen(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadLastScreenTask(mService));
    }

    public void setHeartRateInterval(View view) {
        int hr_interval = 1 ;//10 minutes
        LogModule.i("setHeartRateInterval " +hr_interval);
        MokoSupport.getInstance().sendOrder(new ZWriteHeartRateIntervalTask(mService, hr_interval));
    }

    public void getHeartRateInterval(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadHeartRateIntervalTask(mService));
    }

    public void setCustomSortScreen(View view) {
        ArrayList<Integer> shownScreen = new ArrayList<>();
        shownScreen.add(0);//0:Activity
        shownScreen.add(4);//4:Heart Rate
        shownScreen.add(6);//6:Sleep
        shownScreen.add(12);//3:Sport Step
        shownScreen.add(13);//4:Sport Run
        shownScreen.add(14);//5:Sport Riding
        shownScreen.add(15);//6:Sport Basketball
        shownScreen.add(16);//7:Sport Football
        shownScreen.add(17);//8:Sport Yoga
        shownScreen.add(18);//9:Sport Rope Skipping
        shownScreen.add(19);//8:Sport Mountaineering
        shownScreen.add(2);//8:Stop Watch
        shownScreen.add(3);//8:Timer
        shownScreen.add(5);//8:Breathing Training
        shownScreen.add(7);//7:More
        shownScreen.add(11);//8:Message
        MokoSupport.getInstance().sendOrder(new ZWriteCustomSortScreenTask(mService, shownScreen));
    }

    public void getCustomSortScreen(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadCustomSortScreenTask(mService));
    }

    public void setStepTarget(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteStepTargetTask(mService, 20000));
    }

    public void getStepTarget(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadStepTargetTask(mService));
    }

    public void setDial(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteDialTask(mService, 2));
    }

    public void getDial(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadDialTask(mService));
    }

    public void setNoDiturb(View view) {
        NoDisturb noDisturb = new NoDisturb();
        noDisturb.noDisturb = 1;
        noDisturb.startTime = "08:00";
        noDisturb.endTime = "23:00";
        MokoSupport.getInstance().sendOrder(new ZWriteNoDisturbTask(mService, noDisturb));
    }

    public void getNoDiturb(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadNoDisturbTask(mService));
    }


    public void setDateFormat(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteDateFormatTask(mService, 1));
    }

    public void getDateFormat(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadDateFormatTask(mService));
    }

    public void setShakeStrength(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteShakeStrengthTask(mService, 1));

    }

    public void getShakeStrength(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadShakeStrengthTask(mService));
    }

    public void getLastestSteps(View view) {
        Calendar calendar = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadStepTask(mService, calendar));
    }

    public void getLastestSleeps(View view) {
        Calendar calendar = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadSleepGeneralTask(mService, calendar));
    }

    public void getLastestHeartRate(View view) {
        Calendar calendar = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadHeartRateTask(mService, calendar));
    }

    public void openStepChangeListener(View view) {
        MokoSupport.getInstance().sendOrder(new ZOpenStepListenerTask(mService));
    }

    public void getFirmwareParams(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadParamsTask(mService));
    }

    public void getBattery(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadBatteryTask(mService));
    }

    public void getLastChargeTime(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadLastChargeTimeTask(mService));
    }

    public void openFindPhone(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteFindPhoneTask(mService));
    }

    public void setLanguage(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteLanguageTask(mService));
    }

    public void shakeBand(View view) {
        //        int new_shake_strength = 5;
        LogModule.i("shakeBand: ");
        MokoSupport.getInstance().sendDirectOrder(new ZWriteShakeTask(mService));
    }

    public void sendMultiOrders(View view) {
        ZReadUnitTypeTask unitTypeTask = new ZReadUnitTypeTask(mService);
        ZReadTimeFormatTask timeFormatTask = new ZReadTimeFormatTask(mService);
        ZReadSitAlertTask sitAlertTask = new ZReadSitAlertTask(mService);
        MokoSupport.getInstance().sendOrder(unitTypeTask, timeFormatTask, sitAlertTask);
    }


    public void setStepInterval(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteStepIntervalTask(mService));
    }

    public void getStepInterval(View view) {
        Calendar calendar = Utils.strDate2Calendar("2019-04-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadStepIntervalTask(mService, calendar));
    }

    public void notification(View view) {
        startActivity(new Intent(this, MessageNotificationActivity.class));
    }

    private static final int REQUEST_CODE_FILE = 2;

    public void upgradeFirmware(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_FILE);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "install file manager app", Toast.LENGTH_SHORT).show();
        }
    }


    public void reset(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteResetTask(mService));
    }

    public void close(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteCloseTask(mService));
    }


    public void getSportData(View view) {
        Calendar calendar = Utils.strDate2Calendar("2019-04-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadSportsTask(mService, calendar));
    }

    public void getSportHeartRate(View view) {
        Calendar calendar = Utils.strDate2Calendar("2019-04-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadSportsHeartRateTask(mService, calendar));
    }


    private void upgrade(String firmwarePath) {
        mIsUpgrade = true;
        final File firmwareFile = new File(firmwarePath);
        if (firmwareFile.exists()) {
            final DfuServiceInitiator starter = new DfuServiceInitiator(mDevice.address)
                    .setDeviceName(mDevice.name)
                    .setKeepBond(false)
                    .setDisableNotification(true);
            starter.setZip(null, firmwarePath);
            starter.start(this, DfuService.class);
            showDFUProgressDialog("Waiting...");
        } else {
            Toast.makeText(this, "file is not exists!", Toast.LENGTH_SHORT).show();
        }
    }

    private void back() {
        if (MokoSupport.getInstance().isConnDevice(this, mDevice.address)) {
            mService.disConnectBle();
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_FILE:
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(this, uri);
                    upgrade(path);
                    break;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    private int mDeviceConnectCount;


    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            LogModule.w("onDeviceConnecting...");
            mDeviceConnectCount++;
            if (mDeviceConnectCount > 3) {
                Toast.makeText(SendOrderActivity.this, "Error:DFU Failed", Toast.LENGTH_SHORT).show();
                dismissDFUProgressDialog();
                final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(SendOrderActivity.this);
                final Intent abortAction = new Intent(DfuService.BROADCAST_ACTION);
                abortAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
                manager.sendBroadcast(abortAction);
            }
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            LogModule.w("onDeviceDisconnecting...");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            mDFUDialog.setMessage("DfuProcessStarting...");
        }


        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            mDFUDialog.setMessage("EnablingDfuMode...");
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            mDFUDialog.setMessage("FirmwareValidating...");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            Toast.makeText(SendOrderActivity.this, "DfuCompleted!", Toast.LENGTH_SHORT).show();
            dismissDFUProgressDialog();
            SendOrderActivity.this.finish();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            mDFUDialog.setMessage("DfuAborted...");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            mDFUDialog.setMessage("Progress:" + percent + "%");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            Toast.makeText(SendOrderActivity.this, "Error:" + message, Toast.LENGTH_SHORT).show();
            LogModule.i("Error:" + message);
            dismissDFUProgressDialog();
        }
    };

    private ProgressDialog mDFUDialog;

    private void showDFUProgressDialog(String tips) {
        mDFUDialog = new ProgressDialog(SendOrderActivity.this);
        mDFUDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDFUDialog.setCanceledOnTouchOutside(false);
        mDFUDialog.setCancelable(false);
        mDFUDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDFUDialog.setMessage(tips);
        if (!isFinishing() && mDFUDialog != null && !mDFUDialog.isShowing()) {
            mDFUDialog.show();
        }
    }

    private void dismissDFUProgressDialog() {
        mDeviceConnectCount = 0;
        mIsUpgrade = false;
        if (!isFinishing() && mDFUDialog != null && mDFUDialog.isShowing()) {
            mDFUDialog.dismiss();
        }
    }


    public class SmartSensing {

        private static final int SPORTS_SENSING = 0; // 0： turn off normal sesnig and activate sports sensing
        private static final int NORMAL_SENSING = 1; // 1： 10mins；
        private static final int SLEEP_SENSING =  3; // 3： 30mins
        private static final int SLEEP_HOUR_START = 23; // sleep period start
        private static final int SLEEP_HOUR_END = 6; // sleep period end

        //example chart for cardio zones: http://www.globalwomenconnected.com/2017/10/importance-healthy-heart-rate/
        private static final int CARDIO_ZONE_1_START = 120; //CARDIO ZONE 1: 120 - 140 BPM - LOW INTENSITY, EFFECTIVE FAT BURN
        private static final int CARDIO_ZONE_2_START  = 140;  //CARDIO ZONE 2: 120 - 140 BPM - INTERMEDIATE INTENSITY,
        private static final int CARDIO_ZONE_3_START  = 160;  //CARDIO ZONE 3: 140 - 160 BPM - PERFORMANCE TRAINING,
        private static final int CARDIO_ZONE_4_START  = 180;  //DANGER ZONE 4: OVER 180 BPM - DANGER ZONE, ONLY FOR ATHLETES
        private static final int CARDIO_ZONE_MESSAGE_INTERVAL  = 180;  //DANGER ZONE 4: OVER 180 BPM - DANGER ZONE, ONLY FOR ATHLETES


        private static final String CARDIO_ZONE_0_MSG = "VERY LOW INTENSITY CARDIO. INCREASE INTENSITY FOR FAT-BURN ZONE.";
        private static final String CARDIO_ZONE_1_MSG = "LOW INTENSITY - FAT BURN ZONE.";
        private static final String CARDIO_ZONE_2_MSG = "INTERMEDIATE INTENSITY - AEROBIC ZONE.";
        private static final String CARDIO_ZONE_3_MSG = "VIGOROUS INTENSITY - HIGH PERFORMANCE ZONE.";
        private static final String CARDIO_ZONE_4_MSG = "EXTREME INTENSITY - ATHLETE'S ZONE. YOU MAY WANT TO SLOW DOWN";


        private Handler handler;
        private int SENSE_INTERVAL ;
        private String CARDIO_ZONE_MSG;
        private int CARDIO_ZONE_TIMER; //SEND MESSAGE EVERY 3 MINUTES
        private HeartRate LAST_HEARTRATE;


        public void startSmartSensing()
        {
            this.handler = new Handler();
            updateSensingInterval();
            CARDIO_ZONE_TIMER = CARDIO_ZONE_MESSAGE_INTERVAL;
        }


        public void stopSmartSensing()
        {
            if (this.handler!=null)
                this.handler.removeMessages(0);

        }


        private void updateSensingInterval()
        {
            SENSE_INTERVAL = NORMAL_SENSING;
            CARDIO_ZONE_MSG="";
            //check sleep hours
            if (isSleepingTime()) {
                SENSE_INTERVAL = SLEEP_SENSING; //Infer whether a user is sleeping, if so sense less often
            }
            //Infer whether a user is performing a sport activity, if so sense more often
            else if (isActiveTime())
            {
                CARDIO_ZONE_TIMER--;
                SENSE_INTERVAL = SPORTS_SENSING;
                //- If a user is doing a sport activity, update the UI to show which cardio zone she is
                UpdateCardioZone();
            }

            updateBand();
            handler.post(new Runnable() {
                public void run() {
                    updateSensingInterval();
                    handler.postDelayed(this, SENSE_INTERVAL);
                }
            });

        }

        private boolean isSleepingTime() {
            Calendar c =  Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            return !(hour>SLEEP_HOUR_END && hour<SLEEP_HOUR_START);
        }


        //check if the user is performing activity
        private boolean isActiveTime() {
            ArrayList<HeartRate> latestSportsHeartRate = MokoSupport.getInstance().getSportsHeartRates();
            if (latestSportsHeartRate == null || latestSportsHeartRate.isEmpty()) {
                return false;
            }
            //check if the last HR was in the last 2 minutes
            LAST_HEARTRATE = latestSportsHeartRate.get(latestSportsHeartRate.size()-1);
            Calendar calendar_hr = LAST_HEARTRATE.strDate2Calendar(LAST_HEARTRATE.time, "yyyy-MM-dd HH:mm");
            Calendar calendar_now =  Calendar.getInstance();
            calendar_now.set(Calendar.MINUTE,calendar_now.get(Calendar.MINUTE)-2);
            if (calendar_hr.getTime().getTime()<calendar_hr.getTime().getTime())
                return false;

            return true;
        }


        private void UpdateCardioZone()
        {
            int avgHR = Integer.valueOf(LAST_HEARTRATE.value);
            if (avgHR<CARDIO_ZONE_1_START)
                CARDIO_ZONE_MSG = CARDIO_ZONE_0_MSG;

            else if (avgHR>=CARDIO_ZONE_1_START && avgHR<CARDIO_ZONE_2_START)
                CARDIO_ZONE_MSG = CARDIO_ZONE_1_MSG;

            else if (avgHR>=CARDIO_ZONE_2_START && avgHR<CARDIO_ZONE_3_START)
                CARDIO_ZONE_MSG = CARDIO_ZONE_2_MSG;

            else if (avgHR>=CARDIO_ZONE_3_START && avgHR<CARDIO_ZONE_4_START)
                CARDIO_ZONE_MSG = CARDIO_ZONE_3_MSG;

            else if (avgHR>=CARDIO_ZONE_4_START)
                CARDIO_ZONE_MSG = CARDIO_ZONE_4_MSG;

        }

        private void updateBand()
        {
            MokoSupport.getInstance().sendOrder(new ZWriteHeartRateIntervalTask(mService, SENSE_INTERVAL));
            if (!CARDIO_ZONE_MSG.equals("") && CARDIO_ZONE_TIMER==0)//if there is a message ready, send it EVERY "CARDIO_ZONE_TIMER" MINUTES
            {
                MokoSupport.getInstance().sendOrder(new ZWriteCommonMessageTask(mService, false, CARDIO_ZONE_MSG, true));
                CARDIO_ZONE_TIMER = CARDIO_ZONE_MESSAGE_INTERVAL;
            }
        }
    }

}
