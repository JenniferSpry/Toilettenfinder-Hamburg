package de.bfhh.stilleoertchenhamburg.activites;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import de.bfhh.stilleoertchenhamburg.helpers.DatabaseHelper;
import de.bfhh.stilleoertchenhamburg.helpers.NetworkUtil;
import de.bfhh.stilleoertchenhamburg.helpers.TagNames;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

public class ActivityBase extends ActionBarActivity {

	static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;

	private NetworkInfo _mobileInfo;
	private NetworkInfo _wifiInfo;

	private ConnectivityManager _connecMan;

	protected int _connecState;// Wifi = 1, Mobile = 2, not connected = 0
	protected int _oldConnecState;

	private boolean _networkReceiverRegistered;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_connecMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		_oldConnecState = TagNames.TYPE_NOT_CONNECTED;
		_connecState = NetworkUtil.getConnectivityStatus(this);
		if (_connecState == TagNames.TYPE_NOT_CONNECTED
				&& !DatabaseHelper.getInstance(getApplicationContext())
						.isDataStillFresh(getApplicationContext())) {
			showAlertMessageNetworkSettings(null, false);
		}
		_oldConnecState = _connecState;

		IntentFilter filter = new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE");
		filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
		registerReceiver(networkChangeReceiver, filter);
		_networkReceiverRegistered = true;
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!_networkReceiverRegistered) {
			IntentFilter filter = new IntentFilter(
					"android.net.conn.CONNECTIVITY_CHANGE");
			filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
			registerReceiver(networkChangeReceiver, filter);
			_networkReceiverRegistered = true;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (_networkReceiverRegistered) {
			unregisterReceiver(networkChangeReceiver);
			_networkReceiverRegistered = false;
		}
	}

	/** Method that is called when there is a network connection of some kind */
	protected void onNetworkConnected() {
		Log.d("ActivityBase", "onNetworkConnected");
	}

	protected boolean checkPlayServices() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
				showErrorDialog(status);
			} else {
				Toast.makeText(this, "Dieses Gerät wird nicht unterstützt.",
						Toast.LENGTH_LONG).show();
				finish();
			}
			return false;
		}
		return true;
	}

	void showErrorDialog(int code) {
		GooglePlayServicesUtil.getErrorDialog(code, this,
				REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("onActivityResult", "" + requestCode);
		switch (requestCode) {
		case REQUEST_CODE_RECOVER_PLAY_SERVICES:
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this,
						"Google Play Services müssen installiert sein.",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Network connection checks
	 */

	private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			_connecState = NetworkUtil.getConnectivityStatus(context);
			// when connected, call a method that needs to be overridden by
			// every activity (specific behaviour)
			if (_connecState == TagNames.TYPE_MOBILE
					|| _connecState == TagNames.TYPE_WIFI) {
				if (_connecState != _oldConnecState
						&& _oldConnecState == TagNames.TYPE_NOT_CONNECTED) {
					Toast.makeText(
							context,
							"Verbindung mit dem Netzwerk hergestellt. Daten werden abgerufen, bitte warten...",
							Toast.LENGTH_LONG).show();
					onNetworkConnected();
					_oldConnecState = _connecState;
				}
			}
		}
	};

	// returns true if connected to mobile network or wifi (or both)
	protected boolean isConnectedToNetwork(Context ctx) {
		if (_connecMan == null) {
			_connecMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		_mobileInfo = _connecMan
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		_wifiInfo = _connecMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return _mobileInfo.isConnected() || _wifiInfo.isConnected();
	}

	DialogInterface.OnClickListener onWifiOkListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int id) {
			startActivityForResult(new Intent(
					android.provider.Settings.ACTION_WIFI_SETTINGS), -1);
		}
	};

	DialogInterface.OnClickListener onMobileNetworkOkListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int id) {
			startActivityForResult(new Intent(
					android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS), -1);
		}
	};

	/** show alert dialog with option to change network settings */
	protected void showAlertMessageNetworkSettings(String msg,
			boolean callOnNetworkConnected) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		if (!callOnNetworkConnected) {
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
						}
					});
		} else {
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							onNetworkConnected();
						}
					});
		}

		if (msg == null) {
			builder.setMessage("Du hast keine Internetverbindung. Bitte überprüfe Deine Netzwerkeinstellungen!");
		} else {
			builder.setMessage(msg);
		}
		final AlertDialog alert = builder.create();
		alert.show();
	}

}
