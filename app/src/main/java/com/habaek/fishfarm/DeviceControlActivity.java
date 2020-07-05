package com.habaek.fishfarm;

/**
 * Created by 박용주 on 2015-09-01.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.RangeCategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.net.FileNameMap;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName ;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    static final int PICK_DEVICE_REQUEST = 0;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    //public final static UUID RX_DATA = UUID.fromString(GattAttributes.RX_DATA);
    //public final static UUID TX_DATA = UUID.fromString(GattAttributes.TX_DATA);

    public static final String TOAST = "toast";
    private Button Master_Check;

    //onClickListener 에 넣어야하지않을까?
    private EditText InputFile_name;
    private Button BTN_readFile;
    private Button BTN_motor;
    private Button BTN_record;

    private Button BTN_3sec;
    private Button BTN_5sec;
    private Button BTN_10sec;

    private Button BTN_offset;
    private EditText Input_temp;
    private EditText Input_chl;
    private EditText Input_chlRing;
    private EditText Input_bga;
    private EditText Input_bgaRing;

    private Button BTN_sync;

    private TextView ReloadFile;
    private TextView CHL;
    private TextView BGA;
    private TextView Temp;
    private TextView CHL_RingBuf;
    private TextView BGA_RingBuf;

    private float temp;
    private float chl;
    private float chlRing;
    private float bga;
    private float bgaRing;

    private double graph_temp;
    private double graph_chl;

    private boolean check_sync = false;

    private int cycle = 3000;

    private boolean button = false;

    public byte[] data_send = new byte[]{(byte) 0x00};
    public boolean flag = false;

    private String SDpath = Environment.getExternalStorageDirectory().getAbsolutePath();

    //파일 입출력 관련 변수 처리
    int btn_count = 0;
    //String record_data = "";
    String Record_data = "";
    String record_Data_bga = "";
    String record_Data_bga_ring = "";
    String record_Data_chl_ring = "";
    String record_Data_temp = "";
    String record_Data_chl = "";

    protected GraphicalView mChartView;


    // 실시간 받아오기
    String strNow = "";

    private int cnt = 0;

    private final Handler handler = new Handler();
    private static final long SEND_COMMEND = 1000;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_RAW);
                //String data_string = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_STRING);
                //String uuid_data = intent.getStringExtra(BluetoothLeService.UUID_STRING);

                /*if(data[0] == 0x5a) {
                    Log.d(TAG, "return sync data : " + data[0]);
                    check_sync = true;
                    Toast.makeText(getApplicationContext(), "receive sync data : " + data[0],
                            Toast.LENGTH_SHORT).show();
                }   // 0x5a : 'Z'
                else {
                    Log.d(TAG, "send data : " + data[0]);
                    displayData(data);
                }*/

                displayData(data);

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //���񽺰� ������ �������̽��� �����Ѵ�.
                discoveredGattServices(mBluetoothLeService.getSupportedGattServices());
                discoveredGattServices2(mBluetoothLeService.getSupportedGattServices());
                findCharacteristic();
                //setBLEstate();
            }
        }
    };

    public void findCharacteristic() {

        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();

        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices)
        {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
            {
                uuid = gattCharacteristic.getUuid().toString();
                if (GattAttributes.RX_DATA.equals(uuid)){characteristicRX = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);// Notification on ����
                }
                if (GattAttributes.TX_DATA.equals(uuid)) characteristicTX = gattCharacteristic;
            }
        }
    }

    private void clearUI() {
        //mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristic);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //handler_commend = new Handler();

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        TabHost tabHost = (TabHost)findViewById(R.id.tab_host);
        tabHost.setup();

        // Tab1 Setting
        TabHost.TabSpec tabSpec1 = tabHost.newTabSpec("Tab1");
        tabSpec1.setIndicator("Display"); // Tab Subject
        tabSpec1.setContent(R.id.Display); // Tab Content
        tabHost.addTab(tabSpec1);

        // Tab2 Setting
        TabHost.TabSpec tabSpec2 = tabHost.newTabSpec("Tab2");
        tabSpec2.setIndicator("FileOut"); // Tab Subject
        tabSpec2.setContent(R.id.FileOut); // Tab Contentep
        tabHost.addTab(tabSpec2);

        // Tab3 Setting
        TabHost.TabSpec tabSpec3 = tabHost.newTabSpec("Tab3");
        tabSpec3.setIndicator("Graph"); // Tab Subject
        tabSpec3.setContent(R.id.Graph); // Tab Content
        tabHost.addTab(tabSpec3);

        // Tab4 Setting
        TabHost.TabSpec tabSpec4 = tabHost.newTabSpec("Tab4");
        tabSpec4.setIndicator("Setting"); // Tab Subject

        tabSpec4.setContent(R.id.Setting); // Tab Content
        tabHost.addTab(tabSpec4);

        // show First Tab Content
        tabHost.setCurrentTab(0);

        //dataSend(check);

        /*temp_c[0] = (byte) 0x3f;
        characteristicTX.setValue(temp_c);
        mBluetoothLeService.writeCharacteristic(characteristicTX, temp_c);*/

        // 실시간 받아오기
        /*long now_time = System.currentTimeMillis();
        Date date = new Date(now_time);

        SimpleDateFormat sdfNow = new SimpleDateFormat("hh:mm");
        strNow = sdfNow.format(date);*/

        //그래프 관련
        //그래프 값 받기
        double[] minValues = new double[] {0, 0, 0, 0, 0, 0, 0};
        double[] D = new double[7];
        List<double[]> values = new ArrayList<double[]>();

        //데이터 받아오기
        for(int i = 0; i < 7; i++){
            D[i] = graph_temp;
        }

        values.add(new double[] {D[0], D[1], D[2], D[3], D[4], D[5], D[6]});

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        RangeCategorySeries series = new RangeCategorySeries("");
        int length = minValues.length;

        for(int k = 0; k < length; k++) series.add(minValues[k], getMonthData[k]);

        dataset.addSeries(series.toXYSeries());

        int[] colors = new int[] { Color.BLACK };
        XYMultipleSeriesRenderer renderer = buildBarRenderer(colors);
        setChartSettings(renderer, "", "", "", 0.5, 12.5, -30, 45, Color.WHITE, Color.WHITE);

        renderer.setBarSpacing(0.5);
        renderer.setXLabels(0);
        renderer.setYLabels(10);
        renderer.setShowGrid(true);
        renderer.setGridColor(Color.parseColor("#c9c9c9"));
        renderer.setMarginsColor(Color.parseColor("#FFFFFF"));
        renderer.setAxesColor(Color.WHITE);
        renderer.setBackgroundColor(Color.WHITE);
        renderer.setZoomEnabled(false, false);
        renderer.setShowAxes(false);
        renderer.setShowLegend(false);
        renderer.setPanEnabled(false, false);
        renderer.setMargins(new int[] {0, 0, 0, 0});
        renderer.setYLabelsAlign(Paint.Align.RIGHT);

        SimpleSeriesRenderer r = renderer.getSeriesRendererAt(0);
        r.setDisplayChartValues(true);

        r.setChartValuesTextSize(12);
        r.setChartValuesSpacing(3);
        r.setGradientEnabled(true);
        r.setGradientStart(40000, Color.parseColor("#e71d73"));
        r.setGradientStop(45000, Color.parseColor("#ffffff"));
        LinearLayout chart_area = (LinearLayout) findViewById(R.id.chart_area);

        mChartView =
                ChartFactory.getRangeBarChartView(this, dataset, renderer, BarChart.Type.STACKED);
        chart_area.addView(mChartView);





        // Graph
        // 표시할 수치값
        //List<double[]> values = new ArrayList<double[]>();
        /*double[] values = new double[] { 14230, 12300, 14240, 15244, 15900, 19200,
                22030, 21200, 19500, 15500, 12600, 14000 };

        // 그래프 출력을 위한 그래픽 속성 지정객체
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

        // 상단 표시 제목과 글자 크기
        renderer.setChartTitle("2011년도 판매량");
        renderer.setChartTitleTextSize(20);

        // 분류에 대한 이름
        String[] titles = new String[] { "월별 판매량" };

        // 항목을 표시하는데 사용될 색상값
        int[] colors = new int[] { Color.YELLOW };

        // 분류명 글자 크기 및 각 색상 지정
        renderer.setLegendTextSize(15);
        int length = colors.length;
        for (int i = 0; i < length; i++) {
            SimpleSeriesRenderer r = new SimpleSeriesRenderer();
            r.setColor(colors[i]);
            renderer.addSeriesRenderer(r);
        }

        // X,Y축 항목이름과 글자 크기
        renderer.setXTitle("월");
        renderer.setYTitle("판매량");
        renderer.setAxisTitleTextSize(12);

        // 수치값 글자 크기 / X축 최소,최대값 / Y축 최소,최대값
        renderer.setLabelsTextSize(10);
        renderer.setXAxisMin(0.5);
        renderer.setXAxisMax(12.5);
        renderer.setYAxisMin(0);
        renderer.setYAxisMax(24000);

        // X,Y축 라인 색상
        renderer.setAxesColor(Color.WHITE);
        // 상단제목, X,Y축 제목, 수치값의 글자 색상
        renderer.setLabelsColor(Color.CYAN);

        // X축의 표시 간격
        renderer.setXLabels(12);
        // Y축의 표시 간격
        renderer.setYLabels(5);

        // X,Y축 정렬방향
        renderer.setXLabelsAlign(Paint.Align.LEFT);
        renderer.setYLabelsAlign(Paint.Align.LEFT);
        // X,Y축 스크롤 여부 ON/OFF
        renderer.setPanEnabled(false, false);
        // ZOOM기능 ON/OFF
        renderer.setZoomEnabled(false, false);
        // ZOOM 비율
        renderer.setZoomRate(1.0f);
        // 막대간 간격
        renderer.setBarSpacing(0.5f);


        double[] v = new double[]{};
        // 설정 정보 설정
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        for (int i = 0; i < titles.length; i++) {
            CategorySeries series = new CategorySeries(titles[i]);
            v[i] = i;
            int seriesLength = v.length;
            for (int k = 0; k < seriesLength; k++) {
                series.add(v[k]);
            }
            dataset.addSeries(series.toXYSeries());
        }

        // 그래프 객체 생성
        GraphicalView gv = ChartFactory.getBarChartView(this, dataset, renderer, BarChart.Type.STACKED);

        // 그래프를 LinearLayout에 추가
        RelativeLayout llBody = (RelativeLayout) findViewById(R.id.Graph);
        llBody.addView(gv);*/



        Temp = (TextView) findViewById(R.id.editText_temp);
        BGA = (TextView) findViewById(R.id.textView_bga_2);
        CHL = (TextView) findViewById(R.id.editText_depth);
        CHL_RingBuf = (TextView) findViewById(R.id.textView_chl_RingBuf_num);
        BGA_RingBuf = (TextView) findViewById(R.id.textView_bga_RingBuf_num);

        /*BTN_sync = (Button) findViewById(R.id.button_sync);
        BTN_sync.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    // '?'보내면 센서에서 '!'를 보냄

                    // 'Y' -> 'Z'
                    byte[] buf = new byte[] {(byte) 0x59};
                    characteristicTX.setValue(buf);
                    mBluetoothLeService.writeCharacteristic(characteristicTX, buf);
                    Log.d(TAG, "send data : " + buf[0]);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(check_sync) Toast.makeText(getApplicationContext(), "Sync Complete", Toast.LENGTH_SHORT).show();

                            Log.d(TAG, "check_sync : " + check_sync);
                        }
                    }, cycle);

                } catch (Exception e) {
                    Log.e(TAG, "Sync fail");
                    Toast.makeText(getApplicationContext(), "Sync fail",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });*/

        BTN_motor = (Button) findViewById(R.id.button_motor);
        BTN_motor.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                data_send[0] = (byte) 0x57;

                characteristicTX.setValue(data_send);
                mBluetoothLeService.writeCharacteristic(characteristicTX, data_send);
                mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
            }
        });

        //Master ID Check
        Master_Check = (Button) findViewById(R.id.btn_master);
        Master_Check.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View b) {
                try {
                    byte[] buf = new byte[] {(byte) 0x31};
                    characteristicTX.setValue(buf);
                    mBluetoothLeService.writeCharacteristic(characteristicTX, buf);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);

                    Log.d(TAG, "Master 호출완료");
                    Log.d(TAG, "buf : " + buf[0] + "\n" + buf);
                    Toast.makeText(getApplicationContext(), "sensing loop start!",
                            Toast.LENGTH_SHORT).show();
                    /*Toast.makeText(getApplicationContext(), "전송완료 되었습니다. " + buf[0],
                            Toast.LENGTH_SHORT).show();*/

                } catch (Exception e) {
                    Log.e(TAG, "확인 실패! 종료 후 다시 시작 해주세요");
                    Toast.makeText(getApplicationContext(), "확인 실패! 종료 후 다시 시작 해주세요",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        InputFile_name = (EditText) findViewById(R.id.editText_FileName);

        // recording button active Record 버튼으로 파일 저장하는 내용
        BTN_record = (Button) findViewById(R.id.button_record);
            View.OnClickListener listener2 = new View.OnClickListener(){

            @Override
            public void onClick(View a){

                try {
                    btn_count = btn_count + 1;

                    String FileName = InputFile_name.getText().toString();

                    if(btn_count == 1){
                        Toast.makeText(getApplicationContext(), "Recording Start", Toast.LENGTH_SHORT).show();

                        Record_data = "Time,Temp,CHL,CHLRing,BGA,BGARing\n";

                    } else if(btn_count == 2){

                        //파일 생성 및 쓰기 성공
                        File dir = new File(SDpath, "recordFile");
                        dir.mkdir();
                        File file = new File(dir, FileName + ".csv");
                        FileOutputStream fos = new FileOutputStream(file);
                        //파일 내용 입력
                        fos.write(Record_data.getBytes());

                        fos.close();

                        // 버튼 입력 횟수 변수 초기화
                        btn_count = 0;

                        Toast.makeText(getApplicationContext(), FileName + "에 저장", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "저장 실패");
                    Toast.makeText(getApplicationContext(), "저장 실패",
                            Toast.LENGTH_SHORT).show();
                }

            }
        };

        Input_temp = (EditText) findViewById(R.id.temp_input);
        Input_chl = (EditText) findViewById(R.id.chl_input);
        Input_chlRing = (EditText) findViewById(R.id.chlRing_input);
        Input_bga = (EditText) findViewById(R.id.bga_input);
        Input_bgaRing = (EditText) findViewById(R.id.bgaRing_input);

        // OFFSET 버튼
        BTN_offset = (Button) findViewById(R.id.button_offset);
        View.OnClickListener listener3 = new View.OnClickListener(){

            @Override
            public void onClick(View a){

                try {
                    /*str_temp = Input_temp.getText().toString().getBytes();
                    str_chl  = Input_chl.getText().toString();
                    str_chlRing = Input_chlRing.getText().toString();
                    str_bga = Input_bga.getText().toString();
                    str_bgaRing = Input_bgaRing.getText().toString();*/

                    temp = Float.parseFloat(Input_temp.getText().toString());
                    chl = Float.parseFloat(Input_chl.getText().toString());
                    chlRing = Float.parseFloat(Input_chlRing.getText().toString());
                    bga = Float.parseFloat(Input_bga.getText().toString());
                    bgaRing = Float.parseFloat(Input_bgaRing.getText().toString());

                    button = true;
                } catch (Exception e) {
                    Log.e(TAG, "Offset 설정 실패");
                    Toast.makeText(getApplicationContext(), "Offset 설정 실패",
                            Toast.LENGTH_SHORT).show();
                }

            }
        };

        BTN_offset.setOnClickListener(listener3);

        ReloadFile = (TextView) findViewById(R.id.reloadFile);
        ReloadFile.setMaxLines(100);
        ReloadFile.setVerticalScrollBarEnabled(true);
        ReloadFile.setMovementMethod(new ScrollingMovementMethod());

        BTN_readFile = (Button) findViewById(R.id.ReadFile);
        BTN_record = (Button) findViewById(R.id.button_record);
        BTN_motor = (Button) findViewById(R.id.button_motor);

        // 파일 출력 버튼
        View.OnClickListener listener1 = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    String FileName = InputFile_name.getText().toString();

                    switch (v.getId()) {

                        case R.id.ReadFile:

                            File dir = new File(SDpath, "recordFile");
                            File file = new File(dir, FileName + ".csv");

                            FileInputStream fis = new FileInputStream(file);
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader br = new BufferedReader(isr);

                            String str_result = "";
                            String str = "";
                            while((str = br.readLine()) != null){
                                str_result += str +"\n";
                            }

                            br.close();

                            ReloadFile.setText(String.valueOf(str_result));

                            Toast.makeText(getApplicationContext(), FileName + "File Open!", Toast.LENGTH_SHORT).show();

                            break;
                    }

                }catch (Exception e) {
                    Log.e(TAG, "파일 읽기 실패");
                    Toast.makeText(getApplicationContext(), "파일 읽기 실패",
                            Toast.LENGTH_SHORT).show();
                }
            }

        };

        BTN_3sec = (Button) findViewById(R.id.button_3sec);
        BTN_5sec = (Button) findViewById(R.id.button_5sec);
        BTN_10sec = (Button) findViewById(R.id.button_10sec);

        // 주기 변경 버튼
        View.OnClickListener listener_cycle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    switch (v.getId()) {

                        case R.id.button_3sec:
                            cycle = 3000;
                            break;

                        case R.id.button_5sec:
                            cycle = 5000;
                            break;

                        case R.id.button_10sec:
                            cycle = 10000;
                            break;

                    }
                    Toast.makeText(getApplicationContext(), "주기 " + cycle / 1000 + "초로 변경",
                            Toast.LENGTH_SHORT).show();

                }catch (Exception e) {
                    Log.e(TAG, "주기 설정 실패");
                    Toast.makeText(getApplicationContext(), "주기 설정 실패",
                            Toast.LENGTH_SHORT).show();
                }
            }

        };

        BTN_3sec.setOnClickListener(listener_cycle);
        BTN_5sec.setOnClickListener(listener_cycle);
        BTN_10sec.setOnClickListener(listener_cycle);

        BTN_record.setOnClickListener(listener2);
        BTN_readFile.setOnClickListener(listener1);

        // 실시간 1초마다 갱신
        CurrentTime();

    } //onCreate 종료


    protected void setChartSettings(
            XYMultipleSeriesRendererrenderer, String title, String xTitle, String yTitle,
            double xMin, double xMax, double yMin, double yMax,
            int axesColor, int labelsColor) {
        renderer.setChartTitle(title);
        renderer.setXTitle(xTitle);
        renderer.setYTitle(yTitle);
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(4000);
        renderer.setYAxisMax(4600);
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(labelsColor);
    }

    protected XYMultipleSeriesRenderer buildBarRenderer(int[] colors) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        int length = colors.length;

        for (int i = 0; i < length; i++) {
            SimpleSeriesRenderer r = new SimpleSeriesRenderer();
            r.setColor(colors[i]);
            renderer.addSeriesRenderer(r);
        }

        return renderer;
    }


    private void CurrentTime(){
        TimerTask CurrentTimerTask = new TimerTask() {
            @Override
            public void run() {
                Update();
            }
        };

        Timer displayTimer = new Timer();
        displayTimer.schedule(CurrentTimerTask,0, 1000);
    }

    Calendar mcalendar = Calendar.getInstance();
    Date mcurMillis;
    int mcurYear;
    int mcurMonth;
    int mcurDay;
    int mcurHour;
    int mcurNoon;

    int mcurMinute;
    int mcurSecond;

    String mNoon;
    String mstrDate;
    String mstrTime;

    private final Handler mDateTimeHandler = new Handler();

    protected void Update(){
        mcalendar = Calendar.getInstance();
        mcurMillis = mcalendar.getTime();
        /*mcurYear = mcalendar.get(Calendar.YEAR);
        mcurMonth = mcalendar.get(Calendar.MONTH)+1;
        mcurDay = mcalendar.get(Calendar.DAY_OF_MONTH);
        mcurHour = mcalendar.get(Calendar.HOUR_OF_DAY);*/
        mcurNoon = mcalendar.get(Calendar.AM_PM);

        mcurMinute = mcalendar.get(Calendar.MINUTE);
        mcurSecond = mcalendar.get(Calendar.SECOND);

        if(mcurNoon == 0){
            mNoon = "AM";
        }else{
            mNoon = "PM";
            mcurHour -= 12;
        }

        Runnable updater = new Runnable() {
            public void run() {
                /*if((mcurMonth<10)&&(mcurDay<10)){
                    mstrDate = mcurYear+"/"+"0"+mcurMonth+"/"+"0"+mcurDay;
                }else if((mcurMonth<10)&&(mcurDay>=10)){
                    mstrDate = mcurYear+"/"+"0"+mcurMonth+"/"+mcurDay;
                }else if((mcurMonth>=10)&&(mcurDay<10)){
                    mstrDate = mcurYear+"/"+mcurMonth+"/"+"0"+mcurDay;
                }else{
                    mstrDate = mcurYear+"/"+mcurMonth+"/"+mcurDay;
                }*/

                /*if((mcurHour<10)&&(mcurMinute<10)&&(mcurSecond<10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+"0"+mcurMinute+":"+"0"+mcurSecond;
                }else if((mcurHour<10)&&(mcurMinute<10)&&(mcurSecond>=10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+"0"+mcurMinute+":"+mcurSecond;
                }else if((mcurHour<10)&&(mcurMinute>=10)&&(mcurSecond<10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+mcurMinute+":"+"0"+mcurSecond;
                }else if((mcurHour<10)&&(mcurMinute>=10)&&(mcurSecond>=10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+mcurMinute+":"+mcurSecond;
                }else if((mcurHour>=10)&&(mcurMinute<10)&&(mcurSecond<10)){
                    mstrTime = mNoon+" "+mcurHour+":"+"0"+mcurMinute+":"+"0"+mcurSecond;
                }else if((mcurHour>=10)&&(mcurMinute<10)&&(mcurSecond>=10)){
                    mstrTime = mNoon+" "+mcurHour+":"+"0"+mcurMinute+":"+mcurSecond;
                }else if((mcurHour>=10)&&(mcurMinute>=10)&&(mcurSecond<10)){
                    mstrTime = mNoon+" "+mcurHour+":"+mcurMinute+":"+"0"+mcurSecond;
                }else{
                    mstrTime = mNoon+" "+mcurHour+":"+mcurMinute+":"+mcurSecond;
                }*/

                if((mcurHour<10)&&(mcurMinute<10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+"0"+mcurMinute;
                }else if((mcurHour<10)&&(mcurMinute<10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+"0"+mcurMinute;
                }else if((mcurHour<10)&&(mcurMinute>=10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+mcurMinute;
                }else if((mcurHour<10)&&(mcurMinute>=10)){
                    mstrTime = mNoon+" "+"0"+mcurHour+":"+mcurMinute;
                }else if((mcurHour>=10)&&(mcurMinute<10)){
                    mstrTime = mNoon+" "+mcurHour+":"+"0"+mcurMinute;
                }else if((mcurHour>=10)&&(mcurMinute<10)){
                    mstrTime = mNoon+" "+mcurHour+":"+"0"+mcurMinute;
                }else if((mcurHour>=10)&&(mcurMinute>=10)){
                    mstrTime = mNoon+" "+mcurHour+":"+mcurMinute;
                }else{
                    mstrTime = mNoon+" "+mcurHour+":"+mcurMinute;
                }

                strNow = mstrTime;

            }
        };
        mDateTimeHandler.post(updater);

    }

    public byte[] GetData(byte[] data){

        return data;
    }

    private void check_sync(byte[] data){

    }

    // Record Data 내용
    public void RecordData(){
        //String Record_data = "";

        if(record_Data_temp == ""){
            record_Data_temp = "No data";
        }
        if(record_Data_chl == ""){
            record_Data_chl = "No data";
        }
        if(record_Data_chl_ring == ""){
            record_Data_chl_ring = "No data";
        }
        if(record_Data_bga == ""){
            record_Data_bga = "No data";
        }
        if(record_Data_bga_ring == ""){
            record_Data_bga_ring = "No data";
        }

        Record_data = Record_data + strNow + "," + record_Data_temp + "," + record_Data_chl + "," + record_Data_chl_ring + "," + record_Data_bga + "," + record_Data_bga_ring + "\r\n";

        //return Record_data;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId); -- 에러 원인!! 화면에 연결상태 확인용인데 내 앱에는 필요없음.
            }
        });
    }

    private void dataSend(int check_data){

        byte[] send_command = new byte[]{(byte) 0x00};

        try {
            switch (check_data) {
                case 1:
                    Log.d(TAG, "보냈던 command : " + check_data);
                    send_command[0] = (byte) 0x31;
                    characteristicTX.setValue(send_command);
                    mBluetoothLeService.writeCharacteristic(characteristicTX, send_command);
                    Log.d(TAG, "지금 보낸 command : " + send_command[0]);
                    break;

                case 2:
                    Log.d(TAG, "보냈던 command : " + check_data);
                    send_command[0] = (byte) 0x32;
                    characteristicTX.setValue(send_command);
                    mBluetoothLeService.writeCharacteristic(characteristicTX, send_command);
                    Log.d(TAG, "지금 보낸 command : " + send_command[0]);
                    break;

                case 3:
                    Log.d(TAG, "보냈던 command : " + check_data);
                    send_command[0] = (byte) 0x33;
                    characteristicTX.setValue(send_command);
                    mBluetoothLeService.writeCharacteristic(characteristicTX, send_command);
                    Log.d(TAG, "지금 보낸 command : " + send_command[0]);
                    break;

                case 4:
                    Log.d(TAG, "보냈던 command : " + check_data);
                    send_command[0] = (byte) 0x34;
                    characteristicTX.setValue(send_command);
                    mBluetoothLeService.writeCharacteristic(characteristicTX, send_command);
                    Log.d(TAG, "지금 보낸 command : " + send_command[0]);
                    break;

                default:
                    Log.d(TAG, "보냈던 command : " + check_data);
                    send_command[0] = (byte) 0x31;
                    characteristicTX.setValue(send_command);
                    mBluetoothLeService.writeCharacteristic(characteristicTX, send_command);
                    Log.d(TAG, "지금 보낸 command : " + send_command[0]);
                    break;
            }
        }
        // 예외처리
        catch(Exception e){
            //mBluetoothLeService.writeCharacteristic_NO_RESPONSE(characteristicTX);
            Log.e(TAG , "데이터 전송에 실패하였습니다.");
            Toast.makeText(getApplicationContext(), "데이터 전송에 실패하였습니다.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void displayData(byte[] data) {

        //command 날리기위해 어떤 command를 받았는지 체크위한 변수
        //char[] check_data = new char[data.length+1];

        try{
            // 모듈에 데이터를 받았다는 확인 메세지 전송
            flag = true;
            int j, n, i = 0;
            int count = 0;

            // Command 들어온 횟수 세는 변수
            //int cnt = 0;

            // byte -> char 로 변경하여 저장하기 위한 변수
            char[] data_temp = new char[data.length+1];
            char[] data_temp1 = new char[data.length+1];
            char[] data_temp2 = new char[data.length+1];
            int motor_active = 0;
            int[] temp_num = new int[data.length+1];
            String Data_bga = "";
            String Data_bga_ring = "";
            String Data_chl_ring = "";
            String Data_temp = "";
            String Data_chl = "";

            float result1 = 0;
            float result2 = 0;
            String Data = "";

            for(int x = 0; x < data.length; x++){
                temp_num[x] = data[x];
                data_temp[x] = (char)temp_num[x];
            }
            //Data = data_temp.toString();

            if(data_temp[0] == '!') {
                check_sync = true;
            }
            //else {check_sync = false;}

//======================================================================================================
            // 설정해둔 Command 가 맞는지 검사(현재 Command : W, w, 0)
            if((data_temp[0] == '0')||(data_temp[0] == 'w')||(data_temp[0] == 'W')){

                check_sync = false;

            //if(data_temp[0] == '0'){

                /*for(j = 0; j < data.length; j++){
                    Log.d(TAG, "받은 데이터 : " + data_temp[j]);

                    // 62 : '>'
                    if(data_temp[j] == '>'){
                        //m = j;
                        count = count + 1;
                        if(count == 1) {
                            for (n = j; n < data_temp.length; n++) {

                                // 60 : '<'
                                if(data_temp[n] == '<' || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_temp1 = Data_temp1;
                                }
                                else {
                                    Data_temp1 = Data_temp1 + data_temp[n];
                                    if(data_temp[n] == '0') motor_active = 1;
                                    else motor_active = 0;
                                }
                            }
                        }

                        else if(count == 2){
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if((data_temp[n] == '<') || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_temp2 = Data_temp2;
                                }
                                else {
                                    Data_temp2 = Data_temp2 + data_temp[n];
                                    if(data_temp[n] == '0') motor_active = 1;
                                    else motor_active = 0;
                                }
                            }
                        }

                    }

                }*/

                if(data_temp[1] == '>'){

                    if(data_temp[2] == '0'){

                        if(data_temp[3] == '<') motor_active = 1;
                        else motor_active = 0;
                    }
                    else motor_active = 0;
                }
                else motor_active = 0;

                // 모터 작동 여부 결정
                if(motor_active == 1) {
                    Toast.makeText(getApplicationContext(), "모터 작동",
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "모터 작동 안함",
                            Toast.LENGTH_SHORT).show();
                }

            } // Command 검사 if 문 종료
//====================================================================================================
            // 설정해둔 Command 가 맞는지 검사(현재 Command : '1')
            else if(data_temp[0] == '1'){

                check_sync = false;

                for(j = 0; j < data.length; j++) {

                    // 62 : '>'
                    if (data_temp[j] == '>') {
                        //m = j;
                        count = count + 1;
                        if (count == 1) {
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if (data_temp[n] == '<' || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if ((data_temp[n] == 32) || (data_temp[n] == '>')) {
                                    Data_chl = Data_chl;
                                } else {
                                    Data_chl = Data_chl + data_temp[n];
                                }
                            }
                        } else if (count == 2) {
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if ((data_temp[n] == '<') || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if ((data_temp[n] == 32) || (data_temp[n] == '>')) {
                                    Data_temp = Data_temp;
                                } else {
                                    Data_temp = Data_temp + data_temp[n];
                                }
                            }
                        }

                    }
                }

                Log.d(TAG, "Data_temp -> Integer : " + Float.parseFloat(Data_temp));

                if(button){
                    result1 = (Float.parseFloat(Data_chl)) + chl;
                    result2 = (Float.parseFloat(Data_temp)) + temp;

                    Log.d(TAG, "result1 : " + result1);

                    Data_chl = String.valueOf(result1);
                    Data_temp = String.valueOf(result2);
                    Toast.makeText(getApplicationContext(), "offset 연산 결과 Temp : " + Data_temp
                            , Toast.LENGTH_SHORT).show();
                }

                record_Data_temp = Data_temp;
                record_Data_chl = Data_chl;

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                CHL.setText(String.valueOf(Data_chl));

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                Temp.setText(String.valueOf(Data_temp));

                /*Toast.makeText(getApplicationContext(), "1 데이터 수신에 성공하였습니다."
                        , Toast.LENGTH_SHORT).show();*/

                Log.d(TAG, "1수신 완료");
                Log.d(TAG, "출력데이터1 : " + Data_chl);
                Log.d(TAG, "출력데이터2 : " + Data_temp);
                Log.d(TAG, "원본 데이터 : " + data_temp);

                graph_temp = Integer.valueOf(Data_temp);

                cnt = cnt + 1;

                //handler_commend.sendEmptyMessageDelayed(0, cycle);
                //timer.schedule(second, 0, cycle);
                //handler.post(updater);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dataSend(2);
                    }
                }, cycle);

            } // Command 검사 if 문 종료
//===================================================================================================
            // 설정해둔 Command 가 맞는지 검사(현재 Command : '2')
            else if(data_temp[0] == (byte) 0x32){

                check_sync = false;

                for(j = 0; j < data.length; j++){

                    // 62 : '>'
                    if(data_temp[j] == '>'){
                        //m = j;
                        count = count + 1;
                        if(count == 1) {
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if(data_temp[n] == '<' || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_chl_ring = Data_chl_ring;
                                }
                                else {
                                    Data_chl_ring = Data_chl_ring + data_temp[n];
                                }
                            }
                        }

                        else if(count == 2){
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if((data_temp[n] == '<') || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_temp = Data_temp;
                                }
                                else {
                                    Data_temp = Data_temp + data_temp[n];
                                }
                            }
                        }

                    }
                }

                if(button){
                    result1 = (Float.parseFloat(Data_chl_ring)) + chlRing;
                    result2 = (Float.parseFloat(Data_temp)) + temp;

                    Data_chl_ring = String.valueOf(result1);
                    Data_temp = String.valueOf(result2);
                    Toast.makeText(getApplicationContext(), "offset 연산 결과 Temp : " + Data_temp
                            , Toast.LENGTH_SHORT).show();
                }

                record_Data_temp = Data_temp;
                record_Data_chl_ring = Data_chl_ring;

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                CHL_RingBuf.setText(String.valueOf(Data_chl_ring));

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                Temp.setText(String.valueOf(Data_temp));

                /*Toast.makeText(getApplicationContext(), "2 데이터 수신에 성공하였습니다."
                        , Toast.LENGTH_SHORT).show();*/
                Log.d(TAG, "2수신 완료");
                Log.d(TAG, "원본 데이터 : " + data_temp);

                cnt = cnt + 1;

                graph_temp = Integer.valueOf(Data_temp);

                //handler_commend.sendEmptyMessageDelayed(0, cycle);
                //timer.schedule(second, 0, cycle);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dataSend(3);
                    }
                }, cycle);

                //dataSend(3);

            } // Command 검사 if 문 종료
//======================================================================================================
            // 설정해둔 Command 가 맞는지 검사(현재 Command : '3')
            else if(data_temp[0] == (byte) 0x33){

                check_sync = false;

                for(j = 0; j < data.length; j++){

                    // 62 : '>'
                    if(data_temp[j] == '>'){
                        //m = j;
                        count = count + 1;
                        if(count == 1) {
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if(data_temp[n] == '<' || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_bga = Data_bga;
                                }
                                else {
                                    Data_bga = Data_bga + data_temp[n];
                                }
                            }
                        }

                        else if(count == 2){
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if((data_temp[n] == '<') || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_temp = Data_temp;
                                }
                                else {
                                    Data_temp = Data_temp + data_temp[n];
                                }
                            }
                        }

                    }
                }

                if(button){
                    result1 = (Float.parseFloat(Data_bga)) + bga;
                    result2 = (Float.parseFloat(Data_temp)) + temp;

                    Data_bga = String.valueOf(result1);
                    Data_temp = String.valueOf(result2);
                    Toast.makeText(getApplicationContext(), "offset 연산 결과 Temp : " + Data_temp
                            , Toast.LENGTH_SHORT).show();
                }

                record_Data_temp = Data_temp;
                record_Data_bga = Data_bga;

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                BGA.setText(String.valueOf(Data_bga));

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                Temp.setText(String.valueOf(Data_temp));

                /*Toast.makeText(getApplicationContext(), "3 데이터 수신에 성공하였습니다."
                        , Toast.LENGTH_SHORT).show();*/
                Log.d(TAG, "3수신 완료");
                Log.d(TAG, "원본 데이터 : " + data_temp);

                cnt = cnt + 1;

                graph_temp = Integer.valueOf(Data_temp);

                //handler_commend.sendEmptyMessageDelayed(0, cycle);
                //timer.schedule(second, 0, cycle);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dataSend(4);
                    }
                }, cycle);

                //dataSend(4);

            } // Command 검사 if 문 종료
//====================================================================================================
            // 설정해둔 Command 가 맞는지 검사(현재 Command : '4')
            else if(data_temp[0] == (byte) 0x34){

                check_sync = false;

                for(j = 0; j < data.length; j++){

                    // 62 : '>'
                    if(data_temp[j] == '>'){
                        //m = j;
                        count = count + 1;
                        if(count == 1) {
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if(data_temp[n] == '<' || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_bga_ring = Data_bga_ring;
                                }
                                else {
                                    Data_bga_ring = Data_bga_ring + data_temp[n];
                                }
                            }
                        }

                        else if(count == 2){
                            for (n = j; n < data_temp.length; n++) {
                                // 60 : '<'
                                if((data_temp[n] == '<') || (data_temp[n] == '\0')) break;
                                    // 32 : 'SP' --> space, 62 : '>'
                                else if((data_temp[n] == 32) || (data_temp[n] == '>')){
                                    Data_temp = Data_temp;
                                }
                                else {
                                    Data_temp = Data_temp + data_temp[n];
                                }
                            }
                        }

                    }
                }

                if(button){
                    result1 = (Float.parseFloat(Data_bga_ring)) + bgaRing;
                    result2 = (Float.parseFloat(Data_temp)) + temp;

                    Data_bga_ring = String.valueOf(result1);
                    Data_temp = String.valueOf(result2);
                    Toast.makeText(getApplicationContext(), "offset 연산 결과 Temp : " + Data_temp
                            , Toast.LENGTH_SHORT).show();
                }

                record_Data_temp = Data_temp;
                record_Data_bga_ring = Data_bga_ring;

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                BGA_RingBuf.setText(String.valueOf(Data_bga_ring));

                // Test_protocol 이 가리키는 출력 위치에 문자열 출력
                Temp.setText(String.valueOf(Data_temp));

                /*Toast.makeText(getApplicationContext(), "4 데이터 수신에 성공하였습니다."
                        , Toast.LENGTH_SHORT).show();*/
                Log.d(TAG, "4수신 완료");
                Log.d(TAG, "원본 데이터 : " + data_temp);

                cnt = cnt + 1;

                graph_temp = Integer.valueOf(Data_temp);

                //handler_commend.sendEmptyMessageDelayed(0, cycle);
                //timer.schedule(second, 0, cycle);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dataSend(1);
                    }
                }, cycle);

                //dataSend(1);

            } // Command 검사 if 문 종료

            if(cnt == 4){
                RecordData();
                cnt = 0;
                //CurrentTime();
            }
            //RecordData();

            //record_data = RecordData();
        } // try 문 종료
        // 예외처리
        catch(Exception e){
            //mBluetoothLeService.writeCharacteristic_NO_RESPONSE(characteristicTX);
            Log.e(TAG , "데이터 수신에 실패하였습니다.");
            Toast.makeText(getApplicationContext(), "데이터 수신에 실패하였습니다.",
                    Toast.LENGTH_SHORT).show();
        }
        //return check_data[0];
        //RecordData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_DEVICE_REQUEST) {
            if (resultCode == RESULT_OK) {
                // A contact was picked.  Here we will just display it
                // to the user.
                mDeviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
                mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                Toast.makeText(this, "디바이스 이름" + ": "+mDeviceName +" - "+ mDeviceAddress + "\n연결되었습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void discoveredGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices)
        {
            uuid = gattService.getUuid().toString();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
            {
                uuid = gattCharacteristic.getUuid().toString();
            }
        }
    }

    private void discoveredGattServices2(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices)
        {
            uuid = gattService.getUuid().toString();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
            {
                uuid = gattCharacteristic.getUuid().toString();
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public boolean onKeyDown(int KeyCode, KeyEvent event) {
        super.onKeyDown(KeyCode, event);
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (KeyCode) {
                case KeyEvent.KEYCODE_BACK:
                    AlertDialog.Builder dlg = new AlertDialog.Builder(this);
                    dlg.setMessage("호출 중 종료 시 앱이 강제 종료됩니다. 종료 하시겠습니까???")
                            .setCancelable(false)
                            .setPositiveButton("확인",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            // TODO Auto-generated method stub
                                            DeviceControlActivity.this.finish();
                                        }
                                    }).setNegativeButton("취소", null).show();
            }
            return true;
        }
        return false;
    }
}