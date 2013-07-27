package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class DecodeResidualVariables 
{
	protected short[] reverseDecresidual;
	protected short[] memVec;

	protected int i,subCount,subFrame;
	protected short startPos,nBack,nFor,memlGotten;
	protected short diff;
	
	protected StateConstructVariables stateConstructVariables=new StateConstructVariables();
	protected CbConstructVariables cbConstructVariables=new CbConstructVariables();
}
