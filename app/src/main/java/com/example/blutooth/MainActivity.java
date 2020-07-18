package com.example.blutooth;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.speech.RecognizerIntent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String DEVICE_NAME ="ESP32",FILE_NAME="DispensedSpices.txt";

    private static final String STATUS = "Status: ",LEFT ="1",RIGHT="2",DISPENSE ="3",DISPENSE_DONE="Finished Dispensing",MOVE_LEFT="Moving Left",MOVE_RIGHT="Moving Right",STOPPED="Stopped",
            MOVE_RIGHT2="Moving Right*2",DELIMITER="*",STOP="7",DEFAULT_BUTTON_NAME="Rename",SAVE="25",LOAD="26",EMPTY= "EPROM is empty",SAVED ="Saved";
    private static  BluetoothDevice btDevice;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int green = Color.parseColor("#00ff00");
    private static final int red = Color.parseColor("#EF1F1F");
    private static final int blue = Color.parseColor("#1F4FEF");
    private static final int DefaultColors = -20000;


    boolean loading = false, connected=false, saving =false;

    Button send,spiceDispense,spice0,spice1,spice2,dispenseBtn,gotoBtn,renameBtn,reconnectBtn,eStopBtn,speechBtn,recipeBtn;
    Button[] buttonOrder;
    Button selectedBtn= null;
    boolean busy= false,isAuto=false;
    BluetoothAdapter bluetoothAdapter;
    int requestCodeForEnable,numDispensed=0,counter=0,moreToDispense=0;
    Intent enableBtIntent;
    IntentFilter intentFilter,disconnectFilter;
    Set<BluetoothDevice> deviceSet;
    TextView recievedView,statusView;
    EditText writeMsg;
    String msgSend,sendingString,Path;
    String[] buttonNames = new String[4];
    public ArrayList<String> spiceQueue;
    SpiceIndexSaver[] spiceIndexSaver;
    SendRecive sendRecive;

    static final int STATE_LISTENING =1;
    static final int STATE_CONNECTEING =2;
    static final int STATE_CONNECTED =3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED =5;
    static final int STANDARD_CONNECTION = (-25);
    static final int REQ_CODE_SPEECH_OUTPUT = 143;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Path = (getApplicationContext().getFilesDir().getAbsolutePath()+"/"+FILE_NAME);
        send = (Button) findViewById(R.id.sendBtn);
        send.setVisibility(View.GONE);
        spiceDispense = (Button) findViewById(R.id.dispenseSpice);
        spice0 = (Button) findViewById(R.id.spice0);
        spice1 = (Button) findViewById(R.id.spice1);
        spice2 = (Button) findViewById(R.id.spice2);
        recipeBtn = (Button) findViewById(R.id.recipeBtn);
        recipeBtn.setVisibility(View.GONE);
        eStopBtn = (Button) findViewById(R.id.eStopBtn);
        eStopBtn.setVisibility(View.GONE);
        dispenseBtn = (Button) findViewById(R.id.dispenseBtn);
        dispenseBtn.setVisibility(View.GONE);
        reconnectBtn = (Button) findViewById(R.id.reconnectBtn);
        reconnectBtn.setVisibility(View.GONE);
        gotoBtn = (Button) findViewById(R.id.goToBtn);
        gotoBtn.setVisibility(View.GONE);
        renameBtn= (Button) findViewById(R.id.renameBtn);
        renameBtn.setVisibility(View.GONE);
        speechBtn = (Button) findViewById(R.id.useSpeechBtn);
        speechBtn.setVisibility(View.GONE);
        buttonOrder = new Button[]{spiceDispense, spice0, spice1, spice2};

        writeMsg = (EditText) findViewById(R.id.editTextMsg);
        writeMsg.setVisibility(View.GONE);

        recievedView = (TextView) findViewById(R.id.textViewMsg);
        statusView = (TextView) findViewById(R.id.statusView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        spiceQueue = new ArrayList<>();
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        disconnectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        spiceIndexSaver = new SpiceIndexSaver[4];
        for(int i=0;i<spiceIndexSaver.length;i++)
            spiceIndexSaver[i]= new SpiceIndexSaver();

        requestCodeForEnable = 1;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

        initBluetooth();
        DiscoverBT();
        ClickListeners();
        // Register for broadcasts when a device is discovered.
        

    }

    private void ClickListeners(){

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if(busy){
                //     showToast("Please wait for process to complete");
                // }else{
                //     String string = String.valueOf(writeMsg.getText());
                //     sendRecive.write(string.getBytes());
                // }
                loadNames();
            }
        });
        recipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,SpiceAPI.class);
                startActivity(intent);
            }
        });
        eStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRecive.write(STOP.getBytes());
            }
        });
        speechBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turnOnMic();
            }
        });
        spiceDispense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(busy){
                    showToast("Please wait for process to complete");
                }else {
                    selectedBtn = spiceDispense;
                    changeColors(selectedBtn.getId());
                    buttonVisibility(selectedBtn.getId());
                }
            }
        });
        spice0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(busy){
                    showToast("Please wait for process to complete");
                }else {
                    selectedBtn = spice0;
                    changeColors(selectedBtn.getId());
                    buttonVisibility(selectedBtn.getId());
                }
            }
        });
        spice1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(busy){
                    showToast("Please wait for process to complete");
                }else {
                    selectedBtn = spice1;
                    changeColors(selectedBtn.getId());
                    buttonVisibility(selectedBtn.getId());
                }
            }
        });
        spice2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(busy){
                    showToast("Please wait for process to complete");
                }else {
                    selectedBtn = spice2;
                    changeColors(selectedBtn.getId());
                    buttonVisibility(selectedBtn.getId());
                }

            }
        });
        dispenseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedBtn = dispenseBtn; //I have no check in some of my functions if this happens, this could be a problem
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
        reconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initBluetooth();
                DiscoverBT();
            }
        });
        changeColors(DefaultColors);
    }
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//           ... //Device found
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
//           ... //Device is now connected
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//           ... //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
//           ... //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
                busy = true;
            }
        }
    };
    Runnable loadnamesDelay = new Runnable() {
        @Override
        public void run(){
            loadNames();
        }
    };


    Handler h = new Handler();

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
                    busy=true;
                    break;
                case STATE_CONNECTEING:
                    statusView.setText(STATUS + "Connecting");
                    statusView.setTextColor(blue);
                    busy = true;
                    break;
                case STATE_CONNECTED:
                    statusView.setText(STATUS + "Connected");
                    statusView.setTextColor(green);
                    buttonVisibility(STATE_LISTENING);
                    h.postDelayed(loadnamesDelay,1000);
                    busy = false;
                    break;
                case STATE_CONNECTION_FAILED:
                    statusView.setText(STATUS + "Connection failed");
                    statusView.setTextColor(red);
                    busy = true;
                    buttonVisibility(reconnectBtn.getId());
                    changeColors(DefaultColors);
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tmpMsg = new String(readBuff,0,msg.arg1);

                    if(!tmpMsg.contains(DELIMITER) && !tmpMsg.equals(EMPTY)){
                        recievedView.setText(tmpMsg);
                    }
//                    This is only done when there is confirmation that the spice has been dispensed
                    if(tmpMsg.equals(DISPENSE_DONE)){
                        numDispensed++;
                        int tableSpoons = numDispensed/3;
                        Toast.makeText(getApplicationContext(),"Tbsp(s) "+tableSpoons+" and " + numDispensed%3 +" Tsp(s) dispensed",Toast.LENGTH_SHORT).show();
                        if(moreToDispense>0){// if more need to be dispensed do it
                            moreToDispense--;
                            sendRecive.write(DISPENSE.getBytes());
                        }
                        else if(!spiceQueue.isEmpty()){//keep requesting spice amounts until there are none left
                            processWords(spiceQueue.remove(0));
                        }
                    }
                    if(tmpMsg.equals(SAVED)){
                        saving = false;
                    }
                    if (tmpMsg.equals("Ready")) {
                        sendRecive.write(sendingString.getBytes());
                    }

                    if (loading && !tmpMsg.equals(EMPTY)) {
                        seperateNames(tmpMsg);
                        addLabelToButtons();
                        loading = false;
                    } else if (loading && tmpMsg.equals(EMPTY)) {
                        String string = "";
                        for (int i = 0; i < spiceIndexSaver.length; i++) {
                            spiceIndexSaver[i].name = DEFAULT_BUTTON_NAME;// this will have to be seperate strings
                            spiceIndexSaver[i].currIdx = i;
                            spiceIndexSaver[i].startIdx = i;
                        }
                        addLabelToButtons();
                        loading = false;
                    }
                    if (tmpMsg.equals(MOVE_LEFT)) {
                        leftRotate();
                        if (isAuto) {
                            sendRecive.write(DISPENSE.getBytes());
                            isAuto = false;
                        }
                    } else if (tmpMsg.equals(MOVE_RIGHT)) {
                        rightRotate();//this is the problem
                        if (counter > 0) {//to move right twice
                            sendRecive.write(RIGHT.getBytes());
                            counter--;
                        } else if (counter <= 0 && isAuto) {
                            sendRecive.write(DISPENSE.getBytes());
                            isAuto = false;
                        }
                    } else if (tmpMsg.equals(MOVE_RIGHT2)) {
                        rightRotate();
                        rightRotate();
                    }

                    if(!loading && (tmpMsg.contains(DELIMITER) || tmpMsg.contains(EMPTY)) ){
                        statusView.setText(STATUS + "Connected");
                        statusView.setTextColor(green);
                        recievedView.setText("Initial State Loaded");
                        buttonVisibility(STATE_LISTENING);
                        busy=false;
                    }
                    else if(!loading && !tmpMsg.contains(DELIMITER)){
                        statusView.setText(STATUS+"Message Received");
                        statusView.setTextColor(green);
                        buttonVisibility(STATE_LISTENING);
                        busy=false;
                    }
                    
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

                connected=true;

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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes){
            toListenState();
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void showToast(String whatToPrint){
        Toast.makeText(getApplicationContext(),whatToPrint,Toast.LENGTH_SHORT).show();
    }
    private void turnOnMic(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say spice to dispense and how much(1-9)");

        try{
            startActivityForResult(intent, REQ_CODE_SPEECH_OUTPUT);
        }catch(ActivityNotFoundException tim){
            showToast("Google mic isn't open");
        }
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
        for(int i=0;i<spiceIndexSaver.length;i++){
            if(i!=spiceIndexSaver.length-1){
                send += spiceIndexSaver[i].name+DELIMITER;
            }
            else {
                send = send + spiceIndexSaver[i].name;
            }
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
            String string = "Rename";
            for(int i=0;i<spiceIndexSaver.length;i++){
                spiceIndexSaver[i].name= string;
            }
            saveNames();
        }
        showToast("Correctly Loaded");
    }
    private void addLabelToButtons(){
        for(int i = 0;i<spiceIndexSaver.length;i++){//make sure that the index the spice should be in is set appropriately
            if(spiceIndexSaver[i].currIdx==0){
                buttonOrder[0].setText(spiceIndexSaver[i].name);
            }
            else if(spiceIndexSaver[i].currIdx==1){
                buttonOrder[1].setText(spiceIndexSaver[i].name);
            }
            else if(spiceIndexSaver[i].currIdx==2){
                buttonOrder[2].setText(spiceIndexSaver[i].name);
            }
            else if(spiceIndexSaver[i].currIdx==3){
                spice2.setText(spiceIndexSaver[i].name);//when i use spice
            }
        }


    }
    private void seperateNames(String names){
        int idx=0,from=0;
        for(int i=0;i<names.length();i++){
            if(names.charAt(i)==DELIMITER.charAt(0)&&idx==0){
                spiceIndexSaver[idx].name=names.substring(from,i);
                spiceIndexSaver[idx].currIdx=idx;
                idx++;
                from = i;
            }
            else if(names.charAt(i)==DELIMITER.charAt(0)&&(idx==1||idx==2)){
                spiceIndexSaver[idx].name=names.substring(from+1,i);
                spiceIndexSaver[idx].currIdx=idx;
                idx++;
                from = i;
            }
            if(idx==3){
                spiceIndexSaver[idx].name=names.substring(from+1);
                spiceIndexSaver[idx].currIdx=idx;
                break;
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
            speechBtn.setVisibility(View.VISIBLE);
            writeMsg.setVisibility(View.GONE);
            gotoBtn.setVisibility(View.GONE);
        }
        else if(btn ==STATE_LISTENING){
            dispenseBtn.setVisibility(View.GONE);
            renameBtn.setVisibility(View.GONE);
            gotoBtn.setVisibility(View.GONE);
            speechBtn.setVisibility(View.GONE);
            reconnectBtn.setVisibility(View.GONE);
            writeMsg.setVisibility(View.GONE);
            recipeBtn.setVisibility(View.GONE);

        }
        else if(btn== STANDARD_CONNECTION){
            dispenseBtn.setVisibility(View.GONE);
            renameBtn.setVisibility(View.GONE);
            gotoBtn.setVisibility(View.GONE);
            speechBtn.setVisibility(View.GONE);
            reconnectBtn.setVisibility(View.GONE);
            writeMsg.setVisibility(View.GONE);
            recipeBtn.setVisibility(View.VISIBLE);
        }
        else if(btn == reconnectBtn.getId()){
            dispenseBtn.setVisibility(View.GONE);
            renameBtn.setVisibility(View.GONE);
            gotoBtn.setVisibility(View.GONE);
            speechBtn.setVisibility(View.GONE);
            writeMsg.setVisibility(View.GONE);
            writeMsg.setVisibility(View.GONE);
            recipeBtn.setVisibility(View.GONE);
            reconnectBtn.setVisibility(View.VISIBLE);
        }
        else{

            dispenseBtn.setVisibility(View.GONE);
            renameBtn.setVisibility(View.VISIBLE);
            gotoBtn.setVisibility(View.VISIBLE);
            speechBtn.setVisibility(View.VISIBLE);
            writeMsg.setVisibility(View.GONE);
        }
    }

    private void gotTo(int btn) {
        String tmp=null;
        if(btn == spice0.getId()){
            sendRecive.write(LEFT.getBytes());
        }
        else if(btn == spice1.getId()){
            sendRecive.write(RIGHT.getBytes());
            counter=1;
        }
        else if(btn == spice2.getId()){
            sendRecive.write(RIGHT.getBytes());
        }
    }
    private void rightRotate(){//logic is backwards
        numDispensed=0;
        String tmp = new String();
        for(int i=0; i<spiceIndexSaver.length;i++){
            spiceIndexSaver[i].currIdx=(spiceIndexSaver[i].currIdx+1)%4;
        }
        addLabelToButtons();
    }
    private void leftRotate(){
        numDispensed=0;
        String tmp = new String();
        for(int i=spiceIndexSaver.length-1; i>=0;i--){
            if(i == 0) {
                spiceIndexSaver[i].currIdx=3;
            }
            else{
                spiceIndexSaver[i].currIdx-=1;
            }
        }
        addLabelToButtons();
    }
    private SpiceIndexSaver findSpiceIdxFinderWithcurrentIdx(int indexToFind){
        SpiceIndexSaver spiceIndexSavertoReturn= new SpiceIndexSaver();
        for(int i=0;i<spiceIndexSaver.length;i++){
            if(indexToFind==spiceIndexSaver[i].startIdx){
                spiceIndexSavertoReturn =spiceIndexSaver[i];
            }
        }
        return spiceIndexSavertoReturn;
    }

    private void renameBtn(int currBtn){
        writeMsg.setVisibility(View.VISIBLE);
        String string = String.valueOf(writeMsg.getText());
        if(string.equals("")){
            Toast.makeText(getApplicationContext(),"Type name in message box",Toast.LENGTH_SHORT).show();
            return;
        }
        if(currBtn == spiceDispense.getId()){
            spiceDispense.setText(string);
            findSpiceIdxFinderWithcurrentIdx(0).name=string;
            writeMsg.getText().clear();
        }
        else if(currBtn == spice0.getId()){
            spice0.setText(string);
            findSpiceIdxFinderWithcurrentIdx(1).name=string;
            writeMsg.getText().clear();
        }
        else if(currBtn == spice1.getId()){
            spice1.setText(string);
            findSpiceIdxFinderWithcurrentIdx(2).name=string;
            writeMsg.getText().clear();
        }
        else if(currBtn == spice2.getId()){
            spice2.setText(string);
            findSpiceIdxFinderWithcurrentIdx(3).name=string;
            writeMsg.getText().clear();
        }
        saveNames();
        buttonVisibility(STATE_LISTENING);
        changeColors(DefaultColors);
        // saveToPhone();
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
    private int getNumber(String words){
        int ret = 1;
        for (int i = 0; i < words.length(); i++) {// getting the number in the string
            char ch = words.charAt(i);
            if(Character.isDigit(ch)) {
                ret = Character.getNumericValue(ch);
                break;
            }
        }
        if(words.toLowerCase().contains("two")){
            ret =2;
        }
        else if(words.toLowerCase().contains("three")){
            ret =3;
        }
        else if(words.toLowerCase().contains("four")){
            ret =4;
        }
        else if(words.toLowerCase().contains("five")){
            ret =5;
        }
        else if(words.toLowerCase().contains("six")){
            ret =6;
        }
        else if(words.toLowerCase().contains("seven")){
            ret =7;
        }
        else if(words.toLowerCase().contains("eight")){
            ret =8;
        }
        else if(words.toLowerCase().contains("nine")){
            ret =9;
        }
        return ret;
    }
    public void processWords(String words){

        String[] toGetIngredients = words.split("and");
        for(int i =1; i<toGetIngredients.length;i++)// adding the rest of ingredients to the spice queue
            spiceQueue.add(toGetIngredients[i]);
        words = toGetIngredients[0];
        int num = getNumber(words);

        if(words.contains("tablespoons")||words.contains("tbsps")||words.contains("tablespoon")||words.contains("tbsp")){// do conversion for teaspoon to tablespoon
            num *=3;
        }
        words = words.toUpperCase();
        SpiceNameIdx[] spiceNameIdxes = new SpiceNameIdx[4];
        PriorityQueue<SpiceNameIdx> priorityQueue = new PriorityQueue<>();
        for(int i=0; i<spiceIndexSaver.length;i++){
            spiceNameIdxes[i]= new SpiceNameIdx(i,spiceIndexSaver[i].name);
            priorityQueue.add(new SpiceNameIdx(i,spiceIndexSaver[i].name));
        }
        for(int i=spiceNameIdxes.length-1;!priorityQueue.isEmpty();i--)
            spiceNameIdxes[i]=priorityQueue.poll();

        for(int i =0; i< spiceIndexSaver.length;i++){
            if(words.contains(spiceNameIdxes[i].name.toUpperCase())){//if the text has a spice dispense it or go to it then dispense it
                if(spiceNameIdxes[i].idx==0){
                    moreToDispense=num-1;
                    sendRecive.write(DISPENSE.getBytes());
                }
                else if(spiceNameIdxes[i].idx!=0){
                    isAuto= true;
                    moreToDispense=num-1;
                    gotTo(buttonOrder[spiceNameIdxes[i].idx].getId());
                }
                break;
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
        if(requestCode== REQ_CODE_SPEECH_OUTPUT){
            if(resultCode == RESULT_OK && data!=null){
                ArrayList<String> voiceInText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                processWords(voiceInText.get(0));
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
    private void loadNames(){
        loading=true;
        sendRecive.write(LOAD.getBytes());
        //waiting till data is received
    }
    private void saveNames(){
        sendingString= createSaveString();
        sendRecive.write(SAVE.getBytes());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(receiver);
    }
}
class SpiceNameIdx implements Comparable<SpiceNameIdx>{
    String name;
    int idx;
    SpiceNameIdx(int idxes, String names){
        idx = idxes;
        name = names;

    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int compareTo(SpiceNameIdx spiceNameIdx) {
        return Integer.compare(name.length(),spiceNameIdx.name.length());
    }
}
class SpiceIndexSaver{
    String name;
    int currIdx,startIdx;
    SpiceIndexSaver(String Name,int currIdxx){
        name=Name;
        currIdx= currIdxx;
    }
    SpiceIndexSaver(){}
}