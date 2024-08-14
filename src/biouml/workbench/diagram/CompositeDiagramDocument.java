package biouml.workbench.diagram;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JSplitPane;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramFilter;
import biouml.model.DiagramViewBuilder;
import biouml.model.SubDiagram;
import biouml.standard.state.StateUndoManager;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.DefaultViewEditorHelper;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.gui.DocumentTransactionUndoManager;
import ru.biosoft.gui.SaveDocumentAction;

public class CompositeDiagramDocument extends DiagramDocument
{
    protected JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    protected ViewPane diagramViewPane;
    protected ViewPane subdiagramViewPane;

    protected Map<String, DiagramDocument> subdiagramDocuments = new HashMap<>();
    protected DiagramDocument currentSubdiagramDocument = null;

    protected DataCollectionListener diagramCollectionListener;

    public CompositeDiagramDocument(Diagram diagram)
    {
        super(diagram);

        initDiagramViewPane(diagram);
        initSubdiagramViewPane(null);

        splitPane.setTopComponent(diagramViewPane);
        splitPane.setDividerLocation(500);

        viewPane = new ViewEditorPane(new DefaultViewEditorHelper())
        {
            @Override
            public void scale(double sx, double sy)
            {
                diagramViewPane.scale(sx, sy);
            }
        };
        viewPane.add(splitPane);

        diagramCollectionListener = new DataCollectionListenerSupport()
        {
            @Override
            public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
            {
                if( e.getDataElement() instanceof DiagramContainer )
                {
                    subdiagramDocuments.remove( ( (DiagramContainer)e.getDataElement() ).getName()); //subdiagram name unique, encapsulated diagram name is not
                    initSubdiagramViewPane(null);
                }
            }
        };
        diagram.addDataCollectionListener(diagramCollectionListener);
    }
    
    public int getDividerLocation()
    {
        return splitPane.getDividerLocation();
    }
    
    public void setDividerLocation(int location )
    {
        splitPane.setDividerLocation(location);
    }

    protected void refreshSplitPane()
    {
        int location = splitPane.getDividerLocation();
        splitPane.setBottomComponent(subdiagramViewPane);
        splitPane.setDividerLocation(location);
    }

    private void initDiagramViewPane(Diagram diagram)
    {
        diagramViewPane = viewPane;
        diagramViewPane.addViewPaneListener(new ViewPaneAdapter()
        {
            @Override
            public void mousePressed(ViewPaneEvent e)
            {
                if( e.getViewSource() instanceof CompositeView )
                {
                    Object model = ( (CompositeView)e.getViewSource() ).getModel();

                    if (model instanceof DiagramContainer)
                    {
                        initSubdiagramViewPane(((DiagramContainer)model));
                    }
                    else
                    {
                        initSubdiagramViewPane(null);
                    }

                }
            }
        });
    }

    private void initSubdiagramViewPane(DiagramContainer subdiagram)
    {
        if( subdiagram != null )
        {
            if( subdiagramDocuments.get(subdiagram.getName()) != null )
            {
                currentSubdiagramDocument = subdiagramDocuments.get(subdiagram.getName());
                if(!subdiagram.isDiagramMutable())
                {
                    currentSubdiagramDocument.setModel(subdiagram.getDiagram());
                    currentSubdiagramDocument.doUpdate();
                    CompositeDiagramDocument.this.update();
                }
            }
            else
            {
                currentSubdiagramDocument = new DiagramDocument(subdiagram.getDiagram(), subdiagram.isDiagramMutable())
                {
                    @Override
                    public void update()
                    {
                        super.doUpdate();
                        CompositeDiagramDocument.this.update();
                    }

                    @Override
                    public void propertyChange(PropertyChangeEvent pce)
                    {
                        if( pce.getPropertyName().equals(SubDiagram.SUBDIAGRAM_IS_MUTABLE) && pce.getSource() instanceof SubDiagram)
                        {
                            SubDiagram source = (SubDiagram)pce.getSource();
                            DiagramDocument diagramDocument = subdiagramDocuments.get(source.getName());
                            if( diagramDocument != null )
                            {
                                source.getDiagram().removePropertyChangeListener(diagramDocument);
                                subdiagramDocuments.remove(source.getName());
                                initSubdiagramViewPane(source);
                            }
                        }
                        super.propertyChange(pce);
                    }
                };
                subdiagramDocuments.put(subdiagram.getName(), currentSubdiagramDocument); //subdiagram name is unique in the upper diagram
            }

            subdiagramViewPane = currentSubdiagramDocument.getViewPane();

            refreshSplitPane();
        }
        else
        {
            subdiagramViewPane = new ViewPane();
            subdiagramViewPane.removeAll();
            subdiagramViewPane.setLayout(new GridBagLayout());
            JLabel text = new JLabel("Select child diagram");
            subdiagramViewPane.add(text, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));

            currentSubdiagramDocument = null;
            refreshSplitPane();
        }
    }

    @Override
    public boolean isChanged()
    {
        for( DiagramDocument childDocumet : subdiagramDocuments.values() )
        {
            if( childDocumet.isChanged() )
                return true;
        }
        return ( !justSaved && undoManager.canUndo() );
    }

    @Override
    protected void doUpdate()
    {
        //long start = System.currentTimeMillis();
        Diagram diagram = getDiagram();
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();

        Graphics gg = Application.getApplicationFrame().getGraphics();
        gg.translate(X_VIEWPANE_OFFSET, Y_VIEWPANE_OFFSET);
        builder.createDiagramView(diagram, gg);

        DiagramFilter[] filterList = diagram.getFilterList();
        for( DiagramFilter filter : filterList )
        {
            if( filter != null && filter.isEnabled() )
                filter.apply( diagram );
        }

        CompositeView diagramView = (CompositeView)diagram.getView();
        diagramViewPane.setView(diagramView);
        //System.out.println("Diagram update, time=" + ( System.currentTimeMillis() - start ));
    }

    @Override
    public boolean canUpdate()
    {
        if( undoManager.hasTransaction() || undoManager.isUndo() || undoManager.isRedo() )
            return false;

        for( DiagramDocument childDocumet : subdiagramDocuments.values() )
        {
            DocumentTransactionUndoManager childUndoManager = childDocumet.getUndoManager();
            if( childUndoManager.hasTransaction() || childUndoManager.isUndo() || childUndoManager.isRedo() )
                return false;

            Diagram diagram = childDocumet.getDiagram();
            if( diagram.getCurrentState() != null )
            {
                StateUndoManager stUndoManager = diagram.getCurrentState().getStateUndoManager();
                if( stUndoManager.isRedo() || stUndoManager.isUndo() )
                    return false;
            }
        }

        return true;
    }

    @Override
    public void save()
    {
        List<String> changedSubdiagramNames = StreamEx.ofKeys( subdiagramDocuments, DiagramDocument::isChanged ).toList();

        if( changedSubdiagramNames.isEmpty() )
        {
            super.save();
            return;
        }

        List<String> savedSubdiagramNames = null;
        SaveCompositeDiagramDialog dialog = new SaveCompositeDiagramDialog(changedSubdiagramNames);
        if( dialog.doModal() )
        {
            savedSubdiagramNames = dialog.getSubdiagramNamesToBeSaved();
            for( String name : savedSubdiagramNames )
            {
                if( name.equals(getDiagram().getName()) )
                {
                    super.save();
                }
                else
                {
                    DiagramDocument childDocument = subdiagramDocuments.get(name);
                    if (childDocument.getDiagram().getOrigin() instanceof DiagramContainer)
                        continue;
                    childDocument.save();
                }
            }
        }

        if( savedSubdiagramNames == null || savedSubdiagramNames.size() != changedSubdiagramNames.size() )
        {
            Application.getActionManager().enableActions( true, SaveDocumentAction.KEY );
        }
    }

    @Override
    public ViewPane getDiagramViewPane()
    {
        return diagramViewPane;
    }

    public DiagramDocument getCurrentSubiagramDocument()
    {
        return currentSubdiagramDocument;
    }

    public Map<String, DiagramDocument> getSubiagramDocuments()
    {
        return subdiagramDocuments;
    }

    @Override
    public void close()
    {
        Diagram diagram = getDiagram();
        diagram.removeDataCollectionListener(diagramCollectionListener);

        diagramViewPane.removeViewPaneListener(diagramViewAccessProvider.getDocumentViewListener());
        if( diagramViewPane instanceof ViewEditorPane )
            ( (ViewEditorPane)diagramViewPane ).removeTransactionListener(undoManager);

        for( DiagramDocument childDocument : subdiagramDocuments.values() )
        {
            Diagram subdiagram = childDocument.getDiagram();
            DynamicProperty baseOriginIDProperty = subdiagram.getAttributes().getProperty("baseOriginID");
            if( baseOriginIDProperty != null )
            {
                String baseOriginID = (String)baseOriginIDProperty.getValue();
                DataCollection<?> newParent = CollectionFactory.getDataCollection(baseOriginID);
                subdiagram.setOrigin(newParent);
            }
            childDocument.close();
        }

        super.close();
    }

    @Override
    public List<DataElement> getSelectedItems()
    {
        List<DataElement> results = new ArrayList<>();
        Object[] objs = diagramViewPane.getSelectionManager().getSelectedModels();
        for (Object obj: objs)
            if (obj instanceof DataElement)
        results.add( (DataElement)obj );
        return results;
    }
}
