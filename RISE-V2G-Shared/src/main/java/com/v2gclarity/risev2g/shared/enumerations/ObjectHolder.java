// *** EVerest file ***
package com.v2gclarity.risev2g.shared.enumerations;

import com.v2gclarity.risev2g.shared.v2gMessages.msgDef.*;

import java.util.HashMap;
import com.v2gclarity.risev2g.shared.misc.Mqtt;

public class ObjectHolder {
    public static Mqtt mqtt;
    
    /**
	 * Takes a physical quantitiy (composited with a value and a unit) and returns it as PhysicalDataType
	 * @param value The value of the physical quantity as double
	 * @param unit The unit of the physical quantity as UnitSymbolType
	 * @return The physical quantity as PhysicalDataType
	 */
	public static PhysicalValueType doubleToPhysicalValue(double value, UnitSymbolType unit) {
		short exponent = 0;
		for(exponent = 0; exponent < 3; exponent++) {
			if(value * (10 ^ exponent) > Short.MAX_VALUE) {
				exponent -= 1;
				break;
			}
		}
		PhysicalValueType retVal = new PhysicalValueType();
		retVal.setMultiplier((byte) -exponent);
		retVal.setUnit(unit);
		retVal.setValue((short) (value * (10 ^ exponent)));
		return retVal;
	}
	
	public static double physicalValueToDouble(PhysicalValueType value) {
        return ((double) value.getValue()) * (10 ^ value.getMultiplier());
    }
}
