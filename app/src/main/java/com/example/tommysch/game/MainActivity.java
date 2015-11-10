package com.example.tommysch.game;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public GameCharacter main = null;

    public static final int PAINT = 0;
    public static final int ERASE = 1;
    public static final int LOAD = 2;

    private static final String TAG = "MainGameScreen";
    private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "We're starting the receiver");
            Toast.makeText(getApplicationContext(), "We're starting the receiver", Toast.LENGTH_SHORT).show();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(TAG, "permission denied for accessory "
                                + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }
        }
    };


    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }


    private void closeAccessory() {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "We're in oncreate");
        Toast.makeText(getApplicationContext(), "We're in oncreate", Toast.LENGTH_SHORT).show();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mInputStream != null && mOutputStream != null) {
            return;
        }
        Toast.makeText(getApplicationContext(), "onResume", Toast.LENGTH_SHORT).show();

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                Toast.makeText(getApplicationContext(), "opening accessory", Toast.LENGTH_SHORT).show();
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory,mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(TAG, "mAccessory is null");
            Toast.makeText(getApplicationContext(), "null accessory", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void moveDown(View v) {
        if(main == null) {
            main = new GameCharacter(15, 15, 1, 1);
        }
        main.y += 1;
        drawToBoard(main);
    }

    public void moveUp(View v) {
        if(main == null) {
            main = new GameCharacter(15, 15, 1, 1);
        }
        main.y -= 1;
        drawToBoard(main);
    }

    public void moveLeft(View v) {
        if(main == null) {
            main = new GameCharacter(15, 15, 1, 1);
        }
        main.x -= 1;
        drawToBoard(main);
    }

    public void moveRight(View v) {
        if(main == null) {
            main = new GameCharacter(15, 15, 1, 1);
        }
        main.x += 1;
        drawToBoard(main);
    }

    public void drawToBoard(GameCharacter gc) {
        //eraseBoard();

        byte[] msg = new byte[6];
        msg[0] = PAINT;
        msg[1] = Byte.parseByte(((Integer) gc.x).toString());
        msg[2] = Byte.parseByte(((Integer) gc.y).toString());
        msg[3] = Byte.parseByte(((Integer) 1).toString());
        msg[4] = Byte.parseByte(((Integer) 0).toString());
        msg[5] = Byte.parseByte(((Integer) 0).toString());

        if (mOutputStream != null) {
            try {
                mOutputStream.write(msg);
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }

    public void eraseBoard(){
        byte[] msg = new byte[6];
        msg[0] = ERASE;

        if (mOutputStream != null) {
            try {
                mOutputStream.write(msg);
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }
}
