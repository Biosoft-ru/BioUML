package biouml.plugins.physicell;


import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Node;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellInteractions;
import ru.biosoft.physicell.core.Model;


@PropertyName ( "Cell interactions" )
public class InteractionsProperties extends Option
{
    private InteractionProperties[] interactions = new InteractionProperties[0];
    private Node node;
    private double damageRate = 0;
    private double deadPhagocytosisRate = 0;

    public InteractionsProperties()
    {
    }

    public InteractionsProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public void setDiagramElement(DiagramElement de)
    {
        if( de instanceof Node )
        {
            this.node = (Node)de;
            InteractionProperties[] edges = node.edges().filter( e -> e.getInput().equals( node ) ).map( e -> e.getRole() )
                    .select( InteractionProperties.class ).toArray( InteractionProperties[]::new );
            setInteractions( edges );
        }
        else
            setInteractions( new InteractionProperties[0] );
    }

    public InteractionsProperties clone(DiagramElement de)
    {
        InteractionsProperties result = new InteractionsProperties( de );
        result.damageRate = damageRate;
        result.deadPhagocytosisRate = deadPhagocytosisRate;
        return result;
    }

    public void createCellInteractions(CellDefinition cd, Model model)
    {
        CellInteractions cellInteractions = cd.phenotype.cellInteractions;
        cellInteractions.damageRate = damageRate;
        cellInteractions.deadPhagocytosisRate = deadPhagocytosisRate;

        for( InteractionProperties properties : interactions )
        {
            String cellType = properties.getCellType();
            CellDefinition otherCD = model.getCellDefinition( cellType );
            int index = otherCD.type;
            cellInteractions.attackRates[index] = properties.getAttackRate();
            cellInteractions.fusionRates[index] = properties.getFuseRate();
            cellInteractions.livePhagocytosisRates[index] = properties.getPhagocytosisRate();
        }
    }

    public void addInteraction(InteractionProperties interaction)
    {
        InteractionProperties[] newInteractions = new InteractionProperties[this.interactions.length + 1];
        System.arraycopy( interactions, 0, newInteractions, 0, interactions.length );
        newInteractions[interactions.length] = interaction;
        this.setInteractions( newInteractions );
    }

    public void update()
    {
        setDiagramElement( node );
    }

    @PropertyName ( "Cell types" )
    public InteractionProperties[] getInteractions()
    {
        return interactions;
    }
    public void setInteractions(InteractionProperties[] interactions)
    {
        Object oldValue = this.interactions;
        this.interactions = interactions;
        firePropertyChange( "interactions", oldValue, interactions );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Damage rate" )
    public double getDamageRate()
    {
        return damageRate;
    }
    public void setDamageRate(double damageRate)
    {
        this.damageRate = damageRate;
    }

    @PropertyName ( "Dead Phagocytosis rate" )
    public double getDeadPhagocytosisRate()
    {
        return deadPhagocytosisRate;
    }

    public void setDeadPhagocytosisRate(double deadPhagocytosisRate)
    {
        this.deadPhagocytosisRate = deadPhagocytosisRate;
    }

    public String getInteractionName(Integer i, Object obj)
    {
        return ( (InteractionProperties)obj ).getCellType();
    }
}