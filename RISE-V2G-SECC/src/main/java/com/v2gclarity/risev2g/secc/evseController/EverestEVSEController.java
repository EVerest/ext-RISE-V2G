
/*
 *	EVerest implementation based on DummyACEVSEController and DummyDCEVSEController
 */

package com.v2gclarity.risev2g.secc.evseController;

import javax.management.AttributeList;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.v2gclarity.risev2g.secc.session.V2GCommunicationSessionSECC;
import com.v2gclarity.risev2g.shared.utils.ByteUtils;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ACEVSEChargeParameterType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ACEVSEStatusType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EVSENotificationType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EnergyTransferModeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.MeterInfoType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PhysicalValueType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.UnitSymbolType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.DCEVSEChargeParameterType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.DCEVSEStatusCodeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.DCEVSEStatusType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.IsolationLevelType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PaymentOptionType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.SupportedEnergyTransferModeType;

import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import com.v2gclarity.risev2g.shared.utils.SleepUtils;


public class EverestEVSEController implements IACEVSEController, IDCEVSEController{
    
    @SuppressWarnings("unused")
	private V2GCommunicationSessionSECC commSessionContext;

    private Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    // Common
    private String evseID = new String("DE*PIX*E12345");	// TODO: Initialvalue
    private MeterInfoType meterInfo;
    private ArrayList<PaymentOptionType> paymentOptions;
    private ArrayList<EnergyTransferModeType> energyTransferModes;

	private boolean receiptRequired;
	private boolean freeService;
	private String debugMode = new String("None");
	private boolean failedContactError;
	private boolean stopCharging;
	private boolean authorizationEIMOkay;
	private boolean authorizationPnCOkay;
			
    // AC member
    private PhysicalValueType evseNominalVoltage;
    private PhysicalValueType evseMaxCurrent;
	private boolean rcdError;

	private boolean contactorClosed;
	private boolean contactorOpen;

    // DC member
    private PhysicalValueType targetVoltage;
    private PhysicalValueType targetCurrent;
    private PhysicalValueType maximumEVVoltageLimit;
    private PhysicalValueType maximumEVCurrentLimit;
    private PhysicalValueType maximumEVPowerLimit;
    private PhysicalValueType evseMaxVoltageLimit;
    private PhysicalValueType evseMinVoltageLimit;
    private PhysicalValueType evseMaxCurrentLimit;
    private PhysicalValueType evseMinCurrentLimit;
    private PhysicalValueType evseMaxPowerLimit;
    private PhysicalValueType peakCurrentRipple;
    private boolean evseCurrentLimitAchieved;
    private boolean evseVoltageLimitAchieved;
    private boolean evsePowerLimitAchieved;
    private IsolationLevelType isolationLevel;
	private PhysicalValueType evseCurrentRegulationTolerance;

	private PhysicalValueType evseEnergyToBeDelivered;
	private PhysicalValueType evsePresentVoltage;
	private PhysicalValueType evsePresentCurrent;
	private boolean evseUtilityInterruptEvent;
	private boolean evseMalfunction;
	private boolean evseEmergencyShutdown;

	private boolean evseIsolationMonitoringActive;

    public EverestEVSEController() {

        setIsolationLevel(IsolationLevelType.INVALID);

        ObjectHolder.mqtt.handle_cmd("charger", "set_EVSEID", (JSONObject data) -> {
            this.evseID = (String) data.get("EVSEID");
        });

        ObjectHolder.mqtt.handle_cmd("charger", "set_PaymentOptions", (JSONObject data) -> {
			this.paymentOptions = new ArrayList<PaymentOptionType>();
			JSONArray options = (JSONArray) data.get("PaymentOptions");
			for (int i = 0; i < options.size(); i++) {
				String paymentOption = (String) options.get(i);
				try {
					this.paymentOptions.add(PaymentOptionType.fromValue(paymentOption));
				} catch (IllegalArgumentException e) {
					getLogger().warn("PaymentOptionType '" + paymentOption + "' sended via mqtt is not supported");
				}
			}
		});

        ObjectHolder.mqtt.handle_cmd("charger", "set_SupportedEnergyTransferMode", (JSONObject data) -> {
			this.energyTransferModes = new ArrayList<EnergyTransferModeType>();
            JSONArray energyModes = (JSONArray) data.get("SupportedEnergyTransferMode");
            for (int i = 0; i < energyModes.size(); i++) {
                String energyTransferMode = (String) energyModes.get(i);
                try {
					this.energyTransferModes.add(EnergyTransferModeType.fromValue(energyTransferMode));
				} catch (IllegalArgumentException e) {
					getLogger().warn("EnergyTransferModeType '" + energyTransferMode + "' sended via mqtt is not supported");
				}
            }
        });

		ObjectHolder.mqtt.handle_cmd("charger", "set_AC_EVSENominalVoltage", (JSONObject data) -> {
			this.evseNominalVoltage = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSENominalVoltage")), UnitSymbolType.V);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSECurrentRegulationTolerance", (JSONObject data) -> {
			this.evseCurrentRegulationTolerance = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSECurrentRegulationTolerance")), UnitSymbolType.A);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEPeakCurrentRipple", (JSONObject data) -> {
			this.peakCurrentRipple = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEPeakCurrentRipple")), UnitSymbolType.A);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_ReceiptRequired", (JSONObject data) -> {
			this.receiptRequired = (boolean) data.get("ReceiptRequired");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_FreeService", (JSONObject data) -> {
			this.freeService = (boolean) data.get("FreeService");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_EVSEEnergyToBeDelivered", (JSONObject data) -> {
			this.evseEnergyToBeDelivered = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEEnergyToBeDelivered")), UnitSymbolType.WH);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "enable_debug_mode", (JSONObject data) -> {
			this.debugMode = (String) data.get("debug_mode");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_Auth_Okay_EIM", (JSONObject data) -> {
			this.authorizationEIMOkay = (boolean) data.get("auth_okay_eim");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_Auth_Okay_PnC", (JSONObject data) -> {
			this.authorizationPnCOkay = (boolean) data.get("auth_okay_pnc");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_FAILED_ContactorError", (JSONObject data) -> {
			this.failedContactError = (boolean) data.get("ContactorError");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_RCD_Error", (JSONObject data) -> {
			this.rcdError = (boolean) data.get("RCD");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "stop_charging", (JSONObject data) -> {
			this.stopCharging = (boolean) data.get("stop_charging");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEPresentVoltage", (JSONObject data) -> {
			this.evsePresentVoltage = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEPresentVoltage")), UnitSymbolType.V);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEPresentCurrent", (JSONObject data) -> {
			this.evsePresentCurrent = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEPresentCurrent")), UnitSymbolType.A);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_AC_EVSEMaxCurrent", (JSONObject data) -> {
			this.evseMaxCurrent = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEMaxCurrent")), UnitSymbolType.A);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEMaximumCurrentLimit", (JSONObject data) -> {
			this.evseMaxCurrentLimit = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEMaximumCurrentLimit")), UnitSymbolType.A);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEMaximumPowerLimit", (JSONObject data) -> {
			this.evseMaxPowerLimit = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEMaximumPowerLimit")), UnitSymbolType.W);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEMaximumVoltageLimit", (JSONObject data) -> {
			this.evseMaxVoltageLimit = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEMaximumVoltageLimit")), UnitSymbolType.V);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEMinimumCurrentLimit", (JSONObject data) -> {
			this.evseMinCurrentLimit = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEMinimumCurrentLimit")), UnitSymbolType.A);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_DC_EVSEMinimumVoltageLimit", (JSONObject data) -> {
			this.evseMinVoltageLimit = ObjectHolder.doubleToPhysicalValue(convertJsonObjToDouble(data.get("EVSEMinimumVoltageLimit")), UnitSymbolType.V);
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_EVSEIsolationStatus", (JSONObject data) -> {
			setIsolationLevel(IsolationLevelType.fromValue((String) data.get("EVSEIsolationStatus")));
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_EVSE_UtilityInterruptEvent", (JSONObject data) -> {
			this.evseUtilityInterruptEvent = (boolean) data.get("EVSE_UtilityInterruptEvent");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_EVSE_Malfunction", (JSONObject data) -> {
			this.evseMalfunction = (boolean) data.get("EVSE_Malfunction");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_EVSE_EmergencyShutdown", (JSONObject data) -> {
			this.evseEmergencyShutdown = (boolean) data.get("EVSE_EmergencyShutdown");
		});

		ObjectHolder.mqtt.handle_cmd("charger", "set_MeterInfo", (JSONObject data) -> {
			this.meterInfo = new MeterInfoType();
			JSONObject powermeter = (JSONObject) data.get("powermeter");
			this.meterInfo.setMeterID((String) powermeter.get("meter_id"));
		});

		ObjectHolder.mqtt.handle_cmd("charger", "contactor_closed", (JSONObject data) -> {
			boolean closed = (boolean) data.get("status");
			if (closed == true) {
				this.contactorClosed = true;
				this.contactorOpen = false;
			}
		});

		ObjectHolder.mqtt.handle_cmd("charger", "contactor_open", (JSONObject data) -> {
			boolean open = (boolean) data.get("status");
			if (open == true) {
				this.contactorClosed = false;
				this.contactorOpen = true;
			}
		});

    }

    private Logger getLogger() {
		return logger;
	}

    // Common
    @Override
    public String getEvseID() {
        return this.evseID;
    }

    @Override
	public void setCommSessionContext(V2GCommunicationSessionSECC commSessionContext) {
		this.commSessionContext = commSessionContext;
	}

    @Override
	public MeterInfoType getMeterInfo() {
		return this.meterInfo;
	}

    public V2GCommunicationSessionSECC getCommSessionContext() {
		return this.commSessionContext;
	}

	public ArrayList<PaymentOptionType> getPaymentOptions() {
		return this.paymentOptions;
	}

	public ArrayList<EnergyTransferModeType> getEnergyTransferModes() {
		return this.energyTransferModes;
	}

	public boolean getFreeService() {
		return this.freeService;
	}

	public boolean isAuthorizationEIMOkay() {
		return this.authorizationEIMOkay;
	}

	public boolean isAuthorizationPnCOkay() {
		return this.authorizationPnCOkay;
	}

	public void resetauthorizationOkay() {
		this.authorizationEIMOkay = false;
		this.authorizationPnCOkay = false;
	}

    // AC Functions

	@Override
	public JAXBElement<ACEVSEChargeParameterType> getACEVSEChargeParameter() {

		ACEVSEChargeParameterType acEVSEChargeParameter = new ACEVSEChargeParameterType();
		
		acEVSEChargeParameter.setEVSENominalVoltage(evseNominalVoltage);
		acEVSEChargeParameter.setEVSEMaxCurrent(evseMaxCurrent);
		if (this.stopCharging == true) {
			acEVSEChargeParameter.setACEVSEStatus(getACEVSEStatus(EVSENotificationType.STOP_CHARGING));
		} else {
			acEVSEChargeParameter.setACEVSEStatus(getACEVSEStatus(EVSENotificationType.NONE));
		}
		
		return new JAXBElement<ACEVSEChargeParameterType>(
				new QName("urn:iso:15118:2:2013:MsgDataTypes", "AC_EVSEChargeParameter"),
				ACEVSEChargeParameterType.class, 
				acEVSEChargeParameter);
	}
	
	@Override
	public ACEVSEStatusType getACEVSEStatus(EVSENotificationType notification) {

		ACEVSEStatusType acEVSEStatus = new ACEVSEStatusType();
        acEVSEStatus.setNotificationMaxDelay(0);
		acEVSEStatus.setEVSENotification((notification != null) ? notification : EVSENotificationType.NONE);
		acEVSEStatus.setRCD(this.rcdError);

		return acEVSEStatus;
	}

	public PhysicalValueType getEVSEMaxCurrent() {
		return this.evseMaxCurrent;
	}

	@Override
	public boolean closeContactor() {

		long startTime = System.nanoTime();
		long timeout = 0;
		long elapsedTime = 0;
		final int PERFORMANCE_TIMEOUT = 4500;

		while (contactorClosed == false && timeout < PERFORMANCE_TIMEOUT) {								
			elapsedTime = System.nanoTime() - startTime;
			timeout = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
			SleepUtils.safeSleep(TimeUnit.MILLISECONDS, 1);
		}
		return contactorClosed;
	}

	@Override
	public boolean openContactor() {
		long startTime = System.nanoTime();
		long timeout = 0;
		long elapsedTime = 0;
		final int PERFORMANCE_TIMEOUT = 4500;

		while (contactorOpen == false && timeout < PERFORMANCE_TIMEOUT) {								
			elapsedTime = System.nanoTime() - startTime;
			timeout = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
			SleepUtils.safeSleep(TimeUnit.MILLISECONDS, 1);
		}
		return contactorOpen;
	}

	public void resetContactorStatus() {
		this.contactorClosed = false;
		this.contactorOpen = true;
	}

	public void resetRCDError() {
		this.rcdError = false;
	}

    // DC Functions

	@Override
	public JAXBElement<DCEVSEChargeParameterType> getDCEVSEChargeParameter() {

		DCEVSEChargeParameterType dcEVSEChargeParameter = new DCEVSEChargeParameterType();

		if (this.stopCharging == true) {
			dcEVSEChargeParameter.setDCEVSEStatus(getDCEVSEStatus(EVSENotificationType.STOP_CHARGING));
		} else {
			dcEVSEChargeParameter.setDCEVSEStatus(getDCEVSEStatus(EVSENotificationType.NONE));
		}

		dcEVSEChargeParameter.setEVSEMaximumCurrentLimit(getEVSEMaximumCurrentLimit());
		dcEVSEChargeParameter.setEVSEMaximumPowerLimit(getEVSEMaximumPowerLimit());
		dcEVSEChargeParameter.setEVSEMaximumVoltageLimit(getEVSEMaximumVoltageLimit());
		dcEVSEChargeParameter.setEVSEMinimumCurrentLimit(getEVSEMinimumCurrentLimit());
		dcEVSEChargeParameter.setEVSEMinimumVoltageLimit(getEVSEMinimumVoltageLimit());
		dcEVSEChargeParameter.setEVSEPeakCurrentRipple(getEVSEPeakCurrentRipple());
		if (this.evseCurrentRegulationTolerance != null) {
			dcEVSEChargeParameter.setEVSECurrentRegulationTolerance(this.evseCurrentRegulationTolerance);
		}
		if (this.evseEnergyToBeDelivered != null) {
			dcEVSEChargeParameter.setEVSEEnergyToBeDelivered(this.evseEnergyToBeDelivered);
		}

		return new JAXBElement<DCEVSEChargeParameterType>(
				new QName("urn:iso:15118:2:2013:MsgDataTypes", "DC_EVSEChargeParameter"),
				DCEVSEChargeParameterType.class, 
				dcEVSEChargeParameter);
	}
	
	@Override
	public DCEVSEStatusType getDCEVSEStatus(EVSENotificationType notification) {
		DCEVSEStatusType dcEvseStatus = new DCEVSEStatusType();

		dcEvseStatus.setNotificationMaxDelay(0);
		dcEvseStatus.setEVSENotification((notification != null) ? notification : EVSENotificationType.NONE);

		if (this.evseUtilityInterruptEvent == true) {
			dcEvseStatus.setEVSEStatusCode(DCEVSEStatusCodeType.EVSE_UTILITY_INTERRUPT_EVENT);
		} else if (this.evseMalfunction == true) {
			dcEvseStatus.setEVSEStatusCode(DCEVSEStatusCodeType.EVSE_MALFUNCTION);
		} else if (this.evseEmergencyShutdown == true) {
			dcEvseStatus.setEVSEStatusCode(DCEVSEStatusCodeType.EVSE_EMERGENCY_SHUTDOWN);
		} else if (this.stopCharging == true) {
			dcEvseStatus.setEVSEStatusCode(DCEVSEStatusCodeType.EVSE_SHUTDOWN);
		} else if (this.evseIsolationMonitoringActive == true) {
			dcEvseStatus.setEVSEStatusCode(DCEVSEStatusCodeType.EVSE_ISOLATION_MONITORING_ACTIVE);
		} else {
			dcEvseStatus.setEVSEStatusCode(DCEVSEStatusCodeType.EVSE_READY);
		}

		dcEvseStatus.setEVSEIsolationStatus(getIsolationLevel());

		return dcEvseStatus;
	}

	@Override
	public void setTargetVoltage(PhysicalValueType targetVoltage) {
		this.targetVoltage = targetVoltage;
	}

	@Override
	public void setTargetCurrent(PhysicalValueType targetCurrent) {
		this.targetCurrent = targetCurrent;
	}

	@Override
	public PhysicalValueType getPresentVoltage() {
		return this.evsePresentVoltage;
	}
	
	@Override
	public PhysicalValueType getPresentCurrent() {
		return this.evsePresentCurrent;
	}

	@Override
	public void setEVMaximumVoltageLimit(PhysicalValueType maximumVoltageLimit) {
		this.maximumEVVoltageLimit = maximumVoltageLimit;
	}
	
	@Override
	public void setEVMaximumCurrentLimit(PhysicalValueType maximumCurrentLimit) {
		this.maximumEVCurrentLimit = maximumCurrentLimit;
	}

	@Override
	public void setEVMaximumPowerLimit(PhysicalValueType maximumPowerLimit) {
		this.maximumEVPowerLimit = maximumPowerLimit;
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
		return this.evseCurrentLimitAchieved;
	}

	@Override
	public boolean isEVSEVoltageLimitAchieved() {
		return this.evseVoltageLimitAchieved;
	}

	@Override
	public boolean isEVSEPowerLimitAchieved() {
		return this.evsePowerLimitAchieved;
	}

	@Override
	public PhysicalValueType getEVSEPeakCurrentRipple() {
		return this.peakCurrentRipple;
	}

	@Override
	public IsolationLevelType getIsolationLevel() {
		return this.isolationLevel;
	}

	@Override
	public void setIsolationLevel(IsolationLevelType isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	public String getDebugMode() {
		return this.debugMode;
	}

	private double convertJsonObjToDouble(Object obj) {

		double dbl = 0;

		if (obj instanceof Double) {
			dbl = (double) obj;
		}
		else if (obj instanceof Long) {
			dbl = (double) (long) obj;	// MQTT breaks if obj is converted directly to a double.
		}
		
		return dbl;
	}

	public void setIsolationMonitoringActive(boolean value) {
		this.evseIsolationMonitoringActive = value;
	}

	public boolean getReceiptRequired() {
		return false;
	}

	public boolean getStopCharging() {
		return this.stopCharging;
	}

	public void resetStopCharging() {
		this.stopCharging = false;
	}

}
