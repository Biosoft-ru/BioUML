package biouml.plugins.physicell.cycle;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;
import biouml.plugins.physicell.PhaseProperties;
import biouml.plugins.physicell.TransitionProperties;

public class CycleEModel extends EModel
{

    private boolean deathModel = false;


    public CycleEModel(DiagramElement diagramElement)
    {
        super( diagramElement );
    }

    public TransitionProperties[] getTransitionProperties()
    {
        return getDiagramElement().recursiveStream().map( de -> de.getRole() ).select( TransitionProperties.class )
                .toArray( TransitionProperties[]::new );
    }

    public PhaseProperties[] getPhaseProperties()
    {
        return getDiagramElement().recursiveStream().map( de -> de.getRole() ).select( PhaseProperties.class )
                .toArray( PhaseProperties[]::new );
    }

    @PropertyName ( "Death Model" )
    public boolean isDeathModel()
    {
        return deathModel;
    }

    public void setDeathModel(boolean deathModel)
    {
        this.deathModel = deathModel;
    }

    @Override
    public CycleEModel clone(DiagramElement de)
    {
        CycleEModel emodel = new CycleEModel( de );
        emodel.deathModel = deathModel;
        return emodel;
    }
}