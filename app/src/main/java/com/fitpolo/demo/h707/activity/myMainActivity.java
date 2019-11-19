package com.fitpolo.demo.h707.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import com.fitpolo.demo.h707.AppConstants;
import com.fitpolo.demo.h707.R;
import com.fitpolo.demo.h707.service.MokoService;
import com.fitpolo.support.MokoSupport;

public class myMainActivity extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        checkBluetoothConnection();
    }


    private void checkBluetoothConnection() {
        //checks whether the Bluetooth is enabled.
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, AppConstants.REQUEST_CODE_ENABLE_BT);
            return;
        }
        startNextActivity();
    }

    private void startNextActivity()
    {
        //start the service MokoService
        //start the Activity  BtScanActivity
        startService(new Intent(myMainActivity.this, MokoService.class));
        startActivity(new Intent(myMainActivity.this, BtScanActivity.class));
        myMainActivity.this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //check if requestCode == AppConstants.REQUEST_CODE_ENABLE_BT. If so call startNextActivity()
        if (requestCode == AppConstants.REQUEST_CODE_ENABLE_BT) {
            startNextActivity();
        }
    }

}
