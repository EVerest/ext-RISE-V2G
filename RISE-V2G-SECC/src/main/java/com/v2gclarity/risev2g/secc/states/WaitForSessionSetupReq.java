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

import com.v2gclarity.risev2g.secc.session.V2GCommunicationSessionSECC;
import com.v2gclarity.risev2g.shared.enumerations.V2GMessages;
import com.v2gclarity.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.BodyBaseType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ResponseCodeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.SessionSetupReqType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.SessionSetupResType;

// *** EVerest code start ***
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import com.v2gclarity.risev2g.secc.evseController.EverestEVSEController;
// *** EVerest code end ***

public class WaitForSessionSetupReq extends ServerState {
	
	private SessionSetupResType sessionSetupRes; 

	// *** EVerest code start ***
	private static final char[] HEX_VALUES = "0123456789ABCDEF".toCharArray();
	private static final byte COLON_COUNT = 5;
	// *** EVerest code end ***
	
	public WaitForSessionSetupReq(V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
		sessionSetupRes = new SessionSetupResType();
	}
	
	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, SessionSetupReqType.class, sessionSetupRes)) {

			// *** EVerest code start ***
            V2GMessage v2gMessageReq = (V2GMessage) message;
			SessionSetupReqType sessionSetupReq = 
				(SessionSetupReqType) v2gMessageReq.getBody().getBodyElement().getValue();

			String evccid = new String(byte2Hex(sessionSetupReq.getEVCCID()));
            ObjectHolder.mqtt.publish_var("charger", "EVCCIDD", evccid);
 
			((EverestEVSEController) getCommSessionContext().getEvseController()).resetStopCharging();
			((EverestEVSEController) getCommSessionContext().getEvseController()).resetauthorizationOkay();
			((EverestEVSEController) getCommSessionContext().getEvseController()).resetContactorStatus();
			((EverestEVSEController) getCommSessionContext().getEvseController()).resetRCDError();
            // *** EVerest code end ***

			sessionSetupRes.setEVSEID(getCommSessionContext().getEvseController().getEvseID());
			
			// Unix time stamp is needed (seconds instead of milliseconds)
			sessionSetupRes.setEVSETimeStamp(System.currentTimeMillis() / 1000L);
		} else {
			if (sessionSetupRes.getResponseCode().equals(ResponseCodeType.FAILED_SEQUENCE_ERROR)) {
				BodyBaseType responseMessage = getSequenceErrorResMessage(new SessionSetupResType(), message);
				
				return getSendMessage(responseMessage, V2GMessages.NONE, sessionSetupRes.getResponseCode());
			} else {
				setMandatoryFieldsForFailedRes(sessionSetupRes, sessionSetupRes.getResponseCode());
			}
		} 
			
		return getSendMessage(sessionSetupRes, 
				  			  (sessionSetupRes.getResponseCode().toString().startsWith("OK") ? 
				  			  V2GMessages.SERVICE_DISCOVERY_REQ : V2GMessages.NONE),
				  			  sessionSetupRes.getResponseCode()
				 			 );
	}


	@Override
	public BodyBaseType getResponseMessage() {
		return sessionSetupRes;
	}

	// *** EVerest code start ***
	private char[] byte2Hex(byte[] array) {

		char[] hexchar = new char[array.length * 2 + COLON_COUNT];

		int v = array[0] & 0xFF;
		hexchar[0] = HEX_VALUES[v >>> 4];
		hexchar[1] = HEX_VALUES[v & 0x0F];

		for (int j = 1; j < array.length; j++) {
			v = array[j] & 0xFF;
			hexchar[j * 3 - 1] = ':';
			hexchar[j * 3 + 0] = HEX_VALUES[v >>> 4];
			hexchar[j * 3 + 1] = HEX_VALUES[v & 0x0F];
		}

		return hexchar;
	}
	// *** EVerest code end ***

}
