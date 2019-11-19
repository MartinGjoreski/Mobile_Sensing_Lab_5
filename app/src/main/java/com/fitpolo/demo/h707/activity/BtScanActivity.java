package com.fitpolo.demo.h707.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.fitpolo.demo.h707.R;
import com.fitpolo.demo.h707.adapter.DeviceAdapter;
import com.fitpolo.demo.h707.service.MokoService;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoScanDeviceCallback;
import com.fitpolo.support.entity.BleDevice;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description
 */

public class BtScanActivity extends BaseActivity implements AdapterView.OnItemClickListener, MokoScanDeviceCallback {
    private static final String TAG = "BtScanActivity";
    @Bind(R.id.lv_device)
    ListView lvDevice;


    private ArrayList<BleDevice> mDatas;
    private DeviceAdapter mAdapter;
    private ProgressDialog mDialog;
    private MokoService mService;
    private BleDevice mDevice;
    private HashMap<String, BleDevice> deviceMap;


    //helper variables to navigate autoconnection
    private String default_pref_string = "NO VALUE";
    private String pref_key_address = "DEVICE_ADDRESS";
    private String pref_key_name = "DEVICE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_scan_layout);
        ButterKnife.bind(this);
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
    }

    //if device exists in shared preferences connect to it and return true, if not, return false
    private boolean autoConnect()
    {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Auto Connecting...");
        mDialog.show();
        BleDevice device = readFromSharedPreferences();
        if (!device.address.equals(default_pref_string))
        {
            initiateConnection(device);
            return true;
        }
        mDialog.dismiss(); //Auto connecting not possible
        return false;
    }


    public void saveToSharedPreferences(BleDevice device)
    {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(pref_key_address ,device.address);
        editor.putString(pref_key_name ,device.name);
        Log.d(TAG,"Added to sharedPrefs:"+device.address);
        editor.commit();
    }

    private BleDevice readFromSharedPreferences()
    {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        BleDevice device = new BleDevice();
        device.address =  sharedPref.getString(pref_key_address,default_pref_string);
        device.name = sharedPref.getString(pref_key_name,default_pref_string);
        Log.d(TAG,"Read from sharedPrefs:"+device.address);
        return device;
    }

    private void initContentView() {
        mDialog = new ProgressDialog(this);
        mDatas = new ArrayList<>();
        deviceMap = new HashMap<>();
        mAdapter = new DeviceAdapter(this);
        mAdapter.setItems(mDatas);
        lvDevice.setAdapter(mAdapter);
        lvDevice.setOnItemClickListener(this);
    }



    public void initiateConnection(BleDevice device)
    {
        Log.d(TAG,"initiateConnection with: "+device.address);
        mDevice = device;
        mService.connectBluetoothDevice(mDevice.address);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mDialog.setMessage("Connect...");
        mDialog.show();
        BleDevice device = (BleDevice) parent.getItemAtPosition(position);
        saveToSharedPreferences(device);
        initiateConnection(device);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(intent.getAction())) {
                    abortBroadcast();
                    if (!BtScanActivity.this.isFinishing() && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                    Toast.makeText(BtScanActivity.this, "Connect success", Toast.LENGTH_SHORT).show();
                    Intent orderIntent = new Intent(BtScanActivity.this, SmartSensing.class);
                    orderIntent.putExtra("device", mDevice);
                    startActivity(orderIntent);
                }
                if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(intent.getAction())) {
                    abortBroadcast();
                    if (MokoSupport.getInstance().isBluetoothOpen() && MokoSupport.getInstance().getReconnectCount() > 0) {
                        return;
                    }
                    if (!BtScanActivity.this.isFinishing() && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                    Toast.makeText(BtScanActivity.this, "Connect failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
        stopService(new Intent(this, MokoService.class));
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
            filter.addAction(MokoConstants.ACTION_DISCOVER_SUCCESS);
            filter.setPriority(100);
            registerReceiver(mReceiver, filter);
            Log.d(TAG,"Service connected");

//            autoConnect();
//            if (!autoConnect())
//                initContentView(); //scan for devices
        initContentView(); //scan for devices

            Log.d(TAG,"onCreate");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public void searchDevices(View view) {
        MokoSupport.getInstance().startScanDevice(this);
    }


    @Override
    public void onStartScan() {
        deviceMap.clear();
        mDialog.setMessage("Scanning...");
        mDialog.show();
    }


    @Override
    public void onScanDevice(BleDevice device) {
        deviceMap.put(device.address, device);
        mDatas.clear();
        mDatas.addAll(deviceMap.values());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStopScan() {
        if (!BtScanActivity.this.isFinishing() && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDatas.clear();
        mDatas.addAll(deviceMap.values());
        mAdapter.notifyDataSetChanged();
    }
}
