package com.google.aleenajose.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

public class WIfiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;

    public WIfiDirectBroadcastReceiver(WifiP2pManager mManager,WifiP2pManager.Channel mChannel,MainActivity mActivity)
    {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = mActivity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            // Determine if Wifi P2p mode is enabled or not, alert the activity
            int state=intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);

            if (state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Toast.makeText(context,"Wifi is ON",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context,"Wifi is OFF",Toast.LENGTH_SHORT).show();
            }
        }else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //the peer list has changed
            // Request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager!=null)
            {
                mManager.requestPeers(mChannel,mActivity.peerListListener);
            }
            Log.d(mActivity.TAG, "P2P peers changed");//added

        }else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            //Connection state changed
            if (mManager==null)
            {
                return;
            }

            NetworkInfo networkInfo=intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO); //storing network information

            if (networkInfo.isConnected())
            {
                mManager.requestConnectionInfo(mChannel,mActivity.connectionInfoListener);
            }else {
                mActivity.connectionStatus.setText("Device Disconnected");
            }
        }else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            //added
           /* DeviceListFragment fragment = (DeviceListFragment) mActivity.getFragmentManager().findFragmentById(R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));*/

            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            int d = Log.d(MainActivity.TAG, "Device status -" + device.deviceName);
            switch (device.status) {
                case WifiP2pDevice.CONNECTED:
                    Log.v(MainActivity.TAG,"mConnected");
                    break;
                case WifiP2pDevice.INVITED:
                    Log.v(MainActivity.TAG,"mInvited");
                    break;
                case WifiP2pDevice.FAILED:
                    Log.v(MainActivity.TAG,"mFailed");
                    break;
                case WifiP2pDevice.AVAILABLE:
                    Log.v(MainActivity.TAG,"mAvailable");
                    break;
                case WifiP2pDevice.UNAVAILABLE:
                    Log.v(MainActivity.TAG,"mUnavailable");
                    break;
                default:
                    Log.v(MainActivity.TAG,"mUnknown");
                    break;
            }
        }
    }
}
