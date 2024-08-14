package biouml.plugins.virtualcell.core;

/**
 * Agent that provides some biologial process (transcription, translation, etc.) 
 */
public abstract class Process 
{
	protected Pool[] input;
	protected Pool[] otput;
	
	/**
	 * 
	 * @param in
	 * @param out
	 */
	public abstract int doStep(Pool[] in, Pool[] out);
}
