var exec = require('cordova/exec'),
cordova = require('cordova');

module.exports = {

	start:function(apSsid,apBssid,apPassword,broadcast,taskResultCountStr,encKey, successCallback, errorCallback){
		exec(successCallback, errorCallback, "espTouchSmartconfig", "startConfig", [apSsid,apBssid,apPassword,broadcast, taskResultCountStr, encKey]);
	},
	stop:function(successCallback, errorCallback){
		exec(successCallback, errorCallback, "espTouchSmartconfig", "stopConfig", []);
    },
    requestLocationPermission: function(successCallback, errorCallback){
        exec(successCallback, errorCallback, "espTouchSmartconfig", "requestLocationPermission", []);
    }
}