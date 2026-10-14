package biouml.plugins.mcp.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.SerializableAsText;

/**
 * Converts arbitrary Java values into plain JSON-serializable values (Maps, Lists, Strings, Numbers,
 * Booleans, null).
 *
 * <p>Raw domain objects must never be handed to Jackson: it serializes them by calling every public
 * getter, so e.g. a {@link DataElementPath} drags in {@code getDataElement()}, {@code getChildren()},
 * {@code getParentPath()}… — loading repository collections and running into cycles, which makes the
 * whole tool result fail with {@code serialization_error}. Instead, values are reduced to their text
 * form: paths to their string, {@link SerializableAsText} beans to {@code getAsText()}, enums to their
 * name, data elements to their complete path, anything else to {@code String.valueOf}.</p>
 */
public final class McpJsonSafe
{
	private McpJsonSafe()
	{
	}

	/** A JSON-serializable copy of {@code value}. */
	public static Object toJsonSafe( Object value )
	{
		return convert( value, new IdentityHashMap<Object, Boolean>() );
	}

	private static Object convert( Object value, IdentityHashMap<Object, Boolean> visiting )
	{
		if ( value == null || value instanceof String || value instanceof Boolean || value instanceof Number )
			return value;
		if ( value instanceof Enum )
			return ( (Enum<?>) value ).name();
		if ( value instanceof Character || value instanceof DataElementPath )
			return value.toString();
		// Before the Iterable branch: a DataCollection is iterable, and must not be walked.
		if ( value instanceof DataElement )
		{
			try
			{
				return DataElementPath.create( (DataElement) value ).toString();
			}
			catch ( Exception e )
			{
				return ( (DataElement) value ).getName();
			}
		}
		if ( value instanceof SerializableAsText )
		{
			try
			{
				return ( (SerializableAsText) value ).getAsText();
			}
			catch ( Exception e )
			{
				return String.valueOf( value );
			}
		}
		if ( value instanceof Map || value instanceof Iterable || value.getClass().isArray() )
		{
			// Guard against self-containing containers.
			if ( visiting.put( value, Boolean.TRUE ) != null )
				return null;
			try
			{
				if ( value instanceof Map )
				{
					Map<String, Object> out = new LinkedHashMap<String, Object>();
					for ( Map.Entry<?, ?> e : ( (Map<?, ?>) value ).entrySet() )
						out.put( String.valueOf( e.getKey() ), convert( e.getValue(), visiting ) );
					return out;
				}
				List<Object> out = new ArrayList<Object>();
				if ( value instanceof Iterable )
				{
					for ( Object o : (Iterable<?>) value )
						out.add( convert( o, visiting ) );
				}
				else
				{
					int n = Array.getLength( value );
					for ( int i = 0; i < n; i++ )
						out.add( convert( Array.get( value, i ), visiting ) );
				}
				return out;
			}
			finally
			{
				visiting.remove( value );
			}
		}
		try
		{
			return String.valueOf( value );
		}
		catch ( Exception e )
		{
			return value.getClass().getName();
		}
	}
}
