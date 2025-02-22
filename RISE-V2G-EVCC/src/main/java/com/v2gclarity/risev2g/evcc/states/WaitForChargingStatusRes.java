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
package com.v2gclarity.risev2g.evcc.states;

import com.v2gclarity.risev2g.evcc.evController.IACEVController;
import com.v2gclarity.risev2g.evcc.session.V2GCommunicationSessionEVCC;
import com.v2gclarity.risev2g.shared.enumerations.GlobalValues;
import com.v2gclarity.risev2g.shared.enumerations.V2GMessages;
import com.v2gclarity.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import com.v2gclarity.risev2g.shared.messageHandling.TerminateSession;
import com.v2gclarity.risev2g.shared.utils.SecurityUtils;
import com.v2gclarity.risev2g.shared.utils.MiscUtils;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ChargeProgressType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ChargingSessionType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ChargingStatusReqType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ChargingStatusResType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.MeteringReceiptReqType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;


// *** EVerest code start ***
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
// *** EVerest code end ***

public class WaitForChargingStatusRes extends ClientState {

	public WaitForChargingStatusRes(V2GCommunicationSessionEVCC commSessionContext) {
		super(commSessionContext);
	}

	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, ChargingStatusResType.class)) {
			V2GMessage v2gMessageRes = (V2GMessage) message;
			ChargingStatusResType chargingStatusRes = 
					(ChargingStatusResType) v2gMessageRes.getBody().getBodyElement().getValue();
		
			/*
			 * ReceiptRequired has higher priority than a possible EVSENotification=Renegotiate
			 * 
			 * Check if communication is secured with TLS before reacting upon a possible request from the SECC to send
			 * a MeteringReceiptRequest. If no TLS is used, a MeteringReceiptRequest may not be sent because
			 * a signature cannot be applied without private key of the contract certificate.
			 */
			if (chargingStatusRes.isReceiptRequired() != null && chargingStatusRes.isReceiptRequired() && getCommSessionContext().isTlsConnection()) {
				MeteringReceiptReqType meteringReceiptReq = new MeteringReceiptReqType();
				/*
				 * Experience from the test symposium in San Diego (April 2016):
				 * The Id element of the signature is not restricted in size by the standard itself. But on embedded 
				 * systems, the memory is very limited which is why we should not use long IDs for the signature reference
				 * element. A good size would be 3 characters max (like the example in the ISO 15118-2 annex J)
				 */
				meteringReceiptReq.setId("id1");
				meteringReceiptReq.setMeterInfo(chargingStatusRes.getMeterInfo());
				meteringReceiptReq.setSAScheduleTupleID(chargingStatusRes.getSAScheduleTupleID());
				meteringReceiptReq.setSessionID(getCommSessionContext().getSessionID());
				
				// Set xml reference element
				getXMLSignatureRefElements().put(
						meteringReceiptReq.getId(), 
						SecurityUtils.generateDigest(
								meteringReceiptReq.getId(),
								getMessageHandler().getJaxbElement(meteringReceiptReq)));
				
				// Set signing private key
				setSignaturePrivateKey(SecurityUtils.getPrivateKey(
						SecurityUtils.getKeyStore(
								MiscUtils.getCertsPath() + GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(),
								GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()), 
						GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString())
				);
				
				return getSendMessage(meteringReceiptReq, V2GMessages.METERING_RECEIPT_RES);
			}

			// *** EVerest code start ***
			if (chargingStatusRes.getEVSEMaxCurrent() != null) {
				double AC_EVSEMaxCurrent = chargingStatusRes.getEVSEMaxCurrent().getValue() * Math.pow(10, chargingStatusRes.getEVSEMaxCurrent().getMultiplier());
				ObjectHolder.ev_mqtt.publish_var("ev", "AC_EVSEMaxCurrent", AC_EVSEMaxCurrent);
			}
			// *** EVerest code end ***
				
			// Check for EVSEMaxCurrent and tell the EV
			if (chargingStatusRes.getEVSEMaxCurrent() != null)
				((IACEVController) getCommSessionContext().getEvController())
					.adjustMaxCurrent(chargingStatusRes.getEVSEMaxCurrent());
			
			switch (chargingStatusRes.getACEVSEStatus().getEVSENotification()) {
				case STOP_CHARGING:
					getCommSessionContext().setChargingSession(ChargingSessionType.TERMINATE);
					ObjectHolder.ev_mqtt.publish_var("ev", "AC_StopFromCharger", null);;
					
					return getSendMessage(getPowerDeliveryReq(ChargeProgressType.STOP), 
										  V2GMessages.POWER_DELIVERY_RES,
										  " (ChargeProgress = STOP_CHARGING)");
				case RE_NEGOTIATION:
					getCommSessionContext().setRenegotiationRequested(true);
					return getSendMessage(getPowerDeliveryReq(ChargeProgressType.RENEGOTIATE), 
										  V2GMessages.POWER_DELIVERY_RES,
							  			  " (ChargeProgress = RE_NEGOTIATION)");
				default:
					// TODO regard [V2G2-305] (new SalesTariff if EAmount not yet met and tariff finished)
					
					if (getCommSessionContext().getEvController().isChargingLoopActive()) {
						// Check whether or not the EV controller triggered a renegotiation
						if (getCommSessionContext().isRenegotiationRequested()) {
							return getSendMessage(getPowerDeliveryReq(ChargeProgressType.RENEGOTIATE), 
									  V2GMessages.POWER_DELIVERY_RES,
						  			  " (ChargeProgress = RE_NEGOTIATION)");
						} else {
							ChargingStatusReqType chargingStatusReq = new ChargingStatusReqType();
							return getSendMessage(chargingStatusReq, V2GMessages.CHARGING_STATUS_RES);
						}
					} else {
						/* Check if the EV controller triggered a pause of a charging session. 
						 * If not, indicate a termination of the charging session. This will be
						 * evaluated in the state WaitForPowerDeliveryRes
						 */
						if (getCommSessionContext().getChargingSession() == null)
							getCommSessionContext().setChargingSession(ChargingSessionType.TERMINATE);
						
						return getSendMessage(getPowerDeliveryReq(ChargeProgressType.STOP), 
											  V2GMessages.POWER_DELIVERY_RES,
											  " (ChargeProgress = STOP_CHARGING)");
					}
			}
		} else {
			return new TerminateSession("Incoming message raised an error");
		}
	}
}
