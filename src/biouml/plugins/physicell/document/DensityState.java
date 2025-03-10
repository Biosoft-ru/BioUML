package biouml.plugins.physicell.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class DensityState
{
    private String name;
    private List<double[]> densities = new ArrayList<>();
    private Map<String, Integer> names = new HashMap<String, Integer>();

    public void addDensity(String name, double[] density)
    {
        int index = densities.size();
        densities.add( density );
        names.put( name, index );
    }

    public double[] getDensity(String name)
    {
        Integer index = names.get( name );
        if( index == null )
            return null;
        return densities.get( index );
    }
    
    public String[] getSubstrates()
    {
        return names.keySet().toArray( String[]::new );
    }
    
    public String getName()
    {
        return name;
    }

    public static DensityState fromTable(TableDataCollection tdc)
    {
        DensityState result = new DensityState();
        result.name = tdc.getName();
        int count = tdc.getColumnModel().getColumnCount();
        for( int i = 3; i < count; i++ )
        {
            String name = tdc.getColumnModel().getColumn( i ).getName();
            double[] vals = TableDataCollectionUtils.getColumn( tdc, name );
            result.addDensity( name, vals );
        }
        return result;
    }
}
