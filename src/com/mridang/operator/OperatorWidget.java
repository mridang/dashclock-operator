package com.mridang.operator;

import java.util.Random;

import org.acra.ACRA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class OperatorWidget extends DashClockExtension {

	/* This is the instance of the receiver handled incoming messages */
	private MessageReceiver objReceiver;
	/* This is the response of the last message sent */
	private String strResponse = "";
	/* This is the number to which messages are sent */
	private String strNumber = "";

	/*
	 * This class is the receiver for receiving the incoming messages
	 */
	private class MessageReceiver extends BroadcastReceiver {

		/*
		 * @see
		 * android.content.BroadcastReceiver#onReceive(android.content.Context,
		 * android.content.Intent)
		 */
		@Override
		public void onReceive(Context ctxContext, Intent ittIntent) {

			Bundle bndExtras = ittIntent.getExtras();
			if (bndExtras != null) {

				Object[] arrObject = (Object[]) bndExtras.get("pdus");
				for (int i = 0; i < arrObject.length; ++i) {

					SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) arrObject[i]);
					if (smsMessage.getOriginatingAddress().equalsIgnoreCase(OperatorWidget.this.strNumber)) {

						Log.d("OperatorWidget", "Received a message from the operator");
						OperatorWidget.this.strResponse = smsMessage.getMessageBody().toString();
						Log.v("OperatorWidget", OperatorWidget.this.strResponse);
						this.abortBroadcast();
						OperatorWidget.this.onUpdateData(UPDATE_REASON_MANUAL);

					}

				}

			}

		}

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onInitialize
	 * (boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		super.onInitialize(booReconnect);

		if (objReceiver != null) {

			try {

				Log.d("OperatorWidget", "Unregistered any existing status receivers");
				unregisterReceiver(objReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		IntentFilter itfIntent = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		itfIntent.setPriority(Integer.MAX_VALUE);
		objReceiver = new MessageReceiver();
		registerReceiver(objReceiver, itfIntent);
		Log.d("OperatorWidget", "Registered the status receiver");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("OperatorWidget", "Created");
		ACRA.init(getApplication());

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d("OperatorWidget", "Checking the response from the operator");
		ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(true);

		try {

			SharedPreferences speSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			if (intReason == UPDATE_REASON_PERIODIC || intReason == UPDATE_REASON_SETTINGS_CHANGED
					|| intReason == UPDATE_REASON_INITIAL) {

				SmsManager mgrMessage = SmsManager.getDefault();
				this.strNumber = speSettings.getString("number", "");
				String strMessage = speSettings.getString("message", "");

				if (!strNumber.isEmpty() && !strMessage.isEmpty()) {

					Log.d("OperatorWidget", "Sending a text-message to the operator");
					mgrMessage.sendTextMessage(this.strNumber, null, strMessage, null, null);
					Log.v("OperatorWidget", String.format("Sending %s to %s", strMessage, this.strNumber));

				} else {

					Log.w("OperatorWidget", "Either the number or the message is missing");

				}

			} else {

				if (!strNumber.isEmpty() && !strResponse.isEmpty()) {

					edtInformation.expandedTitle(getString(R.string.status, strNumber));
					edtInformation.expandedBody(strResponse);
					edtInformation.visible(true);

				}

			}

			if (new Random().nextInt(5) == 0 && !(0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))) {

				PackageManager mgrPackages = getApplicationContext().getPackageManager();

				try {

					mgrPackages.getPackageInfo("com.mridang.donate", PackageManager.GET_META_DATA);

				} catch (NameNotFoundException e) {

					Integer intExtensions = 0;
					Intent ittFilter = new Intent("com.google.android.apps.dashclock.Extension");
					String strPackage;

					for (ResolveInfo info : mgrPackages.queryIntentServices(ittFilter, 0)) {

						strPackage = info.serviceInfo.applicationInfo.packageName;
						intExtensions = intExtensions + (strPackage.startsWith("com.mridang.") ? 1 : 0);

					}

					if (intExtensions > 1) {

						edtInformation.visible(true);
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri
								.parse("market://details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation
								.expandedBody("Thank you for using "
										+ intExtensions
										+ " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			} else {
				setUpdateWhenScreenOn(true);
			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e("OperatorWidget", "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		publishUpdate(edtInformation);
		Log.d("OperatorWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();

		if (objReceiver != null) {

			try {

				Log.d("OperatorWidget", "Unregistered the status receiver");
				unregisterReceiver(objReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		Log.d("OperatorWidget", "Destroyed");

	}

}