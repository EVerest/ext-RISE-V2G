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
package com.v2gclarity.risev2g.secc.evseController;

import java.math.BigInteger;

import com.v2gclarity.risev2g.secc.session.V2GCommunicationSessionSECC;
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.*;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.json.simple.JSONObject;

public class EverestDCEVSEController implements IDCEVSEController {

	private V2GCommunicationSessionSECC commSessionContext;
	private IsolationLevelType isolationLevel;
    
    private String evseID;
    private EVSENotificationType notification;
    private int notificationMaxDelay;
    private MeterInfoType meterInfo;
    private PhysicalValueType presentCurrent;
    private PhysicalValueType presentVoltage;
    private PhysicalValueType evseMaxCurrentLimit;
    private PhysicalValueType evseMaxVoltageLimit;
    private PhysicalValueType evseMaxPowerLimit;
    private PhysicalValueType evseMinCurrentLimit;
    private PhysicalValueType evseMinVoltageLimit;
    private PhysicalValueType peakCurrentRipple;    // what is a peak-to-peak current ripple??
    private boolean receiptRequired = false;
    private boolean EVSECurrentLimitAchieved = false;
    private boolean EVSEVoltageLimitAchieved = false;
    private boolean EVSEPowerLimitAchieved = false;

	
	public EverestDCEVSEController() {
		setIsolationLevel(IsolationLevelType.INVALID);
        
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evseid", (JSONObject data) -> {
			evseID = (String) data.get("id");
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_notification", (JSONObject data) -> {
			switch ((String) data.get("notification")) {
				case "None": notification = EVSENotificationType.NONE; break;
				case "StopCharging": notification = EVSENotificationType.STOP_CHARGING; break;
				case "ReNegotiation": notification = EVSENotificationType.RE_NEGOTIATION; break;
			}
			notificationMaxDelay = (int) data.get("max_delay");
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_meter_reading", (JSONObject data) -> {
            meterInfo = new MeterInfoType();
            meterInfo.setMeterID((String) data.get("id"));
            meterInfo.setMeterReading(BigInteger.valueOf((long) ((double) data.get("value"))));
            meterInfo.setTMeter((long) data.get("timestamp"));
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_receipt_required", (JSONObject data) -> {
			receiptRequired = (boolean) data.get("receipt_required");
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_present_voltage", (JSONObject data) -> {
			presentVoltage = ObjectHolder.doubleToPhysicalValue((double) data.get("voltage"), UnitSymbolType.V);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_present_current", (JSONObject data) -> {
			presentCurrent = ObjectHolder.doubleToPhysicalValue((double) data.get("current"), UnitSymbolType.A);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_maximum_current_limit", (JSONObject data) -> {
			evseMaxCurrentLimit = ObjectHolder.doubleToPhysicalValue((double) data.get("max_current"), UnitSymbolType.A);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_maximum_power_limit", (JSONObject data) -> {
			evseMaxPowerLimit = ObjectHolder.doubleToPhysicalValue((double) data.get("max_power"), UnitSymbolType.W);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_maximum_voltage_limit", (JSONObject data) -> {
			evseMaxVoltageLimit = ObjectHolder.doubleToPhysicalValue((double) data.get("max_voltage"), UnitSymbolType.V);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_minimum_current_limit", (JSONObject data) -> {
			evseMinCurrentLimit = ObjectHolder.doubleToPhysicalValue((double) data.get("min_current"), UnitSymbolType.A);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_minimum_voltage_limit", (JSONObject data) -> {
			evseMinVoltageLimit = ObjectHolder.doubleToPhysicalValue((double) data.get("min_voltage"), UnitSymbolType.V);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_peak_current_ripple", (JSONObject data) -> {
			peakCurrentRipple = ObjectHolder.doubleToPhysicalValue((double) data.get("current"), UnitSymbolType.A);
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_current_limit_achieved", (JSONObject data) -> {
			EVSECurrentLimitAchieved = (boolean) data.get("current_limit_achieved");
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_voltage_limit_achieved", (JSONObject data) -> {
			EVSEVoltageLimitAchieved = (boolean) data.get("voltage_limit_achieved");
		});
        ObjectHolder.mqtt.handle_cmd("dc_charger", "set_evse_power_limit_achieved", (JSONObject data) -> {
			EVSEPowerLimitAchieved = (boolean) data.get("power_limit_achieved");
		});
	}
	
	@Override
	public String getEvseID() {
		return evseID;
	}
	
	
	@Override
	public JAXBElement<DCEVSEChargeParameterType> getDCEVSEChargeParameter() {
		DCEVSEChargeParameterType dcEVSEChargeParameter = new DCEVSEChargeParameterType();
		
		dcEVSEChargeParameter.setDCEVSEStatus(getDCEVSEStatus(null));
		dcEVSEChargeParameter.setEVSEMaximumCurrentLimit(getEVSEMaximumCurrentLimit());
		dcEVSEChargeParameter.setEVSEMaximumPowerLimit(getEVSEMaximumPowerLimit());
		dcEVSEChargeParameter.setEVSEMaximumVoltageLimit(getEVSEMaximumVoltageLimit());
		dcEVSEChargeParameter.setEVSEMinimumCurrentLimit(getEVSEMinimumCurrentLimit());
		dcEVSEChargeParameter.setEVSEMinimumVoltageLimit(getEVSEMinimumVoltageLimit());
		dcEVSEChargeParameter.setEVSEPeakCurrentRipple(getEVSEPeakCurrentRipple());
		
		return new JAXBElement<DCEVSEChargeParameterType>(
				new QName("urn:iso:15118:2:2013:MsgDataTypes", "DC_EVSEChargeParameter"),
				DCEVSEChargeParameterType.class, 
				dcEVSEChargeParameter);
	}
	
	
	public V2GCommunicationSessionSECC getCommSessionContext() {
		return commSessionContext;
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
	public DCEVSEStatusType getDCEVSEStatus(EVSENotificationType notification) {
		DCEVSEStatusType dcEvseStatus = new DCEVSEStatusType();
		dcEvseStatus.setNotificationMaxDelay(this.notificationMaxDelay);
		dcEvseStatus.setEVSENotification((this.notification != null) ? this.notification : EVSENotificationType.NONE);
		dcEvseStatus.setEVSEStatusCode(DCEVSEStatusCodeType.EVSE_READY);
		dcEvseStatus.setEVSEIsolationStatus(getIsolationLevel());
		
		return dcEvseStatus;
	}

	@Override
	public void setTargetVoltage(PhysicalValueType targetVoltage) {
		ObjectHolder.mqtt.publish_var("dc_charger", "ev_target_voltage", ObjectHolder.physicalValueToDouble(targetVoltage));
	}

	@Override
	public void setTargetCurrent(PhysicalValueType targetCurrent) {
        ObjectHolder.mqtt.publish_var("dc_charger", "ev_target_current", ObjectHolder.physicalValueToDouble(targetCurrent));
	}

	@Override
	public PhysicalValueType getPresentVoltage() {
		return this.presentVoltage;
	}
	
	
	@Override
	public PhysicalValueType getPresentCurrent() {
		return this.presentCurrent;
	}

	@Override
	public void setEVMaximumVoltageLimit(PhysicalValueType maximumVoltageLimit) {
        ObjectHolder.mqtt.publish_var("dc_charger", "ev_maximum_voltage_limit", ObjectHolder.physicalValueToDouble(maximumVoltageLimit));
	}
	
	
	@Override
	public void setEVMaximumCurrentLimit(PhysicalValueType maximumCurrentLimit) {
        ObjectHolder.mqtt.publish_var("dc_charger", "ev_maximum_current_limit", ObjectHolder.physicalValueToDouble(maximumCurrentLimit));
	}

	@Override
	public void setEVMaximumPowerLimit(PhysicalValueType maximumPowerLimit) {
        ObjectHolder.mqtt.publish_var("dc_charger", "ev_maximum_power_limit", ObjectHolder.physicalValueToDouble(maximumPowerLimit));
	}


	@Override
	public PhysicalValueType getEVSEMaximumVoltageLimit() {
		return this.evseMaxVoltageLimit;
	}
	
	
	@Override
	public PhysicalValueType getEVSEMinimumVoltageLimit() {
		return this.evseMinVoltageLimit;
	}
	

	@Override
	public PhysicalValueType getEVSEMaximumCurrentLimit() {
		return this.evseMaxCurrentLimit;
	}
	
	
	@Override
	public PhysicalValueType getEVSEMinimumCurrentLimit() {
		return this.evseMinCurrentLimit;
	}

	@Override
	public PhysicalValueType getEVSEMaximumPowerLimit() {
		return this.evseMaxPowerLimit;
	}

	@Override
	public boolean isEVSECurrentLimitAchieved() {
		return this.EVSECurrentLimitAchieved;
	}

	@Override
	public boolean isEVSEVoltageLimitAchieved() {
		return this.EVSEVoltageLimitAchieved;
	}

	@Override
	public boolean isEVSEPowerLimitAchieved() {
		return this.EVSEPowerLimitAchieved;
	}

	@Override
	public MeterInfoType getMeterInfo() {
		return meterInfo;
	}

	@Override
	public PhysicalValueType getEVSEPeakCurrentRipple() {
		return this.peakCurrentRipple;    // what is a peak-to-peak current ripple??
	}

	@Override
	public IsolationLevelType getIsolationLevel() {
		return isolationLevel;
	}

	@Override
	public void setIsolationLevel(IsolationLevelType isolationLevel) {
		this.isolationLevel = isolationLevel;
	}
	
	public boolean getReceiptRequired() {
        return receiptRequired;
    }
}
