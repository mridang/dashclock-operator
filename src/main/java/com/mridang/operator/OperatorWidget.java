package com.mridang.operator;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.android.apps.dashclock.api.ExtensionData;

import org.acra.ACRA;

/*
 * This class is the main class that provides the widget
 */
public class OperatorWidget extends ImprovedExtension {

    /**
     * The contents of the message of the message that is returned from the operator
     */
    private String strResponse = "";

    /*
     * (non-Javadoc)
     * @see com.mridang.operator.ImprovedExtension#getIntents()
     */
    @Override
    protected IntentFilter getIntents() {

        IntentFilter itfIntents = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        itfIntents.setPriority(Integer.MAX_VALUE);
        return itfIntents;

    }

    /*
     * (non-Javadoc)
     * @see com.mridang.operator.ImprovedExtension#getTag()
     */
    @Override
    protected String getTag() {
        return getClass().getSimpleName();
    }

    /*
     * (non-Javadoc)
     * @see com.mridang.operator.ImprovedExtension#getUris()
     */
    @Override
    protected String[] getUris() {
        return null;
    }

    /*
     * @see
     * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
     * (int)
     */
    @Override
    protected void onUpdateData(int intReason) {

        Log.d(getTag(), "Checking the response from the operator");
        ExtensionData edtInformation = new ExtensionData();
        setUpdateWhenScreenOn(true);

        try {

            String strNumber = getString("number", "");
            if (intReason == UPDATE_REASON_PERIODIC || intReason == UPDATE_REASON_SETTINGS_CHANGED
                    || intReason == UPDATE_REASON_INITIAL) {

                SmsManager mgrMessage = SmsManager.getDefault();
                String strMessage = getString("message", "");

                if (!strNumber.isEmpty() && !strMessage.isEmpty()) {

                    Log.d(getTag(), "Sending a text-message to the operator");
                    mgrMessage.sendTextMessage(strNumber, null, strMessage, null, null);
                    Log.v(getTag(), String.format("Sending %s to %s", strMessage, strNumber));

                } else {
                    Log.w(getTag(), "Either the number or the message is missing");
                }

            } else {

                if (!strNumber.isEmpty() && !strResponse.isEmpty()) {

                    edtInformation.expandedTitle(getString(R.string.status, strNumber));
                    edtInformation.expandedBody(strResponse);
                    edtInformation.visible(true);

                }

            }

        } catch (Exception e) {
            edtInformation.visible(false);
            Log.e(getTag(), "Encountered an error", e);
            ACRA.getErrorReporter().handleSilentException(e);
        }

        edtInformation.icon(R.drawable.ic_dashclock);
        doUpdate(edtInformation);

    }

    /*
     * (non-Javadoc)
     * @see com.mridang.operator.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
     */
    @Override
    protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {

        Bundle bndExtras = ittIntent.getExtras();
        if (bndExtras != null) {

            Object[] arrObject = (Object[]) bndExtras.get("pdus");
            for (Object anArrObject : arrObject) {

                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) anArrObject);
                if (smsMessage.getOriginatingAddress().equalsIgnoreCase(getString("number", ""))) {

                    Log.d(getTag(), "Received a message from the operator");
                    OperatorWidget.this.strResponse = smsMessage.getMessageBody();
                    Log.v(getTag(), OperatorWidget.this.strResponse);
                    //this.abortBroadcast();
                    OperatorWidget.this.onUpdateData(UPDATE_REASON_MANUAL);

                }

            }

        }

    }

}