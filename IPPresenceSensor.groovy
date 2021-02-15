/**
 *  IP Presence Sensor v2.12
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Release Notes:
 *  v1.01:  Fixed a bug that could happen if a user updated from an older version of the code, but didn't click "Save Preferences".
 *  v2.01:  IP Presence Sensor adapted from iPhone Presence Sensor by Joel Wetzel
 *          Changed tries per minute to tries per hour, changed timeoutMinutes to timeoutTries.  If tries >= timeoutTries, then presence is set to OFFLINE.
 *  v2.02:  Adds valid presence check for valid HTTP status 200 response.  Presence is validated with either 408 connection refused, or status 200.
 *  v2.03:  Adds valid presence check for HTTP status 401 response and "Unauthorized".
 *
 *  v2.1 :  Major upgrade.  Moves event handler into refresh as attempt to avoid issue where multiple sequential events were generated.
 *	Consolidates HTTP checks into single conditional IF vice sequential IF... ELSE statements.
 *	Adds additional parameter for refresh rate (in seconds) when device is offline for increased check frequency.
 *	Streamlines logging to either the hub was discovered or the hub was not discovered.
 *  v2.11:  Corrected numerous syntax errors.
 *  v2.12:  Corrected final syntax errors.
 *	    
 */

import groovy.json.*
	
metadata {
	definition (name: "IP Presence Sensor", namespace: "joelwetzel", author: "Joel Wetzel")
		{capability "Refresh"
		capability "Sensor"
        	capability "Presence Sensor"}

	preferences {
	section {
	input	type: "string",
                name: "ipAddress",
                title: "IP Address",
                required: true
	input	type: "number",
                name: "triesPerHour",
                title: "Tries Per Hour",
                Description: "Test connection ${triesPerHour} per hour.",
                required: true,
                defaultValue: 12,
                range: "1..60",
                submitOnChange: true
	input	type: "number",
                name: "timeoutTries",
                title: "Timeout Tries",
                Description: "Number of attempts without a response before deciding the device is away/offline.  1 means the device will be set offline on the first attempt.",
                required: true,
		defaultValue: 1
	input	type: "number",
		name: "reconnectRate",
		title: "Reconnect Rate",
		Description: "Number of seconds between tries when a device is disconnected.",
		required: true,
		defaultValue: 60,
		range: "10..3600"
		submitOnChange: true
	input 	type: "bool",
                name: "enableDebugLogging",
                title: "Enable Debug Logging?",
                required: true,
                defaultValue: true
	input	type: "bool",
                name: "enableDevice",
                title: "Enable Device?",
                required: true,
                defaultValue: true}
	}
}

def log(msg) {if (enableDebugLogging) {log.debug(msg)}}

def installed () {Updated()}

def updated ()
	{state.tryCount = 0
	unschedule()
	if (enableDevice)
		{if (triesPerHour == null) {triesPerHour = 12}
        	m = 3600
        	resultIntDiv = m.intdiv(triesPerHour)
        	runIn(resultIntDiv, refresh)}
	runIn(2, refresh)			// But test it once, right after we install or update it too.
	}

def ensureStateVariables() {if (triesPerHour == null) {triesPerHour = 12}}

def refresh()
	{state.tryCount = (state.tryCount + 1)
	ensureStateVariables()
	if ((state.tryCount >= timeoutTries) && (device.currentValue('presence') != "not present"))
		{def descriptionText = "${device.displayName} is OFFLINE";
		log descriptionText
		sendEvent(name: "presence", value: "not present", linkText: deviceName, descriptionText: descriptionText)}
	if (ipAddress == null || ipAddress.size() == 0) {return}
	asynchttpGet("httpGetCallback", [uri: "http://${ipAddress}/", timeout: 5]);
	if ((state.tryCount = 0) && (device.currentValue('presence') != "present"))
		{def descriptionText = "${device.displayName} is ONLINE";
		log descriptionText
		sendEvent(name: "presence", value: "present", linkText: deviceName, descriptionText: descriptionText)}
	if (enableDevice)
		{if (triesPerHour == null) {triesPerHour = 12}
		m = 3600
		resultIntDiv = m.intdiv(triesPerHour)
		if (device.currentValue('presence') != "present") {resultIntDiv = reconnectRate}
		runIn(resultIntDiv, refresh)}
	}

def httpGetCallback(response, data)
	{log "${device.displayName}: httpGetCallback(${groovy.json.JsonOutput.toJson(response)}, data)"
	if (response != null && response.status == 408 && response.errorMessage.contains("Connection refused") ||
	response != null && response.status == 200 ||
	response != null && response.status == 401 && response.errorMessage.contains("Unauthorized"))
		{log "${device.displayName}: httpGetCallback(The network device was DISCOVERED: ${groovy.json.JsonOutput.toJson(response)}, data)"
		state.tryCount = 0}
	 else
    		{log "${device.displayName}: httpGetCallback(The network device was NOT DISCOVERED: ${groovy.json.JsonOutput.toJson(response)}, data)"}
	}

