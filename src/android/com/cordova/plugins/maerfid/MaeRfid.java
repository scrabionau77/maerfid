/**
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) 2019, Daniele Pellerucci @ Maestrale Information Technology
 * www.maestrale.it
 */
package com.cordova.plugins.maerfid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.ProlificSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.content.pm.PackageManager;

import com.caen.RFIDLibrary.*;
import com.caen.VCPSerialPort.VCPSerialPort;




public class MaeRfid extends CordovaPlugin {
    
    public static final int REQUEST_CODE = 0x0ba7c;
    private static final String CANCELLED = "cancelled";
    private static final String FORMAT = "format";
    private static final String TEXT = "text";


    private static final String TAG = "MaeRfid";

    // actions definitions
    private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
    
    

    // NUOVA IMPLEMENTAZIONE
    // UsbManager instance to deal with permission and opening
    private UsbManager manager;
    // The current driver that handle the serial port
    private UsbSerialDriver driver;

    private JSONArray requestArgs;
    private CallbackContext callbackContext;


    /**
     * Constructor.
     */
    public MaeRfid() {
    }





    /**
	 * Overridden execute method
	 * @param action the string representation of the action to execute
	 * @param args
	 * @param callbackContext the cordova {@link CallbackContext}
	 * @return true if the action exists, false otherwise
	 * @throws JSONException if the args parsing fails
	 */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Action: " + action);
        JSONObject arg_object = args.optJSONObject(0);

        if(ACTION_REQUEST_PERMISSION.equals(action)){
            JSONObject opts = arg_object.has("opts")? arg_object.getJSONObject("opts") : new JSONObject();
            requestPermission(opts, callbackContext);
        } else {
            return false;
        }
        return true;
    }




    /**
     * RICHIESTA PERMESSI CONNESSIONE USB
     */
    private void requestPermission(final JSONObject opts, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                // get UsbManager from Android
                manager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
                UsbSerialProber prober;

                if (opts.has("vid") && opts.has("pid")) {
                    ProbeTable customTable = new ProbeTable();
                    Object o_vid = opts.opt("vid"); //can be an integer Number or a hex String
                    Object o_pid = opts.opt("pid"); //can be an integer Number or a hex String
                    int vid = o_vid instanceof Number ? ((Number) o_vid).intValue() : Integer.parseInt((String) o_vid,16);
                    int pid = o_pid instanceof Number ? ((Number) o_pid).intValue() : Integer.parseInt((String) o_pid,16);
                    String driver = opts.has("driver") ? (String) opts.opt("driver") : "CdcAcmSerialDriver";

                    if (driver.equals("FtdiSerialDriver")) {
                        customTable.addProduct(vid, pid, FtdiSerialDriver.class);
                    }
                    else if (driver.equals("CdcAcmSerialDriver")) {
                        customTable.addProduct(vid, pid, CdcAcmSerialDriver.class);
                    }
                    else if (driver.equals("Cp21xxSerialDriver")) {
                        customTable.addProduct(vid, pid, Cp21xxSerialDriver.class);
                    }
                    else if (driver.equals("ProlificSerialDriver")) {
                        customTable.addProduct(vid, pid, ProlificSerialDriver.class);
                    }
                    else if (driver.equals("Ch34xSerialDriver")) {
                        customTable.addProduct(vid, pid, Ch34xSerialDriver.class);
                    }
                    else {
                        Log.d(TAG, "Unknown driver!");
                        callbackContext.error("Unknown driver!");
                    }

                    prober = new UsbSerialProber(customTable);

                }
                else {
                    // find all available drivers from attached devices.
                    prober = UsbSerialProber.getDefaultProber();
                }

                List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(manager);

                if (!availableDrivers.isEmpty()) {
                    // get the first one as there is a high chance that there is no more than one usb device attached to your android
                    driver = availableDrivers.get(0);
                    UsbDevice device = driver.getDevice();
                    // create the intent that will be used to get the permission
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(UsbBroadcastReceiver2.USB_PERMISSION), 0);
                    // and a filter on the permission we ask
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(UsbBroadcastReceiver2.USB_PERMISSION);
                    // this broadcast receiver will handle the permission results
                    UsbBroadcastReceiver2 usbReceiver = new UsbBroadcastReceiver2(callbackContext, cordova.getActivity());
                    cordova.getActivity().registerReceiver(usbReceiver, filter);
                    // finally ask for the permission
                    manager.requestPermission(device, pendingIntent);
                }
                else {
                    // no available drivers
                    Log.d(TAG, "No device found!");
                    callbackContext.error("No device found!");
                }
            }
        });
    }

    

    /**
     * Called when the barcode scanner intent completes.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                       allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE && this.callbackContext != null) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(TEXT, intent.getStringExtra("SCAN_RESULT"));
                    obj.put(FORMAT, intent.getStringExtra("SCAN_RESULT_FORMAT"));
                    obj.put(CANCELLED, false);
                } catch (JSONException e) {
                    Log.d(TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(TEXT, "");
                    obj.put(FORMAT, "");
                    obj.put(CANCELLED, true);
                } catch (JSONException e) {
                    Log.d(TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else {
                //this.error(new PluginResult(PluginResult.Status.ERROR), this.callback);
                this.callbackContext.error("Unexpected error");
            }
        }
    }



}
