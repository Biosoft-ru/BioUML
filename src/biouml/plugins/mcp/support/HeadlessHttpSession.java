package biouml.plugins.mcp.support;

import java.util.HashMap;
import java.util.Map;

/**
 * A synthetic carrier object that mimics a {@code javax.servlet.http.HttpSession} for headless
 * (non-servlet) use. {@code WebSession} reflectively looks up and invokes the following public
 * methods on the carrier:
 * <ul>
 *   <li>{@code String getId()} — the session ID (used by {@code WebSession.getSession} and
 *       {@code WebSession.getSessionId})</li>
 *   <li>{@code Object getValue(String)} — read a session attribute</li>
 *   <li>{@code void putValue(String, Object)} — write a session attribute</li>
 *   <li>{@code boolean removeValue(String)} — remove a session attribute</li>
 * </ul>
 * This class provides all of them, backed by a simple in-memory map, so that async (job-based)
 * providers like {@code CopyFolderProvider}, {@code SimulationProvider}, and {@code WebScriptsProvider}
 * can store and retrieve their {@code WebJob} objects headlessly.
 */
public class HeadlessHttpSession
{
	private final String id;
	private final Map<String, Object> attributes = new HashMap<String, Object>();

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
	 * Return the session ID. Reflectively invoked by {@code WebSession.getSession}.
	 * @return the session ID
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Read a session attribute. Reflectively invoked by {@code WebSession.getValue}.
	 * @param key the attribute name
	 * @return the attribute value, or {@code null} if absent
	 */
	public Object getValue( String key )
	{
		return attributes.get( key );
	}

	/**
	 * Write a session attribute. Reflectively invoked by {@code WebSession.putValue}.
	 * @param key   the attribute name
	 * @param value the attribute value
	 */
	public void putValue( String key, Object value )
	{
		attributes.put( key, value );
	}

	/**
	 * Remove a session attribute. Reflectively invoked by {@code WebSession.removeValue}.
	 * @param key the attribute name
	 * @return {@code true} if the attribute was present and removed
	 */
	public boolean removeValue( String key )
	{
		return attributes.remove( key ) != null;
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

