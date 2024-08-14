package biouml.standard.diagram;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.standard.type.Base;
import biouml.standard.type.Compartment;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;

/**
 * PathwaySimulationDiagramType is extension of PathwayDiagramType.
 * It defines three new node types used for model simulations:
 * <ul>
 *   <li> {@link Event} </li>
 *   <li> {@link Equation} </li>
 *   <li> {@link Function} </li>
 * </ul>
 */
public class PathwaySimulationDiagramType extends PathwayDiagramType
{

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.setRole(new EModel(diagram));

        PathwaySimulationDiagramType.DiagramPropertyChangeListener listener = new PathwaySimulationDiagramType.DiagramPropertyChangeListener(
                diagram);
        diagram.getViewOptions().addPropertyChangeListener(listener);

        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Compartment.class, Type.TYPE_BLOCK, Gene.class, RNA.class, Protein.class, Substance.class, Reaction.class,
                Stub.Note.class, Stub.OutputConnectionPort.class, Stub.InputConnectionPort.class, Stub.ContactConnectionPort.class,
                Event.class, Equation.class, Function.class, Constraint.class, State.class, Type.TYPE_TABLE};
    }

    @Override
    public Class[] getEdgeTypes()
    {
        return new Class[] {Stub.NoteLink.class, Transition.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new PathwaySimulationDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new PathwaySimulationSemanticController();

        return semanticController;
    }

    /**
     * listener for switching "dependencyEdges" view options
     *
     */
    public static class DiagramPropertyChangeListener implements PropertyChangeListener, DataCollectionListener
    {
        protected Logger log = Logger.getLogger(DiagramPropertyChangeListener.class.getName());

        private Diagram diagram;
        private DiagramPropertyChangeListener listenerOnDiagram;
        public DiagramPropertyChangeListener(Diagram diagram)
        {
            this.diagram = diagram;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            Object source = evt.getSource();
            if( source instanceof DiagramViewOptions && evt.getPropertyName().equals("dependencyEdges") )
            {
                if( (Boolean)evt.getNewValue() )
                {
                    try
                    {
                        MathDiagramUtility.generateDependencies(diagram);
                        DiagramPropertyChangeListener listener = new DiagramPropertyChangeListener(diagram);
                        diagram.addPropertyChangeListener(listener);
                        diagram.addDataCollectionListener(listener);
                        listenerOnDiagram = listener;
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Can't generate dependency edges");
                    }
                }
                else
                {
                    try
                    {
                        diagram.removeDataCollectionListener(listenerOnDiagram);
                        diagram.removePropertyChangeListener(listenerOnDiagram);
                        boolean notificationEnabled = diagram.isNotificationEnabled();
                        diagram.setNotificationEnabled(false);
                        MathDiagramUtility.removeDependencies(diagram);
                        diagram.setNotificationEnabled(notificationEnabled);
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Can't remove dependency edges");
                    }
                }
            }
            if( evt.getSource() instanceof Equation && ( (Equation)evt.getSource() ).getDiagramElement() instanceof Node )
            {
                if( evt.getPropertyName().equals("formula") || evt.getPropertyName().equals("variable") )
                    MathDiagramUtility.generateDependenciesForEquation((Node) ( (Equation)evt.getSource() ).getDiagramElement());
            }
        }

        @Override
        public void elementAdded(DataCollectionEvent e) throws Exception
        {
            DiagramElement de = (DiagramElement)e.getDataElement();
            if( de instanceof Node && ( Util.isPort( de ) ) )
            {
                MathDiagramUtility.generateDependencies(Diagram.getDiagram(de));
            }

        }

        @Override
        public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
        }

        @Override
        public void elementChanged(DataCollectionEvent e) throws Exception
        {
        }

        @Override
        public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
        }

        @Override
        public void elementRemoved(DataCollectionEvent e) throws Exception
        {
        }

        @Override
        public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
        }
    }
    
    
    @Override
    public boolean isGeneralPurpose()
    {
        return false;
    }
}
