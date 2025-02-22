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
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EVSENotificationType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PreChargeReqType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PreChargeResType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ResponseCodeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;

// *** EVerest code start ***
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import com.v2gclarity.risev2g.secc.evseController.EverestEVSEController; 
// *** EVerest code end ***

public class WaitForPreChargeReq extends ServerState {

	private PreChargeResType preChargeRes;
	
	public WaitForPreChargeReq(V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
		preChargeRes = new PreChargeResType();
	}
	
	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, PreChargeReqType.class, preChargeRes)) {
			V2GMessage v2gMessageReq = (V2GMessage) message;
			PreChargeReqType preChargeReq = 
					(PreChargeReqType) v2gMessageReq.getBody().getBodyElement().getValue();
			
			EVSENotificationType notification = EVSENotificationType.NONE;
			if (((EverestEVSEController)getCommSessionContext().getDCEvseController()).getStopCharging()) {
				notification = EVSENotificationType.STOP_CHARGING;
			}
				
			// *** EVerest code start ***
			ObjectHolder.mqtt.publish_var("charger", "DC_EVReady", preChargeReq.getDCEVStatus().isEVReady());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVErrorCode", preChargeReq.getDCEVStatus().getEVErrorCode().value());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVRESSSOC", preChargeReq.getDCEVStatus().getEVRESSSOC());
			double DC_EVTargetVoltage = preChargeReq.getEVTargetVoltage().getValue() * Math.pow(10, preChargeReq.getEVTargetVoltage().getMultiplier());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVTargetVoltage", DC_EVTargetVoltage);
			double DC_EVTargetCurrent = preChargeReq.getEVTargetCurrent().getValue() * Math.pow(10, preChargeReq.getEVTargetCurrent().getMultiplier());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVTargetCurrent", DC_EVTargetCurrent);
			// *** EVerest code end ***

			// TODO how to react to failure status of DCEVStatus of cableCheckReq?
			
			IDCEVSEController evseController = (IDCEVSEController) getCommSessionContext().getDCEvseController();

			preChargeRes.setDCEVSEStatus(evseController.getDCEVSEStatus(notification));
			preChargeRes.setEVSEPresentVoltage(evseController.getPresentVoltage());
			
			((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
			.getAllowedRequests().add(V2GMessages.PRE_CHARGE_REQ);
			((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
			.getAllowedRequests().add(V2GMessages.POWER_DELIVERY_REQ);
		} else {
			if (preChargeRes.getResponseCode().equals(ResponseCodeType.FAILED_SEQUENCE_ERROR)) {
				BodyBaseType responseMessage = getSequenceErrorResMessage(new PreChargeResType(), message);
				
				return getSendMessage(responseMessage, V2GMessages.NONE, preChargeRes.getResponseCode());
			} else {
				setMandatoryFieldsForFailedRes(preChargeRes, preChargeRes.getResponseCode());
			}
		}
		
		return getSendMessage(preChargeRes, 
							  (preChargeRes.getResponseCode().toString().startsWith("OK") ? 
							  V2GMessages.FORK : V2GMessages.NONE),
							  preChargeRes.getResponseCode()
						 	 );
	}


	@Override
	public BodyBaseType getResponseMessage() {
		return preChargeRes;
	}
}
