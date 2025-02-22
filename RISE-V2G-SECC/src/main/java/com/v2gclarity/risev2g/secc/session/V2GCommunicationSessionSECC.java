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
package com.v2gclarity.risev2g.secc.session;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

import com.v2gclarity.risev2g.secc.backend.IBackendInterface;
import com.v2gclarity.risev2g.secc.evseController.EverestEVSEController;
import com.v2gclarity.risev2g.secc.evseController.IACEVSEController;
import com.v2gclarity.risev2g.secc.evseController.IDCEVSEController;
import com.v2gclarity.risev2g.secc.evseController.IEVSEController;
import com.v2gclarity.risev2g.secc.misc.SECCImplementationFactory;
import com.v2gclarity.risev2g.secc.states.ForkState;
import com.v2gclarity.risev2g.secc.states.WaitForAuthorizationReq;
import com.v2gclarity.risev2g.secc.states.WaitForCableCheckReq;
import com.v2gclarity.risev2g.secc.states.WaitForCertificateInstallationReq;
import com.v2gclarity.risev2g.secc.states.WaitForCertificateUpdateReq;
import com.v2gclarity.risev2g.secc.states.WaitForChargeParameterDiscoveryReq;
import com.v2gclarity.risev2g.secc.states.WaitForChargingStatusReq;
import com.v2gclarity.risev2g.secc.states.WaitForCurrentDemandReq;
import com.v2gclarity.risev2g.secc.states.WaitForMeteringReceiptReq;
import com.v2gclarity.risev2g.secc.states.WaitForPaymentDetailsReq;
import com.v2gclarity.risev2g.secc.states.WaitForPaymentServiceSelectionReq;
import com.v2gclarity.risev2g.secc.states.WaitForPowerDeliveryReq;
import com.v2gclarity.risev2g.secc.states.WaitForPreChargeReq;
import com.v2gclarity.risev2g.secc.states.WaitForServiceDetailReq;
import com.v2gclarity.risev2g.secc.states.WaitForServiceDiscoveryReq;
import com.v2gclarity.risev2g.secc.states.WaitForSessionSetupReq;
import com.v2gclarity.risev2g.secc.states.WaitForSessionStopReq;
import com.v2gclarity.risev2g.secc.states.WaitForSupportedAppProtocolReq;
import com.v2gclarity.risev2g.secc.states.WaitForWeldingDetectionReq;
import com.v2gclarity.risev2g.secc.transportLayer.ConnectionHandler;
import com.v2gclarity.risev2g.shared.enumerations.GlobalValues;
import com.v2gclarity.risev2g.shared.enumerations.V2GMessages;
import com.v2gclarity.risev2g.shared.messageHandling.ChangeProcessingState;
import com.v2gclarity.risev2g.shared.messageHandling.PauseSession;
import com.v2gclarity.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import com.v2gclarity.risev2g.shared.messageHandling.SendMessage;
import com.v2gclarity.risev2g.shared.messageHandling.TerminateSession;
import com.v2gclarity.risev2g.shared.misc.V2GCommunicationSession;
import com.v2gclarity.risev2g.shared.misc.V2GTPMessage;
import com.v2gclarity.risev2g.shared.utils.ByteUtils;
import com.v2gclarity.risev2g.shared.utils.MiscUtils;
import com.v2gclarity.risev2g.shared.v2gMessages.appProtocol.SupportedAppProtocolReq;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ACEVSEStatusType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.CertificateChainType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ChargingSessionType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EVSENotificationType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EnergyTransferModeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.MessageHeaderType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.MeterInfoType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PMaxScheduleType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PaymentOptionListType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PaymentOptionType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ResponseCodeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.SAScheduleListType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.ServiceType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;

// *** EVerest code start ***
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import org.json.simple.JSONObject;
import javax.xml.bind.JAXBElement;
import java.io.StringWriter;
import com.v2gclarity.risev2g.shared.v2gMessages.appProtocol.SupportedAppProtocolRes;
import javax.xml.bind.JAXBException;
import com.v2gclarity.risev2g.shared.exiCodec.ExiCodec;
import com.v2gclarity.risev2g.shared.exiCodec.EXIficientCodec;
import com.v2gclarity.risev2g.shared.utils.SleepUtils;
// *** EVerest code end ***

public class V2GCommunicationSessionSECC extends V2GCommunicationSession implements Observer {
	
	private short schemaID;
	private ACEVSEStatusType acEVSEStatus;
	private ChargingSessionType chargingSession;
	private PMaxScheduleType pMaxSchedule;
	private short chosenSAScheduleTuple;
	private IACEVSEController acEvseController;
	private IDCEVSEController dcEvseController;
	private IBackendInterface backendInterface;
	private boolean oldSessionJoined;
	private byte[] incomingV2GTPMessage;
	private ConnectionHandler connectionHandler;
	private ArrayList<ServiceType> offeredServices;
	private byte[] genChallenge;
	private SAScheduleListType saSchedules;
	private EnergyTransferModeType requestedEnergyTransferMode;
	private PaymentOptionType selectedPaymentOption;
	private CertificateChainType contractSignatureCertChain;
	private MeterInfoType sentMeterInfo;
	private boolean chargeProgressStarted; // for checking [V2G2-812]
	
	public V2GCommunicationSessionSECC(ConnectionHandler connectionHandler) {
		setConnectionHandler(connectionHandler);
		
		// Tell the respective ConnectionHandler to notify if a new V2GTPMessage has arrived (see update()-method)
		connectionHandler.addObserver(this);
		
		getStates().put(V2GMessages.FORK, new ForkState(this));
		getStates().put(V2GMessages.SUPPORTED_APP_PROTOCOL_REQ, new WaitForSupportedAppProtocolReq(this));
		getStates().put(V2GMessages.SESSION_SETUP_REQ, new WaitForSessionSetupReq(this));
		getStates().put(V2GMessages.SERVICE_DISCOVERY_REQ, new WaitForServiceDiscoveryReq(this));
		getStates().put(V2GMessages.SERVICE_DETAIL_REQ, new WaitForServiceDetailReq(this));
		getStates().put(V2GMessages.PAYMENT_SERVICE_SELECTION_REQ, new WaitForPaymentServiceSelectionReq(this));
		getStates().put(V2GMessages.CERTIFICATE_INSTALLATION_REQ, new WaitForCertificateInstallationReq(this));
		getStates().put(V2GMessages.CERTIFICATE_UPDATE_REQ, new WaitForCertificateUpdateReq(this));
		getStates().put(V2GMessages.PAYMENT_DETAILS_REQ, new WaitForPaymentDetailsReq(this));
		getStates().put(V2GMessages.AUTHORIZATION_REQ, new WaitForAuthorizationReq(this));
		getStates().put(V2GMessages.CHARGE_PARAMETER_DISCOVERY_REQ, new WaitForChargeParameterDiscoveryReq(this));
		getStates().put(V2GMessages.CABLE_CHECK_REQ, new WaitForCableCheckReq(this));
		getStates().put(V2GMessages.PRE_CHARGE_REQ, new WaitForPreChargeReq(this));
		getStates().put(V2GMessages.POWER_DELIVERY_REQ, new WaitForPowerDeliveryReq(this));
		getStates().put(V2GMessages.CHARGING_STATUS_REQ, new WaitForChargingStatusReq(this));
		getStates().put(V2GMessages.CURRENT_DEMAND_REQ, new WaitForCurrentDemandReq(this));
		getStates().put(V2GMessages.METERING_RECEIPT_REQ, new WaitForMeteringReceiptReq(this));
		getStates().put(V2GMessages.WELDING_DETECTION_REQ, new WaitForWeldingDetectionReq(this));
		getStates().put(V2GMessages.SESSION_STOP_REQ, new WaitForSessionStopReq(this));
		
		setStartState(getStates().get(V2GMessages.SUPPORTED_APP_PROTOCOL_REQ));
		setCurrentState(getStartState());
		
		// Configures which backend interface implementation to use for retrieving SASchedules
		setBackendInterface(SECCImplementationFactory.createBackendInterface(this));

		// ACEVSE notification
		setACEVSEStatus(new ACEVSEStatusType());
		getACEVSEStatus().setEVSENotification(EVSENotificationType.NONE);
		getACEVSEStatus().setNotificationMaxDelay(0);
		getACEVSEStatus().setRCD(false);
		
		// Will be set only if a session is to be stopped or paused
		setChargingSession(null);
		
		setOfferedServices(new ArrayList<ServiceType>());
		
		getLogger().debug("\n*******************************************" +
						  "\n* New V2G communication session initialized" +
						  "\n*******************************************");
	}
	
	
	@Override
	public void update(Observable obs, Object obj) {
		if (obs instanceof ConnectionHandler && obj instanceof byte[]) {
			processIncomingMessage((byte[]) obj);
		} else if (obs instanceof ConnectionHandler && obj == null) {
			terminateSession("ConnectionHandler has notified an error", false);
		}
	}
	
	
	public void processIncomingMessage(Object incomingMessage) {
		setV2gTpMessage(new V2GTPMessage((byte[]) incomingMessage)); 
		
		if (getMessageHandler().isV2GTPMessageValid(getV2gTpMessage()) &&
			Arrays.equals(getV2gTpMessage().getPayloadType(), GlobalValues.V2GTP_PAYLOAD_TYPE_EXI_ENCODED_V2G_MESSAGE.getByteArrayValue())) {
			/*
			 * Decide which schema to use for decoding the EXI encoded message. 
			 * Only the SupportedAppProtocolReq/Res message uses a different schema
			 */
			if (getCurrentState().equals(getStates().get(V2GMessages.SUPPORTED_APP_PROTOCOL_REQ))) {
				incomingMessage = (SupportedAppProtocolReq) getMessageHandler().exiToSuppAppProtocolMsg(getV2gTpMessage().getPayload());
			} else {
				incomingMessage = (V2GMessage) getMessageHandler().exiToV2gMsg(getV2gTpMessage().getPayload());
			}

			// *** EVerest code start ***
			if (((EverestEVSEController) getEvseController()).getDebugMode().equals("Full")) {
				DebugAllV2GMessages(incomingMessage, getV2gTpMessage().getPayload());
			}

			if (getCurrentState().equals(getStates().get(V2GMessages.CURRENT_DEMAND_REQ)) || getCurrentState().equals(getStates().get(V2GMessages.CURRENT_DEMAND_RES))) {
				SleepUtils.safeSleep(TimeUnit.MILLISECONDS, 200);
			} else {
				SleepUtils.safeSleep(TimeUnit.MILLISECONDS, 500);
			}
			
			// *** EVerest code end ***
			
			processReaction(getCurrentState().processIncomingMessage(incomingMessage));
		} else {
			getLogger().warn("Received incoming message is not a valid V2GTPMessage", false);
		}
	}
	
	
	private void processReaction(ReactionToIncomingMessage reactionToIncomingMessage) {
		// Check the outcome of the processIncomingMessage() of the respective state
		if (reactionToIncomingMessage instanceof SendMessage) {
			send((SendMessage) reactionToIncomingMessage);
			if (getChargingSession() != null && getChargingSession() == ChargingSessionType.TERMINATE) 
				terminateSession("EVCC indicated request to stop the session or a FAILED response code was sent", true);
			
			if (getChargingSession() != null && getChargingSession() == ChargingSessionType.PAUSE) {
				pauseSession(new PauseSession());
			}
		} else if (reactionToIncomingMessage instanceof ChangeProcessingState) {
			setCurrentState(((ChangeProcessingState) reactionToIncomingMessage).getNewState());
			processReaction(
					getCurrentState().processIncomingMessage(
							((ChangeProcessingState) reactionToIncomingMessage).getPayload()));
		} else if (reactionToIncomingMessage instanceof TerminateSession) {
			/*
			 * TODO is this really needed? if sth. goes wrong, a negative response code will be sent by
			 * the respective state anyway, the reaction to this negative response code should only
			 * instantiate a TerminateSession object.
			 */
			terminateSession(((TerminateSession) reactionToIncomingMessage));
		} else {
			terminateSession("Reaction to incoming message is undefined", false);
		}
	}
	
	
	/**
	 * Returns a response code according to 8.4.2
	 * @param header The header encapsulated in the EVCC request message
	 * @return The corresponding response code
	 */
	public ResponseCodeType checkSessionID(MessageHeaderType header) {
		if (getCurrentState().equals(getStates().get(V2GMessages.SESSION_SETUP_REQ)) && 
			ByteUtils.toHexString(header.getSessionID()).equals("00")) {
			// EV wants to start a totally new charging session
			setSessionID(generateSessionIDRandomly());
			setOldSessionJoined(false);
			return ResponseCodeType.OK_NEW_SESSION_ESTABLISHED;
		} else if (getCurrentState().equals(getStates().get(V2GMessages.SESSION_SETUP_REQ)) && 
				   Arrays.equals(header.getSessionID(), getSessionID())) {
			// A charging pause has taken place and the EV wants to resume the old charging session
			setOldSessionJoined(true);
			return ResponseCodeType.OK_OLD_SESSION_JOINED;
		} else if (getCurrentState().equals(getStates().get(V2GMessages.SESSION_SETUP_REQ)) && 
				  !ByteUtils.toHexString(header.getSessionID()).equals("00") &&
				  !Arrays.equals(header.getSessionID(), getSessionID())) {
			// Avoid a "FAILED_..." response code by generating a new SessionID in the response
			getLogger().warn("Presented session ID '" + ByteUtils.toHexString(header.getSessionID()) + "' does not match stored session ID '" +
							 ByteUtils.toHexString(getSessionID()) + "'. Will reassign a new session ID");
			setSessionID(generateSessionIDRandomly());
			setOldSessionJoined(false);
			return ResponseCodeType.OK_NEW_SESSION_ESTABLISHED;
		} else if (Arrays.equals(header.getSessionID(), getSessionID())) {
			// This should be the routine during a running charging session after a session setup
			setOldSessionJoined(false);
			return ResponseCodeType.OK;
		} else {
			// EV sends a SessionID DURING the already running charging session which does not match
			setOldSessionJoined(false);
			return ResponseCodeType.FAILED_UNKNOWN_SESSION;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public PaymentOptionListType getPaymentOptions() {
		ArrayList<PaymentOptionType> paymentOptions = new ArrayList<PaymentOptionType>();
		
		if (isOldSessionJoined()) {
			paymentOptions.add(selectedPaymentOption);
		} else { 
			paymentOptions.addAll( ((EverestEVSEController)getEvseController()).getPaymentOptions());
		}
		
		// Contract-based payment may only be offered if TLS is used
		if (!isTlsConnection()) 
			paymentOptions.remove(PaymentOptionType.CONTRACT);
				
		PaymentOptionListType paymentOptionList = new PaymentOptionListType();
		paymentOptionList.getPaymentOption().addAll(paymentOptions);
		
		return paymentOptionList;
	}
	
	
	public void send(SendMessage sendMessage) {
		// Only EXI encoded messages will be sent here. Decide whether V2GMessage or SupportedAppProtocolRes
		byte[] payload = null;

		if (sendMessage.getPayload() instanceof V2GMessage) {
			payload = (byte[]) getMessageHandler().v2gMsgToExi(sendMessage.getPayload());
		} else {
			payload = (byte[]) getMessageHandler().suppAppProtocolMsgToExi(sendMessage.getPayload());
		}

		// *** EVerest code start ***
		if (((EverestEVSEController) getEvseController()).getDebugMode().equals("Full")) {
			DebugAllV2GMessages(sendMessage.getPayload(), payload);
		}
		// *** EVerest code end ***
			
		setV2gTpMessage(
				new V2GTPMessage(GlobalValues.V2GTP_VERSION_1_IS.getByteValue(), 
				GlobalValues.V2GTP_PAYLOAD_TYPE_EXI_ENCODED_V2G_MESSAGE.getByteArrayValue(),
				payload)
			);
		
		getConnectionHandler().send(getV2gTpMessage());
		
		if (sendMessage.getNextState() != null) {
			setCurrentState(sendMessage.getNextState());
		} else {
			getLogger().info("State machine has come to an end, no new state provided");
		}
	}
	
	
	public short getSchemaID() {
		return schemaID;
	}
	
	public void setSchemaID(short schemaID) {
		this.schemaID = schemaID;
	}
	
	public ACEVSEStatusType getACEVSEStatus() {
		return acEVSEStatus;
	}
	
	public void setACEVSEStatus(ACEVSEStatusType acEVSEStatus) {
		this.acEVSEStatus = acEVSEStatus;
	}

	public PMaxScheduleType getPMaxSchedule() {
		return pMaxSchedule;
	}

	public void setPMaxSchedule(PMaxScheduleType newPMaxSchedule) {
		this.pMaxSchedule = newPMaxSchedule;
	}

	public short getChosenSAScheduleTuple() {
		return chosenSAScheduleTuple;
	}

	public void setChosenSAScheduleTuple(short saScheduleTupleID) {
		this.chosenSAScheduleTuple = saScheduleTupleID;
	}

	public IBackendInterface getBackendInterface() {
		return backendInterface;
	}


	public void setBackendInterface(IBackendInterface backendInterface) {
		this.backendInterface = backendInterface;
	}


	public boolean isOldSessionJoined() {
		return oldSessionJoined;
	}

	public void setOldSessionJoined(boolean oldSessionJoined) {
		this.oldSessionJoined = oldSessionJoined;
	}

	public byte[] getIncomingV2GTPMessage() {
		return incomingV2GTPMessage;
	}

	public void setIncomingV2GTPMessage(byte[] incomingV2GTPMessage) {
		this.incomingV2GTPMessage = incomingV2GTPMessage;
	}

	public ConnectionHandler getConnectionHandler() {
		return connectionHandler;
	}

	public void setConnectionHandler(ConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}
	
	public ArrayList<ServiceType> getOfferedServices() {
		return offeredServices;
	}


	public void setOfferedServices(ArrayList<ServiceType> offeredServices) {
		this.offeredServices = offeredServices;
	}


	public byte[] getGenChallenge() {
		return genChallenge;
	}


	public void setGenChallenge(byte[] genChallenge) {
		this.genChallenge = genChallenge;
	}


	public SAScheduleListType getSaSchedules() {
		return saSchedules;
	}


	public void setSaSchedules(SAScheduleListType saSchedules) {
		this.saSchedules = saSchedules;
	}


	public EnergyTransferModeType getRequestedEnergyTransferMode() {
		return requestedEnergyTransferMode;
	}


	public void setRequestedEnergyTransferMode(
			EnergyTransferModeType requestedEnergyTransferMode) {
		this.requestedEnergyTransferMode = requestedEnergyTransferMode;
	}


	public CertificateChainType getContractSignatureCertChain() {
		return contractSignatureCertChain;
	}


	public void setContractSignatureCertChain(CertificateChainType contractSignatureCertChain) {
		this.contractSignatureCertChain = contractSignatureCertChain;
	}


	public MeterInfoType getSentMeterInfo() {
		return sentMeterInfo;
	}


	public void setSentMeterInfo(MeterInfoType sentMeterInfo) {
		this.sentMeterInfo = sentMeterInfo;
	}


	public IACEVSEController getACEvseController() {
		return acEvseController;
	}


	public void setACEvseController(IACEVSEController acEvseController) {
		this.acEvseController = acEvseController;
	}


	public IDCEVSEController getDCEvseController() {
		return dcEvseController;
	}


	public void setDCEvseController(IDCEVSEController dcEvseController) {
		this.dcEvseController = dcEvseController;
	}


	public IEVSEController getEvseController() {
		if (getRequestedEnergyTransferMode() != null) {
			if (getRequestedEnergyTransferMode().toString().startsWith("AC")) 
				return acEvseController;
			else if (getRequestedEnergyTransferMode().toString().startsWith("DC")) 
				return dcEvseController;
			else {
				getLogger().error("RequestedEnergyTransferMode '" + getRequestedEnergyTransferMode().toString() + 
								   "is neither of type AC nor DC");
				return null;
			}
		} else return acEvseController; // just AC controller as default
	}


	public PaymentOptionType getSelectedPaymentOption() {
		return selectedPaymentOption;
	}


	public void setSelectedPaymentOption(PaymentOptionType selectedPaymentOption) {
		this.selectedPaymentOption = selectedPaymentOption;
	}


	public boolean isChargeProgressStarted() {
		return chargeProgressStarted;
	}


	public void setChargeProgressStarted(boolean chargeProgressStarted) {
		this.chargeProgressStarted = chargeProgressStarted;
	}


	public ChargingSessionType getChargingSession() {
		return chargingSession;
	}


	public void setChargingSession(ChargingSessionType chargingSession) {
		this.chargingSession = chargingSession;
	}

	// *** EVerest code start ***
	public void DebugAllV2GMessages(Object message, byte[] exiByte) {
		StringWriter sw = new StringWriter();
		String className = "";

		JSONObject v2g_messages = new JSONObject();
		
		if (message instanceof V2GMessage) {
			className = ((V2GMessage) message).getBody().getBodyElement().getName().getLocalPart();
		} else if (message instanceof JAXBElement) {
			className = ((JAXBElement) message).getName().getLocalPart();
		} else if (message instanceof SupportedAppProtocolReq) {
			className = "SupportedAppProtocolReq"; 
		} else if (message instanceof SupportedAppProtocolRes) {
			className = "SupportedAppProtocolRes";
		} else {
			className = "marshalled JAXBElement";
		}
		
		try {
			ExiCodec exiCodec = EXIficientCodec.getInstance();
			exiCodec.getMarshaller().marshal(message, sw);
			String msg = new String(sw.toString());

			v2g_messages.put("V2G_Message_ID", className);
			v2g_messages.put("V2G_Message_XML", msg);
			v2g_messages.put("V2G_Message_EXI_Hex", ByteUtils.toHexString(exiByte));
			v2g_messages.put("V2G_Message_EXI_Base64", Base64.getEncoder().encodeToString(exiByte));

			ObjectHolder.mqtt.publish_var("charger", "V2G_Messages", v2g_messages);

		} catch (JAXBException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to debug XML representation of " + className, e);
		}
	}
	// *** EVerest code start ***
	
}
