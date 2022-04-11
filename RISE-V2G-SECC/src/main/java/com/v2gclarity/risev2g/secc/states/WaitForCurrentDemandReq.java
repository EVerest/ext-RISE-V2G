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
package com.v2gclarity.risev2g.secc.states;

import com.v2gclarity.risev2g.secc.evseController.IDCEVSEController;
import com.v2gclarity.risev2g.secc.session.V2GCommunicationSessionSECC;
import com.v2gclarity.risev2g.shared.enumerations.V2GMessages;
import com.v2gclarity.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.BodyBaseType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.CurrentDemandReqType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.CurrentDemandResType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EVSENotificationType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PaymentOptionType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ResponseCodeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;

// *** EVerest code start ***
import com.v2gclarity.risev2g.secc.evseController.*;
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
// *** EVerest code end ***

public class WaitForCurrentDemandReq extends ServerState {

	private CurrentDemandResType currentDemandRes;
	
	public WaitForCurrentDemandReq(V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
		currentDemandRes = new CurrentDemandResType();
	}
	
	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, CurrentDemandReqType.class, currentDemandRes)) {
			V2GMessage v2gMessageReq = (V2GMessage) message;
			CurrentDemandReqType currentDemandReq = 
					(CurrentDemandReqType) v2gMessageReq.getBody().getBodyElement().getValue();

			// *** EVerest code start ***
			if (currentDemandReq.isBulkChargingComplete() != null) {
				ObjectHolder.mqtt.publish_var("charger", "DC_BulkChargingComplete", currentDemandReq.isBulkChargingComplete());
			}
			ObjectHolder.mqtt.publish_var("charger", "DC_ChargingComplete", currentDemandReq.isChargingComplete());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVReady", currentDemandReq.getDCEVStatus().isEVReady());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVErrorCode", currentDemandReq.getDCEVStatus().getEVErrorCode().value());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVRESSSOC", currentDemandReq.getDCEVStatus().getEVRESSSOC());

			double DC_EVTargetVoltage = currentDemandReq.getEVTargetVoltage().getValue() * Math.pow(10, currentDemandReq.getEVTargetVoltage().getMultiplier());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVTargetVoltage", DC_EVTargetVoltage);
			
			double DC_EVTargetCurrent = currentDemandReq.getEVTargetCurrent().getValue() * Math.pow(10, currentDemandReq.getEVTargetCurrent().getMultiplier());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVTargetCurrent", DC_EVTargetCurrent);

			if (currentDemandReq.getEVMaximumCurrentLimit() != null) {
				double DC_EVMaximumCurrentLimit = currentDemandReq.getEVMaximumCurrentLimit().getValue() * Math.pow(10, currentDemandReq.getEVMaximumCurrentLimit().getMultiplier());
				ObjectHolder.mqtt.publish_var("charger", "DC_EVMaximumCurrentLimit", DC_EVMaximumCurrentLimit);
			}
			if (currentDemandReq.getEVMaximumVoltageLimit() != null) {
				double DC_EVMaximumVoltageLimit = currentDemandReq.getEVMaximumVoltageLimit().getValue() * Math.pow(10, currentDemandReq.getEVMaximumVoltageLimit().getMultiplier());
				ObjectHolder.mqtt.publish_var("charger", "DC_EVMaximumVoltageLimit", DC_EVMaximumVoltageLimit);
			}
			if (currentDemandReq.getEVMaximumPowerLimit() != null) {
				double DC_EVMaximumPowerLimit = currentDemandReq.getEVMaximumPowerLimit().getValue() * Math.pow(10, currentDemandReq.getEVMaximumPowerLimit().getMultiplier());
				ObjectHolder.mqtt.publish_var("charger", "DC_EVMaximumPowerLimit", DC_EVMaximumPowerLimit);
			}

			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

			if (currentDemandReq.getRemainingTimeToBulkSoC() != null) {
				long seconds_toBulkSOC = (long) (currentDemandReq.getRemainingTimeToBulkSoC().getValue() * 
								Math.pow(10, currentDemandReq.getRemainingTimeToBulkSoC().getMultiplier()));
				LocalDateTime dateTime_toBulkSOC = now.plusSeconds(seconds_toBulkSOC);
				String dateTime_toBulkSOC_formatted = dateTime_toBulkSOC.format(myFormatObj);

				ObjectHolder.mqtt.publish_var("charger", "EV_RemainingTimeToFullSoC", dateTime_toBulkSOC_formatted);
			}
			if (currentDemandReq.getRemainingTimeToFullSoC() != null) {
				long seconds_toFullSOC = (long) (currentDemandReq.getRemainingTimeToFullSoC().getValue() * 
								Math.pow(10, currentDemandReq.getRemainingTimeToFullSoC().getMultiplier()));
				LocalDateTime dateTime_toFullSOC = now.plusSeconds(seconds_toFullSOC);
				String dateTime_toFullSOC_formatted = dateTime_toFullSOC.format(myFormatObj);

				ObjectHolder.mqtt.publish_var("charger", "EV_RemainingTimeToFullSoC", dateTime_toFullSOC_formatted);
			}

			// *** EVerest code end ***
			
			IDCEVSEController evseController = getCommSessionContext().getDCEvseController();
			
			evseController.setEVMaximumCurrentLimit(currentDemandReq.getEVMaximumCurrentLimit());
			evseController.setEVMaximumVoltageLimit(currentDemandReq.getEVMaximumVoltageLimit());
			evseController.setEVMaximumPowerLimit(currentDemandReq.getEVMaximumPowerLimit());
			// TODO how to deal with the remaining parameters of currentDemandReq?
			
			/*
			 * TODO check if a renegotiation is wanted or not
			 * Change EVSENotificationType to NONE if you want more than one charge loop iteration, 
			 * but then make sure the EV is stopping the charge loop
			 */

			EVSENotificationType notification = EVSENotificationType.NONE;
			if (((EverestEVSEController)getCommSessionContext().getDCEvseController()).getStopCharging()) {
				notification = EVSENotificationType.STOP_CHARGING;
			}
			currentDemandRes.setDCEVSEStatus(evseController.getDCEVSEStatus(notification));
			
			currentDemandRes.setEVSECurrentLimitAchieved(evseController.isEVSECurrentLimitAchieved());
			currentDemandRes.setEVSEVoltageLimitAchieved(evseController.isEVSEVoltageLimitAchieved());
			currentDemandRes.setEVSEPowerLimitAchieved(evseController.isEVSEPowerLimitAchieved());
			currentDemandRes.setEVSEID(evseController.getEvseID());
			currentDemandRes.setEVSEMaximumCurrentLimit(evseController.getEVSEMaximumCurrentLimit());
			currentDemandRes.setEVSEMaximumVoltageLimit(evseController.getEVSEMaximumVoltageLimit());
			currentDemandRes.setEVSEMaximumPowerLimit(evseController.getEVSEMaximumPowerLimit());
			currentDemandRes.setEVSEPresentCurrent(evseController.getPresentCurrent());
			currentDemandRes.setEVSEPresentVoltage(evseController.getPresentVoltage());
			currentDemandRes.setMeterInfo(evseController.getMeterInfo());
			getCommSessionContext().setSentMeterInfo(evseController.getMeterInfo());
			currentDemandRes.setSAScheduleTupleID(getCommSessionContext().getChosenSAScheduleTuple());
			
			// Optionally indicate that the EVCC is required to send a MeteringReceiptReq message 
			if (getCommSessionContext().getSelectedPaymentOption().equals(PaymentOptionType.EXTERNAL_PAYMENT)) {
				// In EIM, there is never a MeteringReceiptReq/-Res message pair, therefore it is set to false here
				currentDemandRes.setReceiptRequired(false);
			} else {
				// Optionally set to true, but only in PnC mode according to [V2G2-691]
                // *** EVerest code start ***
                //currentDemandRes.setReceiptRequired(false);
                currentDemandRes.setReceiptRequired(((EverestEVSEController)getCommSessionContext().getDCEvseController()).getReceiptRequired());
                // *** EVerest code end ***
			}
			
			if (currentDemandRes.isReceiptRequired()) {
				return getSendMessage(currentDemandRes, V2GMessages.METERING_RECEIPT_REQ);
			} else {
				((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
				.getAllowedRequests().add(V2GMessages.CURRENT_DEMAND_REQ);
				((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
				.getAllowedRequests().add(V2GMessages.POWER_DELIVERY_REQ);
				
				return getSendMessage(currentDemandRes, V2GMessages.FORK);
			}
		} else {
			if (currentDemandRes.getResponseCode().equals(ResponseCodeType.FAILED_SEQUENCE_ERROR)) {
				BodyBaseType responseMessage = getSequenceErrorResMessage(new CurrentDemandResType(), message);
				
				return getSendMessage(responseMessage, V2GMessages.NONE, currentDemandRes.getResponseCode());
			} else {
				setMandatoryFieldsForFailedRes(currentDemandRes, currentDemandRes.getResponseCode());
			}
		}
		
		return getSendMessage(currentDemandRes, V2GMessages.NONE, currentDemandRes.getResponseCode());
	}

	@Override
	public BodyBaseType getResponseMessage() {
		return currentDemandRes;
	}

}
