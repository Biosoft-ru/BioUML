package biouml.plugins.hemodynamics;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;
import biouml.model.dynamics.EModel;

public class HemodynamicsEModel extends EModel
{

    public HemodynamicsEModel(DiagramElement diagramElement)
    {
        super( diagramElement );

        try
        {
            fillVesselsCollection( (Diagram)diagramElement );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        fillInVariables();
    }

    private void fillInVariables()
    {
        setPropagationEnabled( false );
        variables.setPropagationEnabled( false );
        declareVariable( "inputFlow", 200.0 );
        declareVariable( "outputFlow", 110.0 );
        declareVariable( "arterialResistance", 0.08547 );
        declareVariable( "totalVolume", 360.0 );
        declareVariable( "averagePressure", 100.0 );

        declareVariable( "inputPressure", 100.0 );
        declareVariable( "outputPressure", 70.0 );
        declareVariable( "inputArea", 20.0 );
        declareVariable( "outputArea", 3.0 );
        declareVariable( "arteriaElasticity", 0.6 );
        declareVariable( "venousPressure", 2.01 );
        declareVariable( "capillaryResistance", 1.1 );
        declareVariable( "humoralFactor", 1.0 );
       
        declareVariable( "bloodViscosity", 0.035 );

        declareVariable( "renalConductivity", 31.67 );
        declareVariable( "kidneyResistance", 83.33 );
        declareVariable( "kidneyInputFlow", 20.0 );
        declareVariable( "nueroReceptorsControl", 1.0 );
        declareVariable( "vascularity", 1.0 );
        declareVariable( "externalPressure", 0.0 );
        declareVariable( "bloodLoss", 0.0 );
        declareVariable( "vesselSegments",5.0 );
        declareVariable( "integrationSegments", 5.0 );
        declareVariable( "ventriclePressure", 125.0 );
        declareVariable( "bloodViscosity", 0.035 );
        
        declareVariable( "systole", 1.0 );
        declareVariable( "factorArea", 1.0 );
        declareVariable( "factorBeta", 1.0 );
        declareVariable( "factorArea2", 1.0 );
        declareVariable( "factorBeta2", 1.0 );
        declareVariable( "factorLength", 1.0 );
        declareVariable( "factorWeight", 1.0 );
        
        declareVariable("capillaryConductivityFactor", 1.0);
        
        declareVariable( "referencedPressure", 100.0 );
        variables.setPropagationEnabled( true );
        setPropagationEnabled( true );
    }

    @Override
    public void removeNotUsedParameters()
    {

    }

    @Override
    public Role clone(DiagramElement de)
    {
        HemodynamicsEModel emodel = new HemodynamicsEModel( de );
        doClone( emodel );
        return emodel;
    }

    public static final String HEMODYNAMICS_EMODEL_TYPE = "Hemodynamics EModel";

    @Override
    public String getType()
    {
        return HEMODYNAMICS_EMODEL_TYPE;
    }

    DataCollection<Vessel> vessels;
    public DataCollection<Vessel> getVessels()
    {
        return vessels;
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        if( !notificationEnabled )
            return;

        super.elementAdded( e );

        DataElement de = e.getDataElement();
        if( de instanceof Edge && Util.isVessel( (Edge)de ) )
        {
            Vessel vessel = Util.getVessel( (Edge)de );
            if( vessel != null )
                getVessels().put( vessel );
            firePropertyChange( "vessels", null, null );
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        if( !notificationEnabled )
            return;

        super.elementRemoved( e );

        if( elementToRemove instanceof Edge && Util.isVessel( (Edge)elementToRemove ) )
        {
            Vessel vessel = Util.getVessel( (Edge)elementToRemove );
            if( vessel != null )
                getVessels().remove( vessel.getName() );
        }
        firePropertyChange( "vessels", null, null );
    }

    private void fillVesselsCollection(Diagram diagram) throws Exception
    {
        Properties props = new Properties();
        props.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Vessel.class.getName());
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "Vessels");
        
        vessels = new VectorDataCollection<>(diagram.getName()
                + " vessels(vectorDC)", null, props);
        
        diagram.stream( Edge.class ).filter( Util::isVessel ).map( Util::getVessel ).nonNull().forEach( vessels::put );
    }
}
