/*******************************************************************************
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2015 - 2019  Dr. Marc Mültin (V2G Clarity)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *******************************************************************************/
/*
 *	EVerest implementation based on DummyACEVSEController
 */
package com.v2gclarity.risev2g.secc.evseController;

import com.v2gclarity.risev2g.secc.session.V2GCommunicationSessionSECC;
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.*;
import org.json.simple.JSONObject;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;

public class EverestACEVSEController implements IACEVSEController {

	private V2GCommunicationSessionSECC commSessionContext; @SuppressWarnings("unused")
	
    private String evseID;
	private EVSENotificationType notification;
	private int notificationMaxDelay;
	private double nominalVoltage;
	private double maxCurrent;
	private boolean rcdErr = false;
    private MeterInfoType meterInfo;
    private boolean receiptRequired = false;

	public EverestACEVSEController() {
		ObjectHolder.mqtt.handle_cmd("ac_charger", "set_evseid", (JSONObject data) -> {
			evseID = (String) data.get("id");
		});
		ObjectHolder.mqtt.handle_cmd("ac_charger", "set_nominal_voltage", (JSONObject data) -> {
			nominalVoltage = (double) data.get("voltage");
		});
		ObjectHolder.mqtt.handle_cmd("ac_charger", "set_max_current", (JSONObject data) -> {
			maxCurrent = (double) data.get("max_current");
		});
		ObjectHolder.mqtt.handle_cmd("ac_charger", "set_evse_notification", (JSONObject data) -> {
			switch ((String) data.get("notification")) {
				case "None": notification = EVSENotificationType.NONE; break;
				case "StopCharging": notification = EVSENotificationType.STOP_CHARGING; break;
				case "ReNegotiation": notification = EVSENotificationType.RE_NEGOTIATION; break;
			}
			notificationMaxDelay = (int) data.get("max_delay");
		});
		ObjectHolder.mqtt.handle_cmd("ac_charger", "set_rcd", (JSONObject data) -> {
			rcdErr = (boolean) data.get("rcd_err");
		});
		ObjectHolder.mqtt.handle_cmd("ac_charger", "set_meter_reading", (JSONObject data) -> {
            meterInfo = new MeterInfoType();
            meterInfo.setMeterID((String) data.get("id"));
            meterInfo.setMeterReading(BigInteger.valueOf((long) ((double) data.get("value"))));
            meterInfo.setTMeter((long) data.get("timestamp"));
		});
        ObjectHolder.mqtt.handle_cmd("ac_charger", "set_receipt_required", (JSONObject data) -> {
			receiptRequired = (boolean) data.get("receipt_required");
		});
    }

	@Override
	public String getEvseID() {
		return evseID;
	}


	@Override
	public JAXBElement<ACEVSEChargeParameterType> getACEVSEChargeParameter() {
		ACEVSEChargeParameterType acEVSEChargeParameter = new ACEVSEChargeParameterType();
		
		PhysicalValueType evseNominalVoltage = ObjectHolder.doubleToPhysicalValue(nominalVoltage, UnitSymbolType.V);
		acEVSEChargeParameter.setEVSENominalVoltage(evseNominalVoltage);
		
		PhysicalValueType evseMaxCurrent = ObjectHolder.doubleToPhysicalValue(maxCurrent, UnitSymbolType.A);
		acEVSEChargeParameter.setEVSEMaxCurrent(evseMaxCurrent);
		
		acEVSEChargeParameter.setACEVSEStatus(getACEVSEStatus(null));
		return new JAXBElement<ACEVSEChargeParameterType>(
				new QName("urn:iso:15118:2:2013:MsgDataTypes", "AC_EVSEChargeParameter"),
				ACEVSEChargeParameterType.class, 
				acEVSEChargeParameter);
	}


	@Override
	public ACEVSEStatusType getACEVSEStatus(EVSENotificationType notification) {
		ACEVSEStatusType acEVSEStatus = new ACEVSEStatusType();
        acEVSEStatus.setNotificationMaxDelay(notificationMaxDelay);
		acEVSEStatus.setEVSENotification((this.notification != null) ? this.notification : EVSENotificationType.NONE);
		acEVSEStatus.setNotificationMaxDelay(notificationMaxDelay);
		acEVSEStatus.setRCD(rcdErr);
		
		return acEVSEStatus;
	}

	
	@Override
	public void setCommSessionContext(V2GCommunicationSessionSECC commSessionContext) {
		this.commSessionContext = commSessionContext;
	}


	@Override
	public boolean closeContactor() {
		// A check for CP state B would be necessary
		return true;
	}


	@Override
	public boolean openContactor() {
		return true;
	}


	@Override
	public MeterInfoType getMeterInfo() {
        return meterInfo;
	}
	
	public boolean getReceiptRequired() {
        return receiptRequired;
    }
}
