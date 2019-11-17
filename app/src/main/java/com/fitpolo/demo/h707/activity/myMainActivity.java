package com.fitpolo.demo.h707.activity;

import android.app.AppOpsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import com.fitpolo.demo.h707.AppConstants;
import com.fitpolo.demo.h707.R;
import com.fitpolo.demo.h707.service.MokoService;
import com.fitpolo.demo.h707.utils.Utils;
import com.fitpolo.support.MokoSupport;

public class myMainActivity extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        delayGotoMain();
    }

    private void delayGotoMain() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, AppConstants.REQUEST_CODE_ENABLE_BT);
            return;
        }

        startService(new Intent(myMainActivity.this, MokoService.class));
        startActivity(new Intent(myMainActivity.this, MainActivity.class));
        myMainActivity.this.finish();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_ENABLE_BT) {
            delayGotoMain();
        }
    }

}
