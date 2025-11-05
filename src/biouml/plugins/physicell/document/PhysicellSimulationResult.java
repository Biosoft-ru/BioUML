package biouml.plugins.physicell.document;

import java.awt.image.BufferedImage;
import java.util.TreeMap;

import biouml.standard.type.BaseSupport;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.physicell.ui.DensityState;
import ru.biosoft.physicell.ui.ModelData;
import ru.biosoft.table.TableDataCollection;

public class PhysicellSimulationResult extends BaseSupport
{
    private DataCollection<DataElement> dcAgents = null;
    private DataCollection<DataElement> dcDensity = null;
    protected ModelData modelData;
    private BufferedImage legend = null;
    private ViewOptions options = new ViewOptions();
    private TreeMap<Integer, String> agentElements = new TreeMap<>();
    private TreeMap<Integer, String> densityElements = new TreeMap<>();
    private int step;
    private int maxTime;

    public PhysicellSimulationResult(String name, DataCollection de) throws Exception
    {
        super( null, name );
        DataElement legendDe = de.get( "Legend" );
        if (legendDe instanceof ImageDataElement)
        {
            legend = ((ImageDataElement)legendDe).getImage();
        }
        this.dcAgents = (DataCollection)de.get( "Cells" );
        this.dcDensity = (DataCollection)de.get( "Density" );
        modelData = Util.read((TextDataElement)de.get( "info.txt" ));
        options.setSubstrates( modelData.getSubstrates() );
        options.set2D( modelData.isUse2D() );
    }
    
    public BufferedImage getLegend()
    {
        return legend;
    }

    public ViewOptions getOptions()
    {
        return options;
    }
    
    public ModelData getModelData()
    {
        return modelData;
    }

    public int getMaxTime()
    {
        return maxTime;
    }
    
    public boolean hasDensity()
    {
        return  dcDensity != null;
    }
    
    public void init()
    {
        densityElements.clear();
        if( hasDensity() )
        {
            for( Object nameObj : dcDensity.getNameList() )
            {
                String name = nameObj.toString();
                Integer time = Integer.parseInt( name.split( "_" )[1] );
                densityElements.put( time, name );
            }
        }
        agentElements.clear();
        for( Object nameObj : dcAgents.getNameList() )
        {
            String name = nameObj.toString();
            Integer time = Integer.parseInt( name.split( "_" )[1] );
            agentElements.put( time, name );
        }
        step = agentElements.navigableKeySet().higher( 0 );
        maxTime = agentElements.navigableKeySet().last();
        options.setSize( modelData, maxTime );
        options.setTimeStep( step );
        options.setSubstrates( modelData.getSubstrates() );

    }
    
    public int floorTime(int time)
    {
        return agentElements.floorKey( time );
    }

    public TextDataElement getPoint(int time) throws Exception
    {
        return (TextDataElement)dcAgents.get( agentElements.floorEntry( time ).getValue() );
    }
    
    public DensityState getDensity(int time, String substrate) throws Exception
    {
        if( hasDensity() )
            return Util.fromTable( (TableDataCollection)dcDensity.get( densityElements.floorEntry( time ).getValue() ), substrate );
        return null;
    }
}