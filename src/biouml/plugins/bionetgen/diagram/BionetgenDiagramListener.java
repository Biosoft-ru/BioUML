package biouml.plugins.bionetgen.diagram;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.stream.Collectors;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.DPSUtils;

public class BionetgenDiagramListener implements DataCollectionListener, PropertyChangeListener
{
    public static final String LISTENER_ATTR = "listener";
    protected Diagram diagram;
    protected Bionetgen bionetgen;

    public void release(Diagram diagram)
    {
        this.diagram = null;
        this.bionetgen = null;

        diagram.getAttributes().remove( LISTENER_ATTR );

        diagram.removePropertyChangeListener( this );
        diagram.removeDataCollectionListener( this );
        diagram.getRole( EModel.class ).getVariables().removeDataCollectionListener( this );
    }

    public void register(Diagram diagram)
    {
        if( this.diagram == diagram )
            return;
        else if( this.diagram != null )
            release( this.diagram );

        this.diagram = diagram;
        this.bionetgen = new Bionetgen( diagram );

        DynamicProperty dp = new DynamicProperty( LISTENER_ATTR, BionetgenDiagramListener.class, this );
        DPSUtils.makeTransient( dp );
        dp.setHidden( true );
        diagram.getAttributes().add( dp );

        linkDiagram();

        diagram.addDataCollectionListener( this );
        diagram.addPropertyChangeListener( this );
        diagram.getRole( EModel.class ).getVariables().addDataCollectionListener( this );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( isIgnoringProperty( evt.getPropertyName() ) )
            return;
        String currentText = BionetgenUtils.getBionetgenAttr( diagram );
        updateText( evt, currentText != null ? currentText : "" );
    }
    private boolean isIgnoringProperty(String propertyName)
    {
        return propertyName.equals( "location" ) || propertyName.equals( "variables" ) || propertyName.endsWith( Bionetgen.BIONETGEN_ATTR )
                || propertyName.equals( "path" );
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        String currentText = BionetgenUtils.getBionetgenAttr( diagram );
        updateText( e, currentText != null ? currentText : "" );
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        String currentText = BionetgenUtils.getBionetgenAttr( diagram );
        updateText( e, currentText != null ? currentText : "" );
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        if( e.getType() == DataCollectionEvent.ELEMENT_WILL_REMOVE )
        {
            String currentText = BionetgenUtils.getBionetgenAttr( diagram );
            updateText( e, currentText != null ? currentText : "" );
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        String currentText = BionetgenUtils.getBionetgenAttr( diagram );
        updateText( e, currentText != null ? currentText : "" );
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    protected void updateText(DataCollectionEvent e, String currentText)
    {
        try
        {
            String text = bionetgen.updateText( e );
            if( !currentText.equals( text ) )
                BionetgenUtils.setBionetgenAttr( diagram, text );
        }
        catch( Exception ex )
        {
            ApplicationUtils.errorBox( ExceptionRegistry.log( ex ) );
            return;
        }
    }
    protected void updateText(PropertyChangeEvent evt, String currentText)
    {
        try
        {
            String text = bionetgen.updateText( evt );
            if( !currentText.equals( text ) )
                BionetgenUtils.setBionetgenAttr( diagram, text );
        }
        catch( Exception ex )
        {
            ApplicationUtils.errorBox( ExceptionRegistry.log( ex ) );
            return;
        }
    }

    private void linkDiagram()
    {
        try
        {
            EModel emodel = (EModel)diagram.getRole();
            Diagram linkedDiagram = BionetgenDiagramGenerator.generateDiagram( bionetgen.bngStart, diagram, false );
            EModel linkedEmodel = (EModel)linkedDiagram.getRole();
            if( emodel == null || linkedEmodel == null )
                return;
            for( Variable var : linkedEmodel.getVariables().stream().filter( v -> ! ( v instanceof VariableRole ) )
                    .collect( Collectors.toSet() ) )
            {
                DynamicProperty dp = var.getAttributes().getProperty( Bionetgen.BIONETGEN_LINK );
                if( dp != null )
                {
                    Variable notLinkedVar = emodel.getVariable( var.getName() );
                    if( notLinkedVar != null )
                        notLinkedVar.getAttributes().add( dp );
                }
            }

            linkedDiagram.recursiveStream().select( Node.class ).forEach( n -> copyBionetgenLinkFrom( n ) );
        }
        catch( Exception e )
        {
            e.printStackTrace();
            return;
        }
    }

    private void copyBionetgenLinkFrom(Node node)
    {
        DynamicProperty dp = node.getAttributes().getProperty( Bionetgen.BIONETGEN_LINK );
        Node notLinked = diagram.findNode( node.getName() );
        if( dp != null && notLinked != null )
            notLinked.getAttributes().add( dp );
    }
}
