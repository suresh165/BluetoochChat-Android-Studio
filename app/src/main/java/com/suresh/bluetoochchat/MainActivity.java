package com.suresh.bluetoochchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_STATE_CHANGE =0 ;
    public static final int MESSAGE_TOAST = 4;
    public static final String TOAST = "toast";
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final String DEVICE_NAME = "devicesname";
    public static final int MESSAGE_READ =1 ;
    public static final int MESSAGE_WRITE =2 ;
    public static final int MESSAGE_STATE_CHANGED =0 ;

    private ListView listMainChat;
    private EditText edCreateMessage;
    private Button btnSendMessage;
    private ArrayAdapter<String> adapterMainChat;

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtil chatUtil;
    private final int LOCATION_PERMISSION_REQURED = 101;
    private final int SELECT_DEVICES = 102;
    private String conectedDevices;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case ChatUtil.STATE_NONE:
                            setSate("not connected");
                            break;
                        case ChatUtil. STATE_LISTEN:
                            setSate("not connected");
                            break;
                        case ChatUtil.STATE_CONNECTING:
                            setSate("Conneting.....");
                            break;
                        case ChatUtil.STATE_CONNECTED:
                            setSate("Connected"+conectedDevices);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[])msg.obj;
                    String inputBuffer = new String(buffer, 0, msg.arg1);
                    adapterMainChat.add(conectedDevices + ": " + inputBuffer);
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) msg.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    //string is not find
                    conectedDevices = msg.getData().toString();
                    Toast.makeText(context,conectedDevices,Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    //string is not find
                    Toast.makeText(context,msg.getData().toString(),Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setSate(CharSequence subtitle){
        getSupportActionBar().setSubtitle(subtitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        chatUtil = new ChatUtil(context,handler);

        init();

        initBluetooth();
    }
    //init the ui interface
    private void init(){
        listMainChat = findViewById(R.id.list_conversation);
        edCreateMessage = findViewById(R.id.ed_enter_message);
        btnSendMessage = findViewById(R.id.btn_send_msg);

        adapterMainChat = new ArrayAdapter(context,R.layout.message_layout);
    }


    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No Bluetooth found", Toast.LENGTH_SHORT).show();
            listMainChat.setAdapter(adapterMainChat);

            btnSendMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = edCreateMessage.getText().toString();
                    if (!message.isEmpty()) {
                        edCreateMessage.setText("");
                        chatUtil.write(message.getBytes());
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_search_devices:
                Cheackpermission();
                return true;
            case R.id.menu_enable_Bluetooth:
                enablebluetooth();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void Cheackpermission(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQURED);
        }else {
            Intent intent = new Intent(context,DevicelistActivity.class);
            startActivityForResult(intent,SELECT_DEVICES);
        }
    }
    //find the devices address
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode==SELECT_DEVICES && resultCode == SELECT_DEVICES){
            String address = data.getStringExtra("DevicesAddress");
            chatUtil.connect(bluetoothAdapter.getRemoteDevice(address));

            //  Toast.makeText(context, "address", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQURED) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, DevicelistActivity.class);
                startActivityForResult(intent,SELECT_DEVICES);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("location permission is required \n please grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Cheackpermission();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void enablebluetooth(){
        if (!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent Discoverintent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            Discoverintent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(Discoverintent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatUtil !=null){
            chatUtil.stope();
        }
    }
}