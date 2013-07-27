package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class SmoothVariables 
{
	protected short maxTotal, scale, scale1, scale2, A, B, C, denom16,w11Prim,max1,error;
	protected short bitsW00, bitsW10, bitsW11;
	protected int w11W00, w10W10, w00W00, w00, w10, w11, w00Prim, w10Prim, w11DivW00,i;
	protected int B32, denom, num, errors, crit, endiff;
	protected int tempIndex1,tempIndex2;
}