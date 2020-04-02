# cordova-plugin-esptouch-smartconfig
A cordova plugin for sending wifi configurations to esp8266 and esp32 based devices using [Espressif Esptouch protocol](https://www.espressif.com/en/products/software/esp-touch/overview).

# Install

`cordova plugin add cordova-plugin-esptouch-smartconfig`

### Use in Ionic project
Ionic (typescript) project will throw error because espTouchSmartConfig is not defined. To prevent this declare espTouchSmartConfig in app.ts or any other module where you are using it.
```
// In app.component.ts
declare var espTouchSmartConfig: any;

@Component({
	...
})
...

``` 

# Usage

### To start smart config
```

/**
 * 
 * @param {string} ssid  SSID of the wifi network which you want to send too device
 * @param {string} bssid BSSID of the wifi network which you want to send to the device
 * @param {string} password Wifi Password
 * @param {boolean} how to send data(broadcast or multicast). send true for broadcasting false otherwise
 * @param {number} maxDevices Maximum number of devices you want to be configured
 * @param {string} encKey Encryption key for AES128 encryption
 * @param {function} successCallback Callback on success
 * @param {function} errorCallback  Callback on error
 */
espTouchSmartConfig.start(ssid,password,bssid,broadcast,maxDevices,encKey, (res)=>{
	// SuccessCallback
	// res is a Json String 
	//	res = [
	//		{
	//			"id": "1",	// Index of device in all configured devices.
	//			"bssid": "809010203040", // Bssid if device - usually mac address of the device
	//			"ip": "192.168.0.10" // Obtained ip by the device. 
	//		}
	//	]
	console.log(res); 
}, (err)=>{
	// Errorcallback
	console.log(err);
});

// Notes
// maxDevices: It will not guarentee the maxium number of devices picking up the config. But it'll stop as soon as confirmation is received from these many devices. 
// encKey: Keep it's value "" (Empty String) if you are not using encryption 
// This function will automatically check permission for location and ask for permission if not granted. 
```

### To stop smart config

```
espTouchSmartConfig.stop((res)=>{
	// Successcallback
}, (err)=>{
	// Errorcallback
});
```

### Request Permissions

```
espTouchSmartConfig.requestLocationPermission(()=>{
	// Permission granted. 
}, (err)=>{
	// Boo!! Permission not granted.
	console.log(err); 
});
```


# Android Permissions
This plugin requires permission for location access in Android. 

# Use with Wifiwizard
Todo

# Todo
- [ ] Ios has not been tested yet. 
