module.exports = {

	start:function(ssid,bssid,pass,broadcast,maxDevices,encKey, successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "espTouchSmartconfig", "startConfig", [ssid,bssid,pass,broadcast, maxDevices, encKey]);
	},
	stop:function(successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "espTouchSmartconfig", "stopConfig", []);
    },
    requestLocationPermission: function(successCallback, errorCallback){
        cordova.exec(successCallback, errorCallback, "espTouchSmartconfig", "requestLocationPermission", []);
    }
}