/**
 *  IP Presence Sensor v2.01
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
 */

import groovy.json.*
	
metadata {
	definition (name: "IP Presence Sensor", namespace: "joelwetzel", author: "Joel Wetzel") {
		capability "Refresh"
		capability "Sensor"
        capability "Presence Sensor"
	}

	preferences {
		section {
			input type: "string",
                name: "ipAddress",
                title: "IP Address",
                required: true
            input type: "number",
                name: "triesPerHour",
                title: "Tries Per Hour",
                Description: "Test connection ${triesPerHour} per hour.",
                required: true,
                defaultValue: 12,
                range: "1..60",
                submitOnChange: true
			input type: "number",
                name: "timeoutTries",
                title: "Timeout Tries",
                Description: "Number of attempts without a response before deciding the device is away/offline.  1 means the device will be set offline on the first attempt.",
                required: true, defaultValue: 1
			input type: "bool",
                name: "enableDebugLogging",
                title: "Enable Debug Logging?",
                required: true,
                defaultValue: true
            input type: "bool",
                name: "enableDevice",
                title: "Enable Device?",
                required: true,
                defaultValue: true
		}
	}
}

def log(msg) {if (enableDebugLogging) {log.debug(msg)}}

def installed () {Updated()}

def updated () {
    state.tryCount = 0
	unschedule()
    if (enableDevice) {
        if (triesPerHour == null) {triesPerHour = 12}
        m = 3600
        resultIntDiv = m.intdiv(triesPerHour)
        runIn(resultIntDiv, refresh)
    }
    runIn(2, refresh)				// But test it once, right after we install or update it too.
}

def ensureStateVariables() {if (triesPerHour == null) {triesPerHour = 12}}

def refresh() {
	state.tryCount = (state.tryCount + 1)
    ensureStateVariables()
    if ((state.tryCount >= timeoutTries) && (device.currentValue('presence') != "not present")) {
        def descriptionText = "${device.displayName} is OFFLINE";
        log descriptionText
        sendEvent(name: "presence", value: "not present", linkText: deviceName, descriptionText: descriptionText)
    }
	if (ipAddress == null || ipAddress.size() == 0) {return}
	asynchttpGet("httpGetCallback", [uri: "http://${ipAddress}/", timeout: 5]);
    if (enableDevice) {
        if (triesPerHour == null) {triesPerHour = 12}
        m = 3600
        resultIntDiv = m.intdiv(triesPerHour)
        runIn(resultIntDiv, refresh)
    }
}

def httpGetCallback(response, data) {
	log "${device.displayName}: httpGetCallback(${groovy.json.JsonOutput.toJson(response)}, data)"
	if (response != null && response.status == 408 && response.errorMessage.contains("Connection refused")) {
        log "${device.displayName}: httpGetCallback(The following 'connection refused' result means that the hub was SUCCESSFUL in discovering the device on the network: ${groovy.json.JsonOutput.toJson(response)}, data)"
		state.tryCount = 0
		if (device.currentValue('presence') != "present") {
			def descriptionText = "${device.displayName} is ONLINE";
			log descriptionText
			sendEvent(name: "presence", value: "present", linkText: deviceName, descriptionText: descriptionText)
		}
	} else {log "${device.displayName}: httpGetCallback(The following result means that the hub was UNSUCCESSFUL in discovering the device on the network: ${groovy.json.JsonOutput.toJson(response)}, data)"}
}
