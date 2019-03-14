package com.google.aleenajose.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MyActivity" ;
    Button btnOnOff, btnDiscover, btnSend;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText writeMsg;

    WifiManager  wifiManager;
    WifiP2pManager mManager;//this class provides the API for managing wifi p2p connectivity.
    WifiP2pManager.Channel mChannel;// a channel that connects the application to the wifi p2p framework.
    // Most p2p operations require a Channel as an argument

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray; //we will use this array to show device name in listview
    WifiP2pDevice[] deviceArray; //we will use this array to connect a device

    static final int MESSAGE_READ=1;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)//type of msg
            {
                case MESSAGE_READ:
                    byte[] readBuff= (byte[]) msg.obj;//msg.obj will be converted to byte array
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    read_msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    //wifi on/off
    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (wifiManager.isWifiEnabled())//check current status
                {
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("ON");
                } else {
                    wifiManager.setWifiEnabled(true);//enable/disable
                    btnOnOff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Starting Failed");
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device=deviceArray[i];
                WifiP2pConfig config=new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connected to"+device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg=writeMsg.getText().toString();// this object contains msg
                sendReceive.write(msg.getBytes());
            }
        });
    }

    private void initialWork() {
        btnOnOff=(Button) findViewById(R.id.onOff);
        btnDiscover=(Button) findViewById(R.id.discover);
        btnSend=(Button) findViewById(R.id.sendButton);
        listView=(ListView) findViewById(R.id.peerListView);
        read_msg_box=(TextView) findViewById(R.id.readMsg);
        connectionStatus=(TextView) findViewById(R.id.connectionStatus);
        writeMsg=(EditText) findViewById(R.id.writeMsg);

        wifiManager = (WifiManager) getApplicationContext() .getSystemService(Context.WIFI_SERVICE);

        mManager= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel=mManager.initialize(this,getMainLooper(),null);

        mReceiver=new WIfiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter=new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers))
            { // check whether the peer list has changed
                peers.clear(); // clear prev list
                peers.addAll(peerList.getDeviceList()); //add current list

                deviceNameArray=new String[peerList.getDeviceList().size()]; //size acc to list
                deviceArray=new WifiP2pDevice[peerList.getDeviceList().size()];// same
                int index=0;

                for (WifiP2pDevice device : peerList.getDeviceList())// loop depends on size of list
                {
                    deviceNameArray[index]=device.deviceName;// device names are stored in this array
                    deviceArray[index]=device; // no of devices
                    index++;
                }
                 // shows list of the devices in list view
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }

            if (peers.size()==0)
            {
                //Log.d(MainActivity.TAG, "no devices found");//added
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    // managing sending and receiving data
    WifiP2pManager.ConnectionInfoListener connectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress=wifiP2pInfo.groupOwnerAddress;

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
            {
                connectionStatus.setText("Host");
                serverClass=new ServerClass();
                serverClass.start();
            }else if (wifiP2pInfo.groupFormed)
            {
                connectionStatus.setText("Client");
                clientClass=new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };
    // register the broadcast receiver
    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new WIfiDirectBroadcastReceiver(mManager, mChannel, this);//added
        registerReceiver(mReceiver,mIntentFilter);
    }
     //unregister the broadcast receiver
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerClass extends Thread{ //to create server side thread
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                 serverSocket =new ServerSocket(8888);
                 socket=serverSocket.accept();// soket ready to accept msg
                 sendReceive=new SendReceive(socket);
                 sendReceive.start();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt) //constructor
        {
            socket=skt;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer=new byte[1024];
            int bytes;

            while (socket!=null)//receives msg until null
            {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes>0)
                    {
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class ClientClass extends Thread{ //client side thread
        Socket socket;
        String hostAdd;

        public  ClientClass(InetAddress hostAddress)
        {
            hostAdd=hostAddress.getHostAddress(); //getting host address
            socket=new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
