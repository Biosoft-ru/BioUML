package biouml.plugins.gtrd;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

public class ExperimentalFactor extends DataElementSupport 
{
	private String factorId;
	private String title;
	private String parent;
	private String info;
	private String[] exRefs;
	
	public ExperimentalFactor(String id, String factorId, String title, String parent, DataCollection origin) 
	{
		super(id, origin);
		this.factorId = factorId;
		this.title = title;
		this.parent = parent;
	}

	
	public String getFactorId() {
		return factorId;
	}

	public void setFactorId(String factorId) {
		this.factorId = factorId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}


	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}
	
	public String getInfo() {
		return info;
	}


	public void setInfo(String info) {
		this.info = info;
	}


	public String[] getExRefs() {
		return exRefs;
	}

	public void setExRefs(String[] exRefs) {
		this.exRefs = exRefs;
	}

	public String toString()
    {
        return title;
    }
	
	

}
