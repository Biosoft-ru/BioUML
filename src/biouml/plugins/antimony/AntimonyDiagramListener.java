package biouml.plugins.antimony;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.util.DPSUtils;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;

public class AntimonyDiagramListener implements ViewPaneListener, DataCollectionListener, PropertyChangeListener
{
    public static final String LISTENER_ATTR = "listener";
    protected Diagram diagram;
    protected Antimony antimony;

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals( "attributes/" + AntimonyConstants.ANTIMONY_TEXT_ATTR )
                || evt.getPropertyName().equals( "attributes/" + AntimonyConstants.ANTIMONY_LINK )
                || evt.getPropertyName().equals( "location" ) )
            return;
        String currentText = AntimonyUtility.getAntimonyAttribute( diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR );
        if( currentText == null )
            currentText = "";
        String text;
        try
        {
            text = antimony.updateText( evt );
        }
        catch( Exception e )
        {
            return;
        }
        if( text != null && !currentText.equals( text ) )
            AntimonyUtility.setAntimonyAttribute( diagram, text, AntimonyConstants.ANTIMONY_TEXT_ATTR );
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        String currentText = AntimonyUtility.getAntimonyAttribute( diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR );
        if( currentText == null )
            currentText = "";
        String text;
        try
        {
            text = antimony.updateText( e );
        }
        catch( Exception ex )
        {
            return;
        }
        if( text != null && !currentText.equals( text ) )
            AntimonyUtility.setAntimonyAttribute( diagram, text, AntimonyConstants.ANTIMONY_TEXT_ATTR );
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        if( e.getDataElement() instanceof ModelDefinition )
            return;
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        String currentText = AntimonyUtility.getAntimonyAttribute( diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR );
        if( currentText == null )
            currentText = "";
        String text;
        try
        {
            text = antimony.updateText( e );
        }
        catch( Exception ex )
        {
            return;
        }
        if( text != null && !currentText.equals( text ) )
            AntimonyUtility.setAntimonyAttribute( diagram, text, AntimonyConstants.ANTIMONY_TEXT_ATTR );
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        String currentText = AntimonyUtility.getAntimonyAttribute( diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR );
        if( currentText == null )
            currentText = "";
        String text;
        try
        {
            text = antimony.updateText( e );
        }
        catch( Exception ex )
        {
            return;
        }
        if( text != null && !currentText.equals( text ) )
            AntimonyUtility.setAntimonyAttribute( diagram, text, AntimonyConstants.ANTIMONY_TEXT_ATTR );
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
    }

    @Override
    public void mousePressed(ViewPaneEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseEntered(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseExited(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseDragged(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseMoved(ViewPaneEvent e)
    {
    }

    public void release(Diagram diagram)
    {
        this.diagram = null;
        this.antimony = null;

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
        this.antimony = new Antimony( diagram );
        antimony.createAst();

        DynamicProperty dp = new DynamicProperty( LISTENER_ATTR, AntimonyDiagramListener.class, this );
        DPSUtils.makeTransient( dp );
        dp.setHidden( true );
        diagram.getAttributes().add( dp );

        linkDiagram();

        diagram.addDataCollectionListener( this );
        diagram.addPropertyChangeListener( this );
        diagram.getRole( EModel.class ).getVariables().addDataCollectionListener( this );
    }

    private void linkDiagram()
    {
        try
        {
            EModel emodel = (EModel)diagram.getRole();
            Diagram linkedDiagram = new AntimonyDiagramGenerator().generateDiagram( antimony.astStart, diagram );
            EModel linkedEmodel = (EModel)linkedDiagram.getRole();
            if( emodel == null || linkedEmodel == null )
                return;
            for( Variable var : linkedEmodel.getVariables() )
            {
                DynamicProperty dp;
                if( var instanceof VariableRole )
                    dp = ( (VariableRole)var ).getDiagramElement().getAttributes().getProperty( AntimonyConstants.ANTIMONY_LINK );
                else
                    dp = var.getAttributes().getProperty( AntimonyConstants.ANTIMONY_LINK );
                if( dp != null )
                {
                    Variable notLinkedVar = emodel.getVariable( var.getName() );
                    if( notLinkedVar instanceof VariableRole )
                        ( (VariableRole)notLinkedVar ).getDiagramElement().getAttributes().add( dp );
                    else if( notLinkedVar != null )
                        notLinkedVar.getAttributes().add( dp );
                }
            }

            linkedEmodel.getEquations().map(eq -> eq.getDiagramElement()).filter(de ->  de instanceof Node)
                    .forEach(de -> copyAntimonyLinkFrom(de));

            for( Event eq : linkedEmodel.getEvents() )
            {
                DiagramElement de = eq.getDiagramElement();
                if( de != null )
                    copyAntimonyLinkFrom( de );
            }
            for( Function eq : linkedEmodel.getFunctions() )
            {
                DiagramElement de = eq.getDiagramElement();
                if( de != null )
                    copyAntimonyLinkFrom( de );
            }
            for( Node node : AntimonyUtility.getSubdiagrams( linkedDiagram ) )
                copyAntimonyLinkFrom( node );
            for( Node node : AntimonyUtility.getModelDefinitions( linkedDiagram ) )
                copyAntimonyLinkFrom( node );
        }
        catch( Exception e )
        {
            e.printStackTrace();
            return;
        }
    }

    private void copyAntimonyLinkFrom(DiagramElement de)
    {
        DynamicProperty dp = de.getAttributes().getProperty( AntimonyConstants.ANTIMONY_LINK );
        Node notLinked = diagram.findNode( de.getName() );
        if( dp != null && notLinked != null )
            notLinked.getAttributes().add( dp );
    }

    public void setAntimony(Antimony antimony)
    {
        this.antimony = antimony;
    }

}
