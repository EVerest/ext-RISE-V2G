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
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ResponseCodeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.WeldingDetectionReqType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.WeldingDetectionResType;

// *** EVerest code start ***
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import com.v2gclarity.risev2g.secc.evseController.EverestEVSEController;
// *** EVerest code end ***

public class WaitForWeldingDetectionReq extends ServerState {

	private WeldingDetectionResType weldingDetectionRes;
	
	public WaitForWeldingDetectionReq(V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
		weldingDetectionRes = new WeldingDetectionResType();
	}
	
	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, WeldingDetectionReqType.class, weldingDetectionRes)) {
			V2GMessage v2gMessageReq = (V2GMessage) message;
			WeldingDetectionReqType weldingDetectionReq = 
					(WeldingDetectionReqType) v2gMessageReq.getBody().getBodyElement().getValue();

			EVSENotificationType notification = EVSENotificationType.NONE;
			if (((EverestEVSEController)getCommSessionContext().getDCEvseController()).getStopCharging()) {
				notification = EVSENotificationType.STOP_CHARGING;
			}

			// *** EVerest code start ***
			ObjectHolder.mqtt.publish_var("charger", "DC_EVReady", weldingDetectionReq.getDCEVStatus().isEVReady());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVErrorCode", weldingDetectionReq.getDCEVStatus().getEVErrorCode().value());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVRESSSOC", weldingDetectionReq.getDCEVStatus().getEVRESSSOC());
			// *** EVerest code end ***
			
			// TODO how to react to failure status of DCEVStatus of weldingDetectionReq?
			
			IDCEVSEController evseController = (IDCEVSEController) getCommSessionContext().getDCEvseController();
			
			weldingDetectionRes.setDCEVSEStatus(evseController.getDCEVSEStatus(notification));
			weldingDetectionRes.setEVSEPresentVoltage(evseController.getPresentVoltage());
			
			((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
			.getAllowedRequests().add(V2GMessages.WELDING_DETECTION_REQ);
			((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
			.getAllowedRequests().add(V2GMessages.SESSION_STOP_REQ);
		} else {
			if (weldingDetectionRes.getResponseCode().equals(ResponseCodeType.FAILED_SEQUENCE_ERROR)) {
				BodyBaseType responseMessage = getSequenceErrorResMessage(new WeldingDetectionResType(), message);
				
				return getSendMessage(responseMessage, V2GMessages.NONE, weldingDetectionRes.getResponseCode());
			} else {
				setMandatoryFieldsForFailedRes(weldingDetectionRes, weldingDetectionRes.getResponseCode());
			}
		}
		
		return getSendMessage(weldingDetectionRes, 
				 			  (weldingDetectionRes.getResponseCode().toString().startsWith("OK") ? 
				 			  V2GMessages.FORK : V2GMessages.NONE),
				 			  weldingDetectionRes.getResponseCode()
			 			 	 );
	}


	@Override
	public BodyBaseType getResponseMessage() {
		return weldingDetectionRes;
	}
}
