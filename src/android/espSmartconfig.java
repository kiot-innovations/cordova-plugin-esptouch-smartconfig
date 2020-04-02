package com.iocare.smartconfig;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import java.util.List;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.task.__IEsptouchTask;

//import org.apache.cordova.CallbackContext;
//import org.apache.cordova.CordovaPlugin;
//import org.apache.cordova.PluginResult;
import org.apache.cordova.*;

import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class espSmartconfig extends CordovaPlugin {
  private static final String REQUEST_FINE_LOCATION = "requestFineLocation";
  private static final String ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
  private static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
  private static final int LOCATION_REQUEST_CODE = 1;
  private static final int START_SMART_CONFIG_CODE = 2;

  private WifiManager wifiManager;
  CallbackContext receivingCallbackContext = null;
  JSONArray receivedArgs;
  IEsptouchTask mEsptouchTask = null;
  private static final String TAG = "espSmartconfig";

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.wifiManager = (WifiManager) cordova.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
  }

  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext)
    throws JSONException {
    receivingCallbackContext = callbackContext; // modified by lianghuiyuan
    this.receivedArgs = args;
    if (action.equals("startConfig")) {
      return this.startSmartConfig(callbackContext, args);
    } else if (action.equals("stopConfig")) {
      return this.stopSmartConfig(callbackContext);
    } else if (action.equals("getNetworklist")) {
      return this.getNetworklist(callbackContext, args);
    } else if (action.equals(REQUEST_FINE_LOCATION)) {
      this.requestLocationPermission(LOCATION_REQUEST_CODE);
      return true;
    } else {
      callbackContext.error("can not find the function " + action);
      return false;
    }
  }

  private boolean startSmartConfig(CallbackContext callbackContext, final JSONArray args) throws JSONException {
    // Check for permission. If permission not yet granted than request permission.
    if(!cordova.hasPermission(ACCESS_COARSE_LOCATION)){
      Log.d(TAG, "Will request permission");
      requestLocationPermission(START_SMART_CONFIG_CODE);
      return true;
    }
    Log.d(TAG, "Will start smart config ");
    final String apSsid = args.getString(0);
    final String apBssid = args.getString(1);
    final String apPassword = args.getString(2);
    final String isSsidHiddenStr = args.getString(3);
    final String taskResultCountStr = args.getString(4);
    final int taskResultCount = Integer.parseInt(taskResultCountStr);
    final Object mLock = new Object();
    cordova.getThreadPool().execute(new Runnable() {
                                      public void run() {
                                        synchronized (mLock) {
                                          boolean isSsidHidden = false;
                                          if (isSsidHiddenStr.equals("YES")) {
                                            isSsidHidden = true;
                                          }
                                          cancelEspTouchTask();
                                          mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, cordova.getActivity());
                                          mEsptouchTask.setEsptouchListener(myListener);
                                        }
                                        List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
                                        IEsptouchResult firstResult = resultList.get(0);
                                        if (!firstResult.isCancelled()) {
                                          int count = 0;
                                          final int maxDisplayCount = taskResultCount;
                                          if (firstResult.isSuc()) {
                                            JSONArray resultArray = new JSONArray();
                                            for (IEsptouchResult resultInList : resultList) {
                                              JSONObject resultObj = new JSONObject();
                                              try {
                                                resultObj.put("id", count);
                                                resultObj.put("bssid", resultInList.getBssid());
                                                resultObj.put("ip", resultInList.getInetAddress().getHostAddress());
                                                resultArray.put(count, resultObj);
                                              } catch (JSONException e) {
                                                e.printStackTrace();
                                                Log.e(TAG, e.toString());
                                                sendPluginCallbackResult(PluginResult.Status.ERROR, "An unexpected error occured");
                                              }
                                              count++;
                                              if (count >= maxDisplayCount) {
                                                break;
                                              }
                                            }
                                            if (count < resultList.size()) {
                                              Log.d(TAG, "\nthere's " + (resultList.size() - count)
                                                + " more resultList(s) without showing\n");
                                            }
                                            sendPluginCallbackResult(PluginResult.Status.OK, resultArray.toString());
                                          } else {
                                            sendPluginCallbackResult(PluginResult.Status.ERROR, "No Device Found!");
                                          }
                                        }
                                      }
                                    }// end runnable
    );
    return true;
  }

  private boolean stopSmartConfig(CallbackContext callbackContext) {
    cancelEspTouchTask();
    sendPluginCallbackResult(PluginResult.Status.OK, "Cancel Success");
    return true;
  }

  private void cancelEspTouchTask(){
    if(mEsptouchTask != null){
      mEsptouchTask.interrupt();
      mEsptouchTask = null;
    }
  }

  private void sendPluginCallbackResult(PluginResult.Status status, String message) {
    PluginResult result = new PluginResult(status, message);
    result.setKeepCallback(true); // keep callback after this call
    receivingCallbackContext.sendPluginResult(result);
  }

  /**
   * Code cobtained from WiFiWizard by hoerresb This method uses the
   * callbackContext.success method to send a JSONArray of the scanned networks.
   *
   * @param callbackContext A Cordova callback context
   * @param data            JSONArray with [0] == JSONObject
   * @return true
   */
  private boolean getNetworklist(CallbackContext callbackContext, JSONArray data) {
    List<ScanResult> scanResults = wifiManager.getScanResults();

    JSONArray returnList = new JSONArray();

    Integer numLevels = null;

    if (!validateData(data)) {
      callbackContext.error("espSmartconfig: disconnectNetwork invalid data");
      Log.d(TAG, "espSmartconfig: disconnectNetwork invalid data");
      return false;
    } else if (!data.isNull(0)) {
      try {
        JSONObject options = data.getJSONObject(0);

        if (options.has("numLevels")) {
          Integer levels = options.optInt("numLevels");

          if (levels > 0) {
            numLevels = levels;
          } else if (options.optBoolean("numLevels", false)) {
            // use previous default for {numLevels: true}
            numLevels = 5;
          }
        }
      } catch (JSONException e) {
        e.printStackTrace();
        callbackContext.error(e.toString());
        return false;
      }
    }

    for (ScanResult scan : scanResults) {
      /*
       * @todo - breaking change, remove this notice when tidying new release and
       * explain changes, e.g.: 0.y.z includes a breaking change to
       * espSmartconfig.getNetworklist(). Earlier versions set scans' level attributes
       * to a number derived from wifiManager.calculateSignalLevel. This update
       * returns scans' raw RSSI value as the level, per Android spec / APIs. If your
       * application depends on the previous behaviour, we have added an options
       * object that will modify behaviour: - if `(n == true || n < 2)`,
       * `*.getNetworklist({numLevels: n})` will return data as before, split in 5
       * levels; - if `(n > 1)`, `*.getNetworklist({numLevels: n})` will calculate the
       * signal level, split in n levels; - if `(n == false)`,
       * `*.getNetworklist({numLevels: n})` will use the raw signal level;
       */

      int level;

      if (numLevels == null) {
        level = scan.level;
      } else {
        level = wifiManager.calculateSignalLevel(scan.level, numLevels);
      }

      JSONObject lvl = new JSONObject();
      try {
        lvl.put("level", level);
        lvl.put("SSID", scan.SSID);
        lvl.put("BSSID", scan.BSSID);
        lvl.put("frequency", scan.frequency);
        lvl.put("capabilities", scan.capabilities);
        // lvl.put("timestamp", scan.timestamp);
        returnList.put(lvl);
      } catch (JSONException e) {
        e.printStackTrace();
        callbackContext.error(e.toString());
        return false;
      }
    }

    callbackContext.success(returnList);
    return true;
  }

  // Code cobtained from WiFiWizard by hoerresb
  private boolean validateData(JSONArray data) {
    try {
      if (data == null || data.get(0) == null) {
        receivingCallbackContext.error("Data is null.");
        return false;
      }
      return true;
    } catch (Exception e) {
      receivingCallbackContext.error(e.getMessage());
    }
    return false;
  }

  private boolean isSDKAtLeastP() {
    return Build.VERSION.SDK_INT >= 28;
  }
  /**
   * Handle Android Permission Requests
   */
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
    throws JSONException {

    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        receivingCallbackContext.error( "PERMISSION_DENIED" );
        return;
      }
    }

    switch (requestCode) {
      case START_SMART_CONFIG_CODE:
        startSmartConfig(receivingCallbackContext, receivedArgs); // Call method again after permissions approved
        break;
      case LOCATION_REQUEST_CODE:
        receivingCallbackContext.success("PERMISSION_GRANTED");
        break;
    }
  }


  /**
   * Request ACCESS_FINE_LOCATION Permission
   *
   * @param requestCode
   */
  protected void requestLocationPermission(int requestCode) {
    // TODO: Ask for FINE_LOCATION as done in Wifiwizard.
    cordova.requestPermission(this, requestCode, ACCESS_COARSE_LOCATION);
  }

  // listener to get result
  private IEsptouchListener myListener = new IEsptouchListener() {
    @Override
    public void onEsptouchResultAdded(final IEsptouchResult result) {
      String text = "bssid=" + result.getBssid() + ",InetAddress=" + result.getInetAddress().getHostAddress();
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, text);
      pluginResult.setKeepCallback(true); // keep callback after this call
      // receivingCallbackContext.sendPluginResult(pluginResult); //modified by
      // lianghuiyuan
    }
  };
}
