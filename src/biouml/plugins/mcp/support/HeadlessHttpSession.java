package biouml.plugins.mcp.support;

/**
 * A synthetic carrier object that mimics a {@code javax.servlet.http.HttpSession} with a non-null
 * {@code getId()} method. Used to bootstrap a headless {@code WebSession} for async (job-based)
 * providers like {@code CopyFolderProvider}, {@code SimulationProvider}, and {@code WebScriptsProvider}.
 *
 * <p>{@code WebSession.getSession(Object)} reflectively looks up a public no-arg {@code getId()}
 * method on the carrier and invokes it to get the session ID. This class provides that method.</p>
 */
public class HeadlessHttpSession
{
	private final String id;

	/**
	 * Create a carrier for the given session ID.
	 * @param id the non-null session ID
	 */
	public HeadlessHttpSession( String id )
	{
		if ( id == null || id.isEmpty() )
			throw new IllegalArgumentException( "session id must be non-null and non-empty" );
		this.id = id;
	}

	/**
	 * Return the session ID. This method is reflectively invoked by {@code WebSession.getSession}.
	 * @return the session ID
	 */
	public String getId()
	{
		return id;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals( Object obj )
	{
		return obj == this;
	}

	@Override
	public String toString()
	{
		return "HeadlessHttpSession[" + id + "]";
	}
}
