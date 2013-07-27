/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.javax.media.mscontrol;

public class DefaultEventGeneratorFactory extends EventGeneratorFactory {

	public DefaultEventGeneratorFactory(String pkgName, String eventName, boolean isOnEndpoint) {
		super(pkgName, eventName, isOnEndpoint);		
	}
	
	@Override
	public String toString() {
		return super.toString() + "  DefaultEventGeneratorFactory";
	}

}
