package biouml.plugins.optimization.document.editors;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.document.OptimizationDocument;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.action.ActionInitializer;

import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

@SuppressWarnings ( "serial" )
public class DiagramParametersViewPart extends ViewPartSupport
{
    protected static final Logger log = Logger.getLogger(DiagramParametersViewPart.class.getName());

    public static final String ADD = "add parameters to the fitting set";
    public static final String REMOVE = "remove parameters from the fitting set";

    protected Action addAction = new AddAction(ADD);
    protected Action removeAction = new RemoveAction(REMOVE);

    protected JTabbedPane tabbedPane;

    public DiagramParametersViewPart()
    {
        tabbedPane = new JTabbedPane(SwingConstants.TOP);
        add(BorderLayout.CENTER, tabbedPane);
    }

    private void initTabbedPane(Diagram diagram)
    {
        tabbedPane.removeAll();

        if( diagram != null )
        {
            EModel emodel = (EModel)diagram.getRole();
            SubdiagramParametersTab paramsTab = new SubdiagramParametersTab(emodel, getParametersIterator(emodel),
                    ( (Optimization)model ).getParameters());
            tabbedPane.addTab(diagram.getName(), paramsTab);

            addTabs(diagram, tabbedPane);

            tabbedPane.setSelectedIndex(0);
        }
    }

    protected void addTabs(Diagram diagram, JTabbedPane tabbedPane)
    {
        if( diagram.getType() instanceof CompositeDiagramType )
        {
            for( SubDiagram de : diagram.stream( SubDiagram.class ) )
            {
                Diagram innerDiagram = de.getDiagram();
                if (innerDiagram == null || !(innerDiagram.getRole() instanceof EModel))
                    continue;
                
                EModel emodel = (EModel)innerDiagram.getRole();
                SubdiagramParametersTab paramsTab = new SubdiagramParametersTab(emodel, getParametersIterator(emodel),
                        ( (Optimization)model ).getParameters());
                tabbedPane.addTab(Util.getPath(de), paramsTab);

                addTabs(innerDiagram, tabbedPane);
            }
        }
    }

    protected Iterator<? extends Variable> getParametersIterator(EModel emodel)
    {
        return emodel.getParameters().iterator();
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        if( model instanceof Optimization )
            initTabbedPane( ( (Optimization)model ).getDiagram());
    }

    @Override
    public boolean canExplore(Object model)
    {
        return ( model instanceof Optimization );
    }

    public List<Parameter> getFittingParameters()
    {
        return ( (Optimization)model ).getParameters().getFittingParameters();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Actions
    //
    @Override
    public Action[] getActions()
    {
        ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
        initializer.initAction(addAction, ADD);
        initializer.initAction(removeAction, REMOVE);

        Action[] action = new Action[] {addAction, removeAction};

        return action;
    }

    class AddAction extends AbstractAction
    {
        public AddAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            SubdiagramParametersTab tab = (SubdiagramParametersTab)tabbedPane.getSelectedComponent();
            Variable[] selectedParams = tab.getSelectedParameters();
            if( selectedParams != null )
            {
                boolean isChanged = false;
                List<Parameter> newValue = copy(getFittingParameters());

                String diagramPath = "";
                Diagram de = tab.getEModel().getParent();
                SubDiagram subde = SubDiagram.getParentSubDiagram(de);
                if(subde != null)
                    diagramPath = Util.getPath(subde);

                for( Variable selectedParam : selectedParams )
                {
                    String paramName = DiagramUtility.generatPath(diagramPath, selectedParam.getName());
                    if( !contains(newValue, paramName) )
                    {
                        double val = selectedParam.getInitialValue();
                        Parameter param = new Parameter(paramName, val);

                        if( selectedParam instanceof VariableRole )
                        {
                            String title = ( (VariableRole)selectedParam ).getDiagramElement().getTitle();
                            param.setTitle(title);
                        }
                        else
                        {
                            param.setTitle(selectedParam.getName());
                        }

                        param.setUnits(selectedParam.getUnits());
                        Diagram diagram = tab.getEModel().getParent();
                        param.setParentDiagramName(diagram.getName());
                        param.addPropertyChangeListener((OptimizationDocument)document);

                        newValue.add(param);
                        isChanged = true;
                    }
                    else
                    {
                        log.warning(MessageBundle.format("WARN_NEW_ITEM_ADDING_TO_FITTING_SET", new Object[] {paramName}));
                    }
                }

                if( isChanged )
                    refreshDocument(newValue);
            }
        }
    }
    
    protected boolean contains(List<Parameter> parameters, String paramName)
    {
        for(int i = 0; i < parameters.size(); ++i)
        {
            if(parameters.get(i).getName().equals(paramName))
                return true;
        }
        return false;
    }

    class RemoveAction extends AbstractAction
    {
        public RemoveAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            TabularPropertyInspector editor = ((OptimizationDocument)document).getTabularPropertyInspector();
            int[] rowNumbers = editor.getTable().getSelectedRows();
            if( rowNumbers.length > 0 )
            {
                List<Parameter> newValue = copy(getFittingParameters());
                for( int i = rowNumbers.length - 1; i >= 0; i-- )
                {
                    newValue.remove(rowNumbers[i]);
                }
                refreshDocument(newValue);
            }
            else
            {
                JOptionPane.showMessageDialog(null, MessageBundle.getMessage("WARN_PARAMETER_SELECTION"));
            }
        }
    }

    private List<Parameter> copy(List<Parameter> list)
    {
        ArrayList<Parameter> copyList = new ArrayList<>();
        for( int j = 0; j < list.size(); ++j )
            copyList.add(list.get(j));
        return copyList;
    }

    private void refreshDocument(List<Parameter> newValue)
    {
        ( (Optimization)model ).getParameters().setFittingParameters(newValue);
        ( (OptimizationDocument)document ).fireValueChanged(new EventObject(this));
    }
}
