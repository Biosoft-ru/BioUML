package biouml.standard.diagram;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.SemanticRelation;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.IdGenerator;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;

public class SemanticRelationPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(SemanticRelationPane.class.getName());

    protected DiagramElementSelector inSelector;
    protected DiagramElementSelector outSelector;
    protected Base relation;

    protected Module module;
    protected ViewEditorPane viewEditor;
    protected ViewPaneAdapter adapter;
    protected JButton okButton;
    protected DataCollection relationDC;

    public SemanticRelationPane(Module module, ViewEditorPane viewEditor)
    {
        super(new GridBagLayout());
        this.module = module;
        this.viewEditor = viewEditor;
        viewEditor.setSelectionEnabled(false);

        setBorder(new EmptyBorder(10, 10, 10, 10));

        ResourceBundle resources = ResourceBundle.getBundle(MessageBundle.class.getName());
        inSelector = new DiagramElementSelector(resources.getString("RELATION_IN"));
        outSelector = new DiagramElementSelector(resources.getString("RELATION_OUT"));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(inSelector.getRadioButton());
        buttonGroup.add(outSelector.getRadioButton());
        inSelector.getRadioButton().setSelected(true);

        add(inSelector, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,
                0, 5, 0), 0, 0));
        add(outSelector, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,
                0, 5, 0), 0, 0));

        add(new JLabel(resources.getString("RELATION_DESCRIPTION")), new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(30, 0, 0, 0), 0, 0));

        adapter = ( new ViewPaneAdapter()
        {
            @Override
            public void mousePressed(ViewPaneEvent e)
            {
                Object model = e.getViewSource().getModel();
                if( model instanceof DiagramElement )
                {
                    if( inSelector.isSelected() )
                    {
                        inSelector.setDiagramElement((DiagramElement)model);
                        outSelector.getRadioButton().setSelected(true);
                    }
                    else
                    {
                        outSelector.setDiagramElement((DiagramElement)model);
                    }

                    if( inSelector.getDiagramElement() != null && outSelector.getDiagramElement() != null )
                    {
                        boolean status = createRelation();
                        if( okButton != null )
                            okButton.setEnabled(status);
                    }
                }
            }
        } );

        viewEditor.addViewPaneListener(adapter);
    }

    protected boolean createRelation()
    {
        try
        {
            DataElement in = inSelector.getDiagramElement().getKernel();
            DataElement out = outSelector.getDiagramElement().getKernel();

            String inElementName = DataElementPath.EMPTY_PATH.getChildPath(in.getOrigin().getName(), in.getName()).toString();
            String outElementName = DataElementPath.EMPTY_PATH.getChildPath(out.getOrigin().getName(), out.getName()).toString();
            String relationName = inElementName + " -> " + outElementName;

            relationDC = module.getCategory(SemanticRelation.class);
            String id = relationName;

            // try to genearte ID using specified in DC info
            String formatter = relationDC.getInfo().getProperty(DataCollectionConfigConstants.ID_FORMAT);
            if( formatter != null )
                id = IdGenerator.generateUniqueName(relationDC, new DecimalFormat(formatter));

            SemanticRelation sr = new SemanticRelation(relationDC, id, " ");
            sr.setTitle(" ");
            sr.setInputElementName(inElementName);
            sr.setOutputElementName(outElementName);
            sr.setParticipation("");

            relation = sr;
            return true;
        }
        catch( Throwable t )
        {
            JOptionPane.showMessageDialog(this, "Exception: " + t, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Creates Edge for the SemanticRelation and adds it on the diagram.
     */
    protected void createEdge()
    {
        try
        {
            //try to find relation with the same input and output in relation data collection
            boolean useOldKernel = false;
            if( relationDC != null )
            {
                SemanticRelation sr = findRelation();
                //if found relation ask user to use old version
                if( sr != null )
                {
                    int result = JOptionPane.showConfirmDialog(this,
                            "Relation with the same input and output already exists. Would you like to use old relation?");
                    if( result == JOptionPane.OK_OPTION )
                    {
                        useOldKernel = true;
                        relation = sr;
                    }
                }
            }
            if( !useOldKernel )
            {
                CollectionFactoryUtils.save(relation);
            }

            Node inNode = (Node)inSelector.getDiagramElement();
            Node outNode = (Node)outSelector.getDiagramElement();

            Edge edge = new Edge(relation, inNode, outNode);

            viewEditor.add(edge, new Point(0, 0));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create Edge for relation '" + relation.getName() + "'", t);
        }
    }

    public void release()
    {
        viewEditor.setSelectionEnabled(true);
        viewEditor.removeViewPaneListener(adapter);
    }

    public void okPressed()
    {
        createEdge();
        release();
    }

    protected void cancelPressed()
    {
        release();
    }

    private SemanticRelation findRelation() throws Exception
    {
        SemanticRelation sr = null;

        /* it very difficult operation, temporary locked
        
        SemanticRelation typedRelation = null;
        if( relation instanceof SemanticRelation )
        {
            typedRelation = (SemanticRelation)relation;
        }
        else
            return null;

        //try to find with indexes
        DataCollection module = relationDC;
        while( ! ( module instanceof Module ) && module.getOrigin() != null )
        {
            module = module.getOrigin();
        }
        if( module instanceof Module && module.getInfo() != null )
        {
            QuerySystem qs = module.getInfo().getQuerySystem();
            if( qs instanceof LuceneQuerySystem )
            {
                LuceneQuerySystem lqs = (LuceneQuerySystem)qs;
                if( lqs.testHaveIndex() )
                {
                    DynamicPropertySet dps[] = lqs.search(Module.DATA + "/" + relationDC.getName(), typedRelation.getInputElementName()
                            + " " + typedRelation.getOutputElementName(), new String[] {"inputElementName", "outputElementName"}, null,
                            true);
                    if( dps != null )
                    {
                        for( int i = 0; i < dps.length; i++ )
                        {
                            String name = (String)dps[i].getValue("Name");
                            SemanticRelation current = (SemanticRelation)relationDC.get(name);
                            if( current.getInputElementName().equals(typedRelation.getInputElementName())
                                    && current.getOutputElementName().equals(typedRelation.getOutputElementName()) )
                            {
                                sr = current;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if( sr == null )
        {
            //find relation by looking all relations
            Iterator iter = relationDC.getNameList().iterator();
            while( iter.hasNext() )
            {
                SemanticRelation current = (SemanticRelation)relationDC.get((String)iter.next());
                if( current.getInputElementName().equals(typedRelation.getInputElementName())
                        && current.getOutputElementName().equals(typedRelation.getOutputElementName()) )
                {
                    sr = current;
                    break;
                }
            }
        }
         */
        return sr;
    }
}
