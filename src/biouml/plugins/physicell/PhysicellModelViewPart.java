package biouml.plugins.physicell;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.RowModel;

import biouml.model.Diagram;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.PluggedEditorsTabbedPane;
import ru.biosoft.gui.ViewPartSupport;

public class PhysicellModelViewPart extends ViewPartSupport implements PropertyChangeListener, DataCollectionListener
{
    private JTabbedPane tabbedPane;
    private MulticellEModel emodel;

    private PropertyInspector domainInspector = new PropertyInspector();
    private PropertyInspector parametersInspector = new PropertyInspector();
    private PropertyInspector initialConditionInspector = new PropertyInspector();
    private PropertyInspector reportInspector = new PropertyInspector();
    
    public PhysicellModelViewPart()
    {
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
    }

    private void update()
    {
        Object parent = getParent().getParent();
        if( parent instanceof PluggedEditorsTabbedPane )
        {
            PluggedEditorsTabbedPane pane = (PluggedEditorsTabbedPane)parent;
            pane.updateActions();
        }
    }

    private void initTabbedPane(MulticellEModel emodel)
    {
        tabbedPane.removeAll();
        tabbedPane.addTab( "Domain", domainInspector );
        tabbedPane.addTab( "Substrates", new SubstrateViewPart( emodel ) );
        tabbedPane.addTab( "Cell types", new CellDefinitionsViewPart( emodel ) );
        tabbedPane.addTab( "User Parameters", parametersInspector );
        tabbedPane.addTab( "Initial Condition", initialConditionInspector );
        tabbedPane.addTab( "Model Report", reportInspector );
        tabbedPane.addChangeListener( new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                update();
            }
        } );
        update();
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( emodel != null )
            emodel.removePropertyChangeListener( this );

        if( model != null )
            ( (Diagram)model ).removeDataCollectionListener( this );

        emodel = ( (Diagram)model ).getRole( MulticellEModel.class );
        emodel.addPropertyChangeListener( this );
        ( (Diagram)model ).addDataCollectionListener( this );
        domainInspector.explore( emodel.getDomain() );
        parametersInspector.explore( emodel.getUserParmeters() );
        initialConditionInspector.explore( emodel.getInitialCondition() );
        reportInspector.explore( emodel.getReportProperties() );
        initTabbedPane( emodel );
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ( (Diagram)model ).getRole() instanceof MulticellEModel;
    }

    @Override
    public Action[] getActions()
    {
        return new Action[0];
    }


    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0)
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof SubstrateViewPart )
            ( (SubstrateViewPart)component ).update();
    }
    
    public static class SubstrateViewPart extends PhysicellTab
    {
        public SubstrateViewPart(MulticellEModel emodel)
        {
            super( emodel );
        }

        @Override
        protected RowModel getRowModel()
        {
            return new ListRowModel( emodel.getSubstrates(), SubstrateProperties.class );
        }

        @Override
        protected Object createTemplate()
        {
            return new SubstrateProperties( "" );
        }
    }
    
    public class CellDefinitionsViewPart extends PhysicellTab
    {
        public CellDefinitionsViewPart(MulticellEModel emodel)
        {
            super( emodel );
        }

        @Override
        protected RowModel getRowModel()
        {
            return new ListRowModel( emodel.getCellDefinitions(), CellDefinitionProperties.class );
        }

        @Override
        protected Object createTemplate()
        {
            return new CellDefinitionProperties( "" );
        }
    }
}