# IP Presence Sensor for Hubitat
A virtual presence sensor for Hubitat that checks if an IP device is on the local network.  Adapted from iPhone presence sensor.

- If this sensor shows "present", the device is DEFINITELY present.
- If this sensor shows "not present", the device may or may not be present.

## Compatibility
Tested with local HE LAN device, as long as IP device is present, usually results in a valid check, however recommend checking your device to make sure.
May or may not work with devices that do not respond to web requests.
Original iphone presence sensor checked for HTTP response of 408 and 'Connection refused'.  This keeps that response and also adds a valid HTTP request response of 200.  Both will result in a valid check, anything else results in not present.

## Installation

Hubitat Package manager file is not updated for recent changes.

The best way to install this code is by using [Hubitat Package Manager](https://community.hubitat.com/t/beta-hubitat-package-manager).

However, if you must install  manually:

1. Open your Hubitat web page
2. Go to the "Drivers Code" page
3. Click "+ New Driver"
4. Paste in the contents of iPhoneWiFiPresenceSensor.groovy
5. Click "Save"
6. Go to the "Devices" page
7. Click "+ Add Virtual Device"
8. Set "Device Name" and "Device Network Id" to anything you like.  Set "Type" to "iPhone Wifi Presence Sensor".
9. Click "Save Device"
10. On the device list, click the name of your new sensor
11. Set "IP Address" to the local static IP address of the iPhone.
12. Click "Save Preferences"
