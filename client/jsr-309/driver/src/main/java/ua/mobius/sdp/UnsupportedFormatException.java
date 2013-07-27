/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.sdp;

import javax.media.mscontrol.MediaException;



/**
 * Standard JMF class -- see <a
 * href="http://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/format/UnsupportedFormatException.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 * 
 * @author Ken Larson
 * 
 */
public class UnsupportedFormatException extends MediaException {
	private final Format unsupportedFormat;

	public UnsupportedFormatException(Format unsupportedFormat) {
		super("Unsupported Format "+unsupportedFormat);
		this.unsupportedFormat = unsupportedFormat;
	}

	public UnsupportedFormatException(String message, Format unsupportedFormat) {
		super(message);
		this.unsupportedFormat = unsupportedFormat;

	}

	public Format getFailedFormat() {
		return unsupportedFormat;
	}
}
