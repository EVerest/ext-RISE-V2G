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
package com.v2gclarity.risev2g.evcc.misc;

import com.v2gclarity.risev2g.evcc.evController.DummyEVController;
import com.v2gclarity.risev2g.evcc.evController.EverestEVController;
import com.v2gclarity.risev2g.evcc.evController.IEVController;
import com.v2gclarity.risev2g.evcc.session.V2GCommunicationSessionEVCC;
import com.v2gclarity.risev2g.shared.misc.V2GImplementationFactory;

/**
 * Implementation factory for the EVCC controller
 *
 */
public class EVCCImplementationFactory extends V2GImplementationFactory {

	/**
	 * Creates the controller for the EVCC application
	 * @param commSessionContext the session the backend will be connected to 
	 * @return
	 */
	public static IEVController createEVController(V2GCommunicationSessionEVCC commSessionContext) {
		IEVController instance = buildFromProperties("implementation.evcc.controller", IEVController.class);
		if (instance == null) {
			instance = new DummyEVController();
		}
		instance.setCommSessionContext(commSessionContext);
		return instance;
	}
}
