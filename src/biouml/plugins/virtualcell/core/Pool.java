package biouml.plugins.virtualcell.core;

import java.util.Map;

import ru.biosoft.access.core.VectorDataCollection;

/**
 * Generic collection of biological entities.
 * 
 * It can be loaded from The Virtual Cell build or database.
 */
public class Pool extends VectorDataCollection
{
	public Pool(String name)
	{
		super(name);
	}
	
	private String version;
	public String getVersion()			
	{ 
		return version; 
	}
	
	public void initValues(Map values)
	{
		// stub
	}
	
}
