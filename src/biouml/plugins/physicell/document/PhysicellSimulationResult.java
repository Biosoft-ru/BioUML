package biouml.plugins.physicell.document;

import java.util.TreeMap;

import biouml.standard.type.BaseSupport;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.GenericDataCollection;

public class PhysicellSimulationResult extends BaseSupport
{
    private GenericDataCollection dc;
    ViewOptions options = new ViewOptions();
    static TreeMap<Integer, TextDataElement> files = new TreeMap<>();
    static int step;
    int maxTime;

    public PhysicellSimulationResult(String name, GenericDataCollection de)
    {
        super( null, name );
        this.dc = de;
    }

    public GenericDataCollection getCollection()
    {
        return dc;
    }

    public ViewOptions getOptions()
    {
        return options;
    }

    public void init()
    {
        files.clear();
        for( DataElement de : dc )
        {
            if( de instanceof TextDataElement )
            {
                String name = de.getName();
                Integer time = Integer.parseInt( name.split( "_" )[1] );
                files.put( time, (TextDataElement)de );
            }
        }
        step = files.navigableKeySet().higher( 0 );
        maxTime =  files.navigableKeySet().last();
        options.setSize( 1500, 1500, 1500, maxTime);

    }

    public TextDataElement getPoint(int time)
    {
        return files.floorEntry( time ).getValue();
    }
}