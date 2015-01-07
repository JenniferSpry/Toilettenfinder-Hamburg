package de.bfhh.stilleoertchenhamburg.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
 
public class NetworkUtil {
     
    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
 
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TagNames.TYPE_WIFI;
             
            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TagNames.TYPE_MOBILE;
        } 
        return TagNames.TYPE_NOT_CONNECTED;
    }
     
    public static String getConnectivityStatusString(Context context) {
        int conn = NetworkUtil.getConnectivityStatus(context);
        String status = null;
        if (conn == TagNames.TYPE_WIFI) {
            status = "Wifi enabled";
        } else if (conn == TagNames.TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (conn == TagNames.TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }
}

