module.exports = {

	start:function(ssid,bssid,pass,broadcast,maxDevices,encKey, successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "espTouchSmartConfig", "startConfig", [ssid,bssid,pass,broadcast, maxDevices, encKey]);
	},
	stop:function(successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "espTouchSmartConfig", "stopConfig", []);
    	},
    	requestLocationPermission: function(successCallback, errorCallback){
        	cordova.exec(successCallback, errorCallback, "espTouchSmartConfig", "requestLocationPermission", []);
    	}
}
