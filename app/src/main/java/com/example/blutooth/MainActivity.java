package com.example.blutooth;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String DEVICE_NAME ="ESP32_LED_Control",FILE_NAME="DispensedSpices.txt";
    private static final String STATUS = "Status: ",LEFT ="22",RIGHT="23",DISPENSE ="24",DISPENSE_DONE="Finished Dispensing",MOVE_LEFT="Moving Left",MOVE_RIGHT="Moving Right",MOVE_RIGHT2="Moving Right*2",DELIMITER="*";
    private static  BluetoothDevice btDevice;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int green = Color.parseColor("#00ff00");
    private static final int red = Color.parseColor("#EF1F1F");
    private static final int blue = Color.parseColor("#1F4FEF");
    private static final int DefaultColors = -20000;



    Button send,spiceDispense,spice0,spice1,spice2,dispenseBtn,gotoBtn,renameBtn;
    Button selectedBtn= null;
    BluetoothAdapter bluetoothAdapter;
    int requestCodeForEnable,numDispensed=0,counter=0;
    Intent enableBtIntent;
    IntentFilter intentFilter,disconnectFilter;
    Set<BluetoothDevice> deviceSet;
    TextView recievedView,statusView;
    EditText writeMsg;
    String msgSend,Path;
    String[] buttonNames = new String[4];

    SendRecive sendRecive;

    static final int STATE_LISTENING =1;
    static final int STATE_CONNECTEING =2;
    static final int STATE_CONNECTED =3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED =5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Path = (getApplicationContext().getFilesDir().getAbsolutePath()+"/"+FILE_NAME);
        send = (Button) findViewById(R.id.sendBtn);
        spiceDispense = (Button) findViewById(R.id.dispenseSpice);
        spice0 = (Button) findViewById(R.id.spice0);
        spice1 = (Button) findViewById(R.id.spice1);
        spice2 = (Button) findViewById(R.id.spice2);
        dispenseBtn = (Button) findViewById(R.id.dispenseBtn);
        dispenseBtn.setVisibility(View.GONE);
        gotoBtn = (Button) findViewById(R.id.goToBtn);
        gotoBtn.setVisibility(View.GONE);
        renameBtn= (Button) findViewById(R.id.renameBtn);
        renameBtn.setVisibility(View.GONE);

        writeMsg = (EditText) findViewById(R.id.editTextMsg);

        recievedView = (TextView) findViewById(R.id.textViewMsg);
        statusView = (TextView) findViewById(R.id.statusView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        disconnectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        requestCodeForEnable = 1;
//        BroadcastReceiver receiver;
//        this.registerReceiver(receiver,disconnectFilter);
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                //Device found
//            }
//            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
//                //Device is now connected
//            }
//            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                //Done searching
//            }
//            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
//                //Device is about to disconnect
//            }
//            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
//                //Device has disconnected
//            }
//        }
        initBluetooth();
        DiscoverBT();
        LoadNames();
        ClickListeners();
        // Register for broadcasts when a device is discovered.
        

    }

        Handler handler = new Handler(new Handler.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what){
                case STATE_LISTENING:
                    statusView.setText("Listening");
                    statusView.setTextColor(blue);
                    recievedView.setText("Please Wait");
                    buttonVisibility(STATE_LISTENING);
                    changeColors(STATE_LISTENING);
                    break;
                case STATE_CONNECTEING:
                    statusView.setText(STATUS + "Connecting");
                    statusView.setTextColor(blue);
                    break;
                case STATE_CONNECTED:
                    statusView.setText(STATUS + "Connected");
                    statusView.setTextColor(green);
                    break;
                case STATE_CONNECTION_FAILED:
                    statusView.setText(STATUS + "Connection failed");
                    statusView.setTextColor(red);
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tmpMsg = new String(readBuff,0,msg.arg1);
                    recievedView.setText(tmpMsg);
                    if(tmpMsg.equals(DISPENSE_DONE)){

                        Toast.makeText(getApplicationContext(),++numDispensed +" Tbsp(s) dispensed",Toast.LENGTH_SHORT).show();
                    }
                    else if(tmpMsg.equals(MOVE_LEFT)){
                        leftRotate();
                    }
                    else if(tmpMsg.equals(MOVE_RIGHT)){
                        rightRotate();
                        if(counter>0){//to move right twice
                            sendRecive.write(RIGHT.getBytes());
                            counter--;
                        }
                    }
                    else if(tmpMsg.equals(MOVE_RIGHT2)){
                        rightRotate();
                        rightRotate();
                    }
                    statusView.setText("Message Received");
                    statusView.setTextColor(green);
                    break;
            }
            return true;
        }
    });
    private class ClientClass extends Thread{

        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1){
            device=device1;
            try {
                socket= device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run(){
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendRecive = new SendRecive(socket);
                sendRecive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }
    private class SendRecive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        public final InputStream inputStream;
        private final OutputStream outputStream;

        public SendRecive (BluetoothSocket socket){
            bluetoothSocket =socket;
            InputStream tempIn = null;
            OutputStream tempOut =null;

            try {
                tempIn= bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;

        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                    if(selectedBtn.getId()==dispenseBtn.getId()){

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes){
            // If dispensing wait for a response
            toListenState();
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void ClickListeners(){

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                else if (BluetoothAdapter.ACTION_ACL_DISCONNECTED.equals(action)) {
//                    //Device has disconnected
//                }
                String string = String.valueOf(writeMsg.getText());
                sendRecive.write(string.getBytes());
            }
        });
        spiceDispense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedBtn = spiceDispense;
                changeColors(selectedBtn.getId());
                buttonVisibility(selectedBtn.getId());
            }
        });
        spice0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedBtn = spice0;
                changeColors(selectedBtn.getId());
                buttonVisibility(selectedBtn.getId());
            }
        });
        spice1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedBtn = spice1;
                changeColors(selectedBtn.getId());
                buttonVisibility(selectedBtn.getId());
            }
        });
        spice2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedBtn = spice2;
                changeColors(selectedBtn.getId());
                buttonVisibility(selectedBtn.getId());

            }
        });
        dispenseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedBtn =dispenseBtn; //I have no check in some of my functions if this happens, this could be a problem
                sendRecive.write(DISPENSE.getBytes());
            }
        });
        gotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                numDispensed=0;
                gotTo(selectedBtn.getId());
            }
        });
        renameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ensuring a button is selected before calling the function
                if(selectedBtn != null){
                    renameBtn(selectedBtn.getId());
                }
                else{
                    Toast.makeText(getApplicationContext(),"Please select a spice first",Toast.LENGTH_SHORT).show();
                }
            }
        });
        changeColors(DefaultColors);
    }
    private void showToast(String whatToPrint){
        Toast.makeText(getApplicationContext(),whatToPrint,Toast.LENGTH_SHORT).show();
    }
    private void saveToPhone(){
        String Data=createSaveString(); //define data
        File file = new File(Path);
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            File gpxfile = new File(Path);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(Data);
            writer.flush();
            writer.close();
        } catch (Exception e) { }
        showToast("Saved");


    }
    private String createSaveString(){
        String send= "";
//        The last one dont put the delimiter
        for(int i = 0;i<buttonNames.length;i++){
            if(i==(buttonNames.length-1))
                send += buttonNames[i];
            else
                send = send + buttonNames[i]+DELIMITER;
        }
        return send;
    }
    private void LoadNames(){

        File file = new File(Path);
        if(file.exists()){
            StringBuilder text = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine())!= null) {
                    text.append(line);
//                    text.append('\n');
                }
                br.close();
            } catch (IOException e) { }
            seperateNames(text.toString());
            addLabelToButtons();
        }
        else{
            String string = "Rename Spice";
            for(int i=0;i<buttonNames.length;i++){
                buttonNames[i]= string;
            }
            saveToPhone();
        }
        showToast("Correctly Loaded");
    }
    private void addLabelToButtons(){
        for(int i = 0;i<buttonNames.length;i++){
            switch(i){
                case 0:
                    spiceDispense.setText(buttonNames[i]);
                    break;
                case 1:
                    spice0.setText(buttonNames[i]);
                    break;
                case 2:
                    spice1.setText(buttonNames[i]);
                    break;
                case 3:
                    spice2.setText(buttonNames[i]);
                    break;
            }
        }

    }
    private void seperateNames(String names){
        int idx=0,from=0;
        for(int i=0;i<names.length();i++){
            if(names.charAt(i)==DELIMITER.charAt(0)&&idx==0){
                buttonNames[idx]=names.substring(from,i);
                idx++;
                from = i;
            }
            else if(names.charAt(i)==DELIMITER.charAt(0)&&(idx==1||idx==2)){
                buttonNames[idx]=names.substring(from+1,i);
                idx++;
                from = i;
            }
            if(idx==3){
                buttonNames[idx]=names.substring(from+1);
            }
        }
    }
    private void toListenState(){
        Message msg = Message.obtain();
        msg.what = STATE_LISTENING;
        handler.sendMessage(msg);
    }
    private void buttonVisibility(int btn) {
        if(btn == spiceDispense.getId()){
            dispenseBtn.setVisibility(View.VISIBLE);
            renameBtn.setVisibility(View.VISIBLE);
            gotoBtn.setVisibility(View.GONE);
        }
        else if(btn ==STATE_LISTENING){
            dispenseBtn.setVisibility(View.GONE);
            renameBtn.setVisibility(View.GONE);
            gotoBtn.setVisibility(View.GONE);
        }
        else{

            dispenseBtn.setVisibility(View.GONE);
            renameBtn.setVisibility(View.VISIBLE);
            gotoBtn.setVisibility(View.VISIBLE);
        }
    }

    private void gotTo(int btn) {
        String tmp=null;
        if(btn == spice0.getId()){
            leftRotate();
//            sendRecive.write(LEFT.getBytes());
        }
        else if(btn == spice1.getId()){
            sendRecive.write(RIGHT.getBytes());
            counter=1;
        }
        else if(btn == spice2.getId()){
            sendRecive.write(RIGHT.getBytes());
        }
    }
    private void rightRotate(){
        String tmp = new String();
        for(int i=0; i<buttonNames.length;i++){
            tmp=buttonNames[(i+1)%4];
            buttonNames[(i+1)%4]=buttonNames[0];
            buttonNames[0]=tmp;
        }
        saveToPhone();
        addLabelToButtons();
    }
    private void leftRotate(){
        String tmp = new String();
        for(int i=buttonNames.length-1; i>0;i--){
            if(i == 0) {
                tmp=buttonNames[0];
                buttonNames[0]=buttonNames[3];
            }
            else{
                tmp=buttonNames[(i-1)];
                buttonNames[(i-1)]=buttonNames[3];
            }
            buttonNames[3]=tmp;
        }
        saveToPhone();
        addLabelToButtons();
    }

    private void renameBtn(int currBtn){
        String string = String.valueOf(writeMsg.getText());
        if(string.equals("")){
            Toast.makeText(getApplicationContext(),"Type in the send message box",Toast.LENGTH_SHORT).show();
            return;
        }
        if(currBtn == spiceDispense.getId()){
            spiceDispense.setText(string);
            buttonNames[0]=string;
            writeMsg.getText().clear();
        }
        else if(currBtn == spice0.getId()){
            spice0.setText(string);
            buttonNames[1]=string;
            writeMsg.getText().clear();
        }
        else if(currBtn == spice1.getId()){
            spice1.setText(string);
            buttonNames[2]=string;
            writeMsg.getText().clear();
        }
        else if(currBtn == spice2.getId()){
            spice2.setText(string);
            buttonNames[3]=string;
            writeMsg.getText().clear();
        }
        saveToPhone();
    }
    private void changeColors(int currBut){
        if(currBut == spiceDispense.getId()){
            spiceDispense.setBackgroundResource(R.drawable.dispenseclicked);
            spice0.setBackgroundResource(R.drawable.circle);
            spice1.setBackgroundResource(R.drawable.circle);
            spice2.setBackgroundResource(R.drawable.circle);
        }
        else if(currBut == spice0.getId()){
            spiceDispense.setBackgroundResource(R.drawable.dispense);
            spice0.setBackgroundResource(R.drawable.circleclicked);
            spice1.setBackgroundResource(R.drawable.circle);
            spice2.setBackgroundResource(R.drawable.circle);
        }
        else if(currBut == spice1.getId()){
            spiceDispense.setBackgroundResource(R.drawable.dispense);
            spice0.setBackgroundResource(R.drawable.circle);
            spice1.setBackgroundResource(R.drawable.circleclicked);
            spice2.setBackgroundResource(R.drawable.circle);
        }
        else if(currBut == spice2.getId()){
            spiceDispense.setBackgroundResource(R.drawable.dispense);
            spice0.setBackgroundResource(R.drawable.circle);
            spice1.setBackgroundResource(R.drawable.circle);
            spice2.setBackgroundResource(R.drawable.circleclicked);
        }
        else {
            spiceDispense.setBackgroundResource(R.drawable.dispense);
            spice0.setBackgroundResource(R.drawable.circle);
            spice1.setBackgroundResource(R.drawable.circle);
            spice2.setBackgroundResource(R.drawable.circle);
        }

    }
    private void DiscoverBT() {

        // Getting paired list
        deviceSet = bluetoothAdapter.getBondedDevices();
        int index = 0;
        if(deviceSet.size()>0){
            for(BluetoothDevice device:deviceSet){
                String tmp = device.getName();
                if(tmp.equals(DEVICE_NAME)){
                    btDevice = device;

                    ClientClass clientClass = new ClientClass(btDevice);
                    clientClass.start();

                    statusView.setText("Connecting");
                    break;
                }
            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == requestCodeForEnable) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth is enabled", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth enabling was cancelled", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initBluetooth(){

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Bluetooth not supported",Toast.LENGTH_LONG).show();
        }
        else {

            if (!bluetoothAdapter.isEnabled()) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(receiver, filter);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(receiver);
    }
}