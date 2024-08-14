package biouml.plugins.physicell;


import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Node;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Motility;

@PropertyName ( "Motility" )
@PropertyDescription ( "Motility." )
public class MotilityProperties extends Option
{
    private Motility motility = new Motility();
    private ChemotaxisProperties[] chemotaxis = new ChemotaxisProperties[0];
    private Node node;

    public MotilityProperties()
    {

    }
    public MotilityProperties(Motility motility)
    {
        this.motility = motility.clone();
    }

    public MotilityProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public MotilityProperties clone(DiagramElement de)
    {
        MotilityProperties result = new MotilityProperties( de );
        result.motility = motility.clone();
        return result;
    }

    public void createMotility(CellDefinition cd)
    {
        Microenvironment m = cd.getMicroenvironment();
        Motility motility = cd.phenotype.motility;
        motility.migrationBias = getMigrationBias();
        motility.migrationSpeed = getMigrationSpeed();
        motility.persistenceTime = getPersistenceTime();
        motility.restrictTo2D = isRestrictTo2D();
        motility.isMotile = isMotile();

        for( ChemotaxisProperties properties : chemotaxis )
        {
            String substrate = properties.getTitle();
            int index = m.findDensityIndex( substrate );
            motility.chemotaxisDirection = properties.getDirection();
            motility.chemotacticSensitivities[index] = properties.getSensitivity();
            motility.chemotaxisIndex = index;
        }
    }

    public void update()
    {
        setDiagramElement( node );
    }

    public void setDiagramElement(DiagramElement de)
    {
        if( de instanceof Node )
        {
            this.node = (Node)de;
            setChemotaxis(
                    node.edges().map( e -> e.getRole() ).select( ChemotaxisProperties.class ).toArray( ChemotaxisProperties[]::new ) );
        }
        else
            this.setChemotaxis( new ChemotaxisProperties[0] );
    }

    public boolean isNotMotile()
    {
        return !isMotile();
    }

    @PropertyName ( "Is Motile" )
    public boolean isMotile()
    {
        return motility.isMotile;
    }
    public void setMotile(boolean motile)
    {
        boolean oldValue = this.isMotile();
        this.motility.isMotile = motile;
        firePropertyChange( "motile", oldValue, motile );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Chemotaxis" )
    public ChemotaxisProperties[] getChemotaxis()
    {
        return chemotaxis;
    }
    public void setChemotaxis(ChemotaxisProperties[] chemotaxis)
    {
        Object oldValue = this.chemotaxis;
        this.chemotaxis = chemotaxis;
        firePropertyChange( "chemotaxis", oldValue, chemotaxis );
        firePropertyChange( "*", null, null );
    }

    public void addChemotaxis(ChemotaxisProperties secretion)
    {
        ChemotaxisProperties[] newChemotaxis = new ChemotaxisProperties[this.chemotaxis.length + 1];
        System.arraycopy( chemotaxis, 0, newChemotaxis, 0, chemotaxis.length );
        newChemotaxis[chemotaxis.length] = secretion;
        this.setChemotaxis( newChemotaxis );
    }

    @PropertyName ( "Migration speed" )
    public double getMigrationSpeed()
    {
        return motility.migrationSpeed;
    }

    public void setMigrationSpeed(double migrationSpeed)
    {
        motility.migrationSpeed = migrationSpeed;
    }

    @PropertyName ( "Persistence time" )
    public double getPersistenceTime()
    {
        return motility.persistenceTime;
    }

    public void setPersistenceTime(double persistenceTime)
    {
        motility.persistenceTime = persistenceTime;
    }

    @PropertyName ( "Migration bias" )
    public double getMigrationBias()
    {
        return motility.migrationBias;
    }

    public void setMigrationBias(double migrationBias)
    {
        motility.migrationBias = migrationBias;
    }

    @PropertyName ( "Restricted to 2D" )
    public boolean isRestrictTo2D()
    {
        return motility.restrictTo2D;
    }
    public void setRestrictTo2D(boolean restrictTo2D)
    {
        motility.restrictTo2D = restrictTo2D;
    }

    public String getChemotaxisName(Integer i, Object obj)
    {
        return ( (ChemotaxisProperties)obj ).getTitle();
    }
}