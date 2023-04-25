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
package com.v2gclarity.risev2g.evcc.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.v2gclarity.risev2g.evcc.session.V2GCommunicationSessionHandlerEVCC;
import com.v2gclarity.risev2g.shared.enumerations.GlobalValues;
import com.v2gclarity.risev2g.shared.utils.MiscUtils;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.EnergyTransferModeType;
import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.PaymentOptionType;
import com.v2gclarity.risev2g.shared.enumerations.ObjectHolder;
import com.v2gclarity.risev2g.shared.misc.Mqtt;

public class StartEVCC {

	public static void main(String[] args) {
		final Logger logger = LogManager.getLogger(StartEVCC.class.getSimpleName());

		// *** EVerest code start ***
		logger.info("STARTED WITH ARGS:");
		for(String arg : args) {
			logger.info(arg);
		}

		ObjectHolder.ev_mqtt = new Mqtt(args[0], args[1], args[2], "rise-v2g_java_external_mqtt_evcc");
		ObjectHolder.ev_mqtt.publish_ready(true);

		MiscUtils.setNetworkInterface((String) args[5]);
		MiscUtils.setCiphersuites(args[8].split(":"));
		MiscUtils.setKeystorePassword(args[9]);

		PaymentOptionType payment = PaymentOptionType.fromValue((String) args[3]);
		EnergyTransferModeType energymode = EnergyTransferModeType.fromValue((String) args[4]);

		MiscUtils.setCertsPath((String) args[6]);
		String tls_active = (String) args[7];

		MiscUtils.loadProperties(GlobalValues.EVCC_CONFIG_PROPERTIES_PATH.toString());
		// *** EVerest code end ***

		new V2GCommunicationSessionHandlerEVCC(payment, energymode, tls_active);

	}

}
