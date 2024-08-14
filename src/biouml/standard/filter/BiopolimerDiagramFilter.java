package biouml.standard.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import biouml.model.AbstractFilter;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.EquivalentNodeGroup;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Biopolymer;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.application.PanelInfo;
import com.developmentontheedge.application.PanelManager;

/**
 * Common purpose diagram filter allowing to filter {@link Biopolimer}s
 * by species, cell types and inducers.
 *
 * @pending whether CompositeHighlighActions can contain several HighlightActions with the same color
 * or they should be joined into one?
 *
 * @pending whether we should change EquivalentNodeGroup title (specie list) like GeneNet
 * if we hide some object from the group?
 */
public class BiopolimerDiagramFilter extends AbstractFilter
{
    /** The {@link Diagram} to which the filter can be applied. */
    Diagram diagram;

    private final List<String> species = new ArrayList<>();
    private final List<String> cellTypes = new ArrayList<>();
    private final List<String> inducers = new ArrayList<>();

    PanelManager control;

    public BiopolimerDiagramFilter(Diagram diagram)
    {
        this.diagram = diagram;

        init();
    }

    private void init()
    {
        long start = System.currentTimeMillis();
        collectDiagramInfo(diagram);

        // initialize filters
        if( speciesFilter == null || cellTypeFilter == null || inducerFilter == null )
        {
            speciesFilter  = new SpeciesFilter ( this, getDiagramSpeciesList() );
            cellTypeFilter = new CellTypeFilter( this, getDiagramCellTypesList() );
            inducerFilter  = new InducerFilter ( this, getDiagramInducersList() );
        }

        System.out.println("Load filter data, time=" + (System.currentTimeMillis()-start) );
    }

    /**
     * This method is used only during deserialization
     */
    public BiopolimerDiagramFilter()
    {
    }
    @Override
    public void setDiagram(Diagram diagram)
    {
        if( this.diagram != null )
            return;
        this.diagram = diagram;
        init();
    }

    @Override
    public boolean isEnabled()
    {
        return speciesFilter.isEnabled() || cellTypeFilter.isEnabled() || inducerFilter.isEnabled();
    }

    /** returns visual control to set up the filter options. */
    @Override
    public java.awt.Component getOptionsControl()
    {
        if( control == null )
        {
            control = new PanelManager();

            addFilterControl(speciesFilter,  "speciesFilter",  null);
            addFilterControl(cellTypeFilter, "cellTypeFilter", "speciesFilter");
            addFilterControl(inducerFilter,  "inducerFilter",  "cellTypeFilter");
        }

        return control;
    }

    protected void addFilterControl(ValueActionFilter filter, String name, String groupWith)
    {
        PropertyInspector filterControl = new PropertyInspector();
        filterControl.explore(filter);
        filterControl.setRootVisible(false);
        filterControl.expandAll(true);

        PanelInfo filterInfo = new PanelInfo(name, filterControl, true, null);
        control.addPanel(filterInfo, groupWith, PanelInfo.RIGHT, 230);
    }


    @Override
    protected void processNode(Node node)
    {
        if( node instanceof EquivalentNodeGroup )
            processGroup((EquivalentNodeGroup)node);
        else
        {
            Action action = getNodeAction(node);
            if( action != null )
                action.apply(node);
        }
    }

    protected Action getNodeAction(Node node)
    {
        Base kernel= node.getKernel();
        if( !(kernel instanceof Biopolymer) )
            return null;

        Action action = null;

        // filter by species
        if( speciesFilter.isEnabled() )
            action = speciesFilter.getAction(node);

        // filter by cell types
        if( action != HideAction.instance && cellTypeFilter.isEnabled() )
            action = mergeActions(action, cellTypeFilter.getAction(node));

        // filter by regulation
        if( action != HideAction.instance && inducerFilter.isEnabled() )
            action = mergeActions(action, inducerFilter.getAction(node));

        return action;
    }

    protected void processGroup(EquivalentNodeGroup group)
    {
        List<Action> actions = group.stream( Node.class ).map( this::getNodeAction ).nonNull().toList();

        // if nothing
        if( actions.isEmpty() )
            return;

        // if all actions are hidden - then hide the node
        if( actions.size() == group.getSize() )
        {
            if(actions.stream().allMatch( HideAction.instance::equals ))
            {
                HideAction.instance.apply(group);
                return;
            }
        }

        // create composite highlight action
        CompositeHighlightAction highlight = new CompositeHighlightAction();
        for(Action act : actions)
        {
            if( act instanceof HighlightAction )
                highlight.add((HighlightAction)act);
            else if( act instanceof CompositeHighlightAction )
                highlight.add((CompositeHighlightAction)act);
        }
        highlight.apply(group);
    }

    protected Action mergeActions(Action action1, Action action2)
    {
        if( action1 == HideAction.instance || action2 == HideAction.instance )
            return HideAction.instance;

        if( action1 == null )
            return action2;

        if( action2 == null )
            return action1;

        // here should be highlight actions
        return new CompositeHighlightAction(action1, action2);
    }

    @Override
    protected void restoreView(DiagramElement de)
    {
        CompositeView view = (CompositeView)de.getView();

        // restore view if we hide it
        de.getView().setVisible(true);

        // check and remove highlighter
        View highlighter = view.elementAt(0);
        if(highlighter.getModel() instanceof Action)
            view.remove(highlighter);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utilities
    //

    /**
     * Build list of all species in the diagram.
     *
     * @pending situation if species field is composite and contains list of species
     */
    protected void collectDiagramInfo(Compartment comp)
    {
        for(Node de : comp.stream( Node.class ))
        {
            if(de instanceof Compartment)
            {
                collectDiagramInfo( (Compartment)de );
                continue;
            }

            Object kernel = de.getKernel();
            if( !(kernel instanceof Biopolymer) )
                continue;
            Biopolymer bp = (Biopolymer)kernel;

            // add species
            String str = bp.getSpecies();
            if( str != null)
            {
                str = normaliseSpecies(str);

                if( !species.contains(str) )
                    species.add(str);
            }

            // add cell types
            String cellTypeField = bp.getSource();
            if(cellTypeField != null)
            {
                StringTokenizer cellTypeTokenizer = new StringTokenizer(cellTypeField, ";\r\n");
                while(cellTypeTokenizer.hasMoreTokens())
                {
                    String cellType = cellTypeTokenizer.nextToken().trim();
                    if( !cellTypes.contains(cellType) )
                        cellTypes.add(cellType);
                }
            }

            // add inducers
            String inducerField = bp.getRegulation();
            if(inducerField != null)
            {
                StringTokenizer inducerTokenizer = new StringTokenizer(inducerField, ";\r\n");
                while(inducerTokenizer.hasMoreTokens())
                {
                    String inducer = inducerTokenizer.nextToken().trim();
                    if( !inducers.contains(inducer) )
                        inducers.add(inducer);
                }
            }
        } // end of while

        Collections.sort(species);
        Collections.sort(cellTypes);
        Collections.sort(inducers);
    }

    public String[] getDiagramSpeciesList()
    {
        return species.toArray(new String[species.size()]);
    }

    public String[] getDiagramCellTypesList()
    {
        return cellTypes.toArray(new String[cellTypes.size()]);
    }

    public String[] getDiagramInducersList()
    {
        return inducers.toArray(new String[inducers.size()]);
    }

    protected static String normaliseSpecies(String species)
    {
        int offset = species.indexOf('(');
        if(offset > 0)
            species = species.substring(0, offset-1);

        return species;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Species filter
    //

    SpeciesFilter speciesFilter;
    public SpeciesFilter getSpeciesFilter()
    {
        return speciesFilter;
    }

    /** This methods used only to allow child properties to be modified in PropertyInspector. */
    public void setSpeciesFilter(SpeciesFilter filter)
    {
        SpeciesFilter oldValue = speciesFilter;
        speciesFilter = filter;
        firePropertyChange("speciesFilter", oldValue, speciesFilter);
    }

    public static class SpeciesFilter extends ValueActionFilter
    {
        public SpeciesFilter(Option filter, String[] values)
        {
            super(filter, values, false);
        }

        @Override
        public String getValue(DataElement de)
        {
            String species = null;
            if(de instanceof Biopolymer)
            {
                species = ((Biopolymer)de).getSpecies();
                if(species != null)
                    species = normaliseSpecies(species);
            }

            return species;
        }
    }

    public static class SpeciesFilterBeanInfo extends ValueActionFilterBeanInfo
    {
        public SpeciesFilterBeanInfo()
        {
            super(SpeciesFilter.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties("SPECIES_FILTER");
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    // CellType filter
    //

    CellTypeFilter cellTypeFilter;
    public CellTypeFilter getCellTypeFilter()
    {
        return cellTypeFilter;
    }

    /** This methods used only to allow child properties to be modified in PropertyInspector. */
    public void setCellTypeFilter(CellTypeFilter filter)
    {
        CellTypeFilter oldValue = cellTypeFilter;
        cellTypeFilter = filter;
        firePropertyChange("cellTypeFilter", oldValue, cellTypeFilter);
    }

    public static class CellTypeFilter extends ValueActionFilter
    {
        public CellTypeFilter(Option filter, String[] values)
        {
            super(filter, values, true);
        }

        @Override
        public String getValue(DataElement de)
        {
            if(de instanceof Biopolymer)
                return ((Biopolymer)de).getSource();

            return null;
        }
    }

    public static class CellTypeFilterBeanInfo extends ValueActionFilterBeanInfo
    {
        public CellTypeFilterBeanInfo()
        {
            super(CellTypeFilter.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties("CELL_TYPE_FILTER");
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    // Inducer filter
    //

    InducerFilter inducerFilter;
    public InducerFilter getInducerFilter()
    {
        return inducerFilter;
    }

    /** This methods used only to allow child properties to be modified in PropertyInspector. */
    public void setInducerFilter(InducerFilter filter)
    {
        InducerFilter oldValue = inducerFilter;
        inducerFilter = filter;
        firePropertyChange("inducerFilter", oldValue, inducerFilter);
    }

    public static class InducerFilter extends ValueActionFilter
    {
        public InducerFilter(Option filter, String[] values)
        {
            super(filter, values, true);
        }

        @Override
        public String getValue(DataElement de)
        {
            if(de instanceof Biopolymer)
                return ((Biopolymer)de).getRegulation();

            return null;
        }
    }

    public static class InducerFilterBeanInfo extends ValueActionFilterBeanInfo
    {
        public InducerFilterBeanInfo()
        {
            super(InducerFilter.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties("INDUCER_FILTER");
        }
    }
}
