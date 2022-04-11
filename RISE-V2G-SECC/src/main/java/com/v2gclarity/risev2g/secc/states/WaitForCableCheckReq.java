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

import com.v2gclarity.risev2g.secc.evseController.EverestEVSEController;
import com.v2gclarity.risev2g.secc.evseController.IDCEVSEController;
import com.v2gclarity.risev2g.secc.session.V2GCommunicationSessionSECC;
import com.v2gclarity.risev2g.shared.enumerations.V2GMessages;
import com.v2gclarity.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.BodyBaseType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.CableCheckReqType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.CableCheckResType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EVSENotificationType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EVSEProcessingType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.IsolationLevelType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ResponseCodeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;

// *** EVerest code start ***
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
// *** EVerest code end ***

public class WaitForCableCheckReq extends ServerState {

	private CableCheckResType cableCheckRes;
	private boolean evseProcessingFinished;

	// *** EVerest code start ***
	private boolean isolationCheckRequested;
	// *** EVerest code end ***
	
	public WaitForCableCheckReq(V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
		cableCheckRes = new CableCheckResType();

		// *** EVerest code start ***
		setIsolationCheckRequested(false);
		setEvseProcessingFinished(false);
		// *** EVerest code end ***
	}
	
	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, CableCheckReqType.class, cableCheckRes)) {
			V2GMessage v2gMessageReq = (V2GMessage) message;
			CableCheckReqType cableCheckReq = 
					(CableCheckReqType) v2gMessageReq.getBody().getBodyElement().getValue();
			
			// *** EVerest code start ***
			EverestEVSEController everestController = (EverestEVSEController)getCommSessionContext().getEvseController(); 

			if (getIsolationCheckRequested() == false) {
				ObjectHolder.mqtt.publish_var("charger", "Require_EVSEIsolationCheck", null);
				setIsolationCheckRequested(true);
				everestController.setIsolationMonitoringActive(true);
				ObjectHolder.mqtt.publish_var("charger", "DC_EVReady", cableCheckReq.getDCEVStatus().isEVReady());
	      			ObjectHolder.mqtt.publish_var("charger", "DC_EVErrorCode", cableCheckReq.getDCEVStatus().getEVErrorCode().value());
			ObjectHolder.mqtt.publish_var("charger", "DC_EVRESSSOC", cableCheckReq.getDCEVStatus().getEVRESSSOC());
			} else {
				if (!(everestController.getIsolationLevel().equals(IsolationLevelType.INVALID))) {
					setEvseProcessingFinished(true);
				}
			}
			// *** EVerest code end ***

			EVSENotificationType notification = EVSENotificationType.NONE;
			if (((EverestEVSEController)getCommSessionContext().getDCEvseController()).getStopCharging()) {
				notification = EVSENotificationType.STOP_CHARGING;
			}
			cableCheckRes.setDCEVSEStatus(getCommSessionContext().getDCEvseController().getDCEVSEStatus(notification));
			
			// TODO how to react to failure status of DCEVStatus of cableCheckReq?
			
			/*
			 * TODO we need a timeout mechanism here so that a response can be sent within 2s
			 * the DCEVSEStatus should be generated according to already available values
			 * (if EVSEProcessing == ONGOING, maybe because of EVSE_IsolationMonitoringActive,
			 * within a certain timeout, then the status must be different)
			 */
			
			//setEvseProcessingFinished(true);
			
			if (isEvseProcessingFinished()) {
				// As soon as EVSEProcessing is set to Finished, the IsolationLevelType should be set to valid
				getCommSessionContext().getDCEvseController().setIsolationLevel(IsolationLevelType.VALID);
				
				// *** EVerest code start ***
				((EverestEVSEController) getCommSessionContext().getDCEvseController()).setIsolationMonitoringActive(false);
				// *** EVerest code end ***

				cableCheckRes.setEVSEProcessing(EVSEProcessingType.FINISHED);
				
				return getSendMessage(cableCheckRes, V2GMessages.PRE_CHARGE_REQ);
			} else {
				cableCheckRes.setEVSEProcessing(EVSEProcessingType.ONGOING);
				return getSendMessage(cableCheckRes, V2GMessages.CABLE_CHECK_REQ);
			}
		} else {
			if (cableCheckRes.getResponseCode().equals(ResponseCodeType.FAILED_SEQUENCE_ERROR)) {
				BodyBaseType responseMessage = getSequenceErrorResMessage(new CableCheckResType(), message);
				
				return getSendMessage(responseMessage, V2GMessages.NONE, cableCheckRes.getResponseCode());
			} else {
				setMandatoryFieldsForFailedRes(cableCheckRes, cableCheckRes.getResponseCode());
			}
		}
		
		return getSendMessage(cableCheckRes, V2GMessages.NONE, cableCheckRes.getResponseCode());
	}
	

	public boolean isEvseProcessingFinished() {
		return evseProcessingFinished;
	}

	public void setEvseProcessingFinished(boolean evseProcessingFinished) {
		this.evseProcessingFinished = evseProcessingFinished;
	}

	@Override
	public BodyBaseType getResponseMessage() {
		return cableCheckRes;
	}

	// *** EVerest code start ***
	private boolean getIsolationCheckRequested() {
		return isolationCheckRequested;
	}

	private void setIsolationCheckRequested(boolean isolationCheckRequested) {
		this.isolationCheckRequested = isolationCheckRequested;
	}
	// *** EVerest code end ***
}
