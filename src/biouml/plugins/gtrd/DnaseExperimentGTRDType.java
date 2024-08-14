package biouml.plugins.gtrd;

import java.util.regex.Pattern;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class DnaseExperimentGTRDType extends ReferenceTypeSupport{

	private static Pattern ID_PAT = Pattern.compile("DEXP[0-9]{6}");
	
	@Override
	public int getIdScore(String id) 
	{
		 if(ID_PAT.matcher(id).matches())
            return SCORE_HIGH_SPECIFIC;
	     return SCORE_NOT_THIS_TYPE;
	}

	@Override
	public String getObjectType() 
	{
		return "Dnase experiment";
	}
	
	@Override
    public String getSource()
    {
        return "GTRD";
    }

    @Override
    public DataElementPath getPath(String id)
    {
        return DataElementPath.create( "databases/GTRD/Data/DNase experiments/" ).getChildPath( id );
    }

}
