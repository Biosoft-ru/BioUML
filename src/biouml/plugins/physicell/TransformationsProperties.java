package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Node;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellInteractions;
import ru.biosoft.physicell.core.CellTransformations;
import ru.biosoft.physicell.core.Model;

@PropertyName ( "Cell transformations" )
public class TransformationsProperties extends Option
{
    private TransformationProperties[] transformations = new TransformationProperties[0];
    private Node node;

    public TransformationsProperties()
    {
    }

    public TransformationsProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public void setDiagramElement(DiagramElement de)
    {
        if( de instanceof Node )
        {
            this.node = (Node)de;
            TransformationProperties[] tp = node.edges().filter( e -> e.getInput().equals( node ) ).map( e -> e.getRole() )
                    .select( TransformationProperties.class ).toArray( TransformationProperties[]::new );
            setTransformations( tp );
        }
        else
            setTransformations( new TransformationProperties[0] );
    }

    public TransformationsProperties clone(DiagramElement de)
    {
        return new TransformationsProperties( de );
    }

    public void createCellTransformations(CellDefinition cd, Model model)
    {
        CellTransformations cellTransformation = cd.phenotype.cellTransformations;

        for( TransformationProperties properties : transformations )
        {
            String cellType = properties.getCellType();
            CellDefinition otherCD = model.getCellDefinition( cellType );
            int index = otherCD.type;
            cellTransformation.transformationRates[index] = properties.getRate();
        }
    }

    public void addTransformation(TransformationProperties transformation)
    {
        TransformationProperties[] newInteractions = new TransformationProperties[this.transformations.length + 1];
        System.arraycopy( transformations, 0, newInteractions, 0, transformations.length );
        newInteractions[transformations.length] = transformation;
        this.setTransformations( newInteractions );
    }

    public void update()
    {
        setDiagramElement( node );
    }

    @PropertyName ( "Cell types" )
    public TransformationProperties[] getTransformations()
    {
        return transformations;
    }
    public void setTransformations(TransformationProperties[] transformations)
    {
        Object oldValue = this.transformations;
        this.transformations = transformations;
        firePropertyChange( "transformations", oldValue, transformations );
        firePropertyChange( "*", null, null );
    }

    public String getTransformationName(Integer i, Object obj)
    {
        return ( (TransformationProperties)obj ).getCellType();
    }
}