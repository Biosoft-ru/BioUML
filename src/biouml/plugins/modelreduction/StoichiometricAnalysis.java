package biouml.plugins.modelreduction;

import org.apache.commons.lang.ArrayUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;

public class StoichiometricAnalysis extends AnalysisMethodSupport<StoichiometricAnalysisParameters>
{
    public StoichiometricAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new StoichiometricAnalysisParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = parameters.getInput().getDataElement(Diagram.class);
        TableDataCollection tdc = getStoichiometricMatrix(diagram);

        DataElementPath path = parameters.getOutput();
        DataCollection<?> origin = path.getParentCollection();
        String name = path.getName();

        path.remove();
        TableDataCollection toPut = getStoichiometricMatrixToPut(origin, name, diagram, tdc);
        path.save(toPut);
        return toPut;
    }

    public static TableDataCollection getStoichiometricMatrix(Diagram diagram) throws IllegalArgumentException
    {
        if( diagram != null && diagram.getRole() instanceof EModel )
        {
            if( diagram.getType() instanceof CompositeDiagramType )
                throw new IllegalArgumentException("The analysis can not be applied to the composite diagrams.");

            TableDataCollection tdc = new StandardTableDataCollection(null, "");
            fillTableColumns(diagram, diagram, tdc);
            if( tdc.getColumnModel().getColumnCount() > 0 )
            {
                fillStoichiometry(diagram, tdc);
            }
            return tdc;
        }
        return null;
    }

    public static TableDataCollection getStoichiometricMatrixToPut(DataCollection<?> origin, String name, Diagram diagram,
            TableDataCollection tdc) throws Exception
    {
        if( origin != null )
        {
            TableDataCollection st = TableDataCollectionUtils.createTableDataCollection(origin, name);

            if( tdc != null )
            {
                tdc.columns().forEach( st.getColumnModel()::addColumn );

                VariableResolver resolver = diagram.getRole( EModel.class ).getVariableResolver(2);

                for( int i = 0; i < tdc.getSize(); ++i )
                {
                    RowDataElement rde = tdc.getAt(i);
                    Object[] values = rde.getValues();

                    String title = resolver.resolveVariable(rde.getName());
                    TableDataCollectionUtils.addRow(st, title, values);
                }

                return st;
            }
        }
        return null;
    }

    private static void fillTableColumns(Diagram diagram, Compartment compartment, TableDataCollection tdc)
    {
        for(DiagramElement de : compartment)
        {
            if( de.getKernel() instanceof Reaction )
            {
                Reaction reaction = (Reaction)de.getKernel();
                String description = AnalysisUtils.printReaction(diagram, reaction);
                tdc.getColumnModel().addColumn(reaction.getName(), de.getTitle(), description, Integer.class, "");
            }
            else if( de.getKernel() instanceof biouml.standard.type.Compartment )
            {
                fillTableColumns(diagram, (Compartment)de, tdc);
            }
        }
    }

    private static void fillStoichiometry(Diagram diagram, TableDataCollection tdc)
    {
        EModel emodel = diagram.getRole( EModel.class );
        for(VariableRole var : emodel.getVariableRoles())
        {
            DiagramElement de = var.getDiagramElement();

            if( ! ( de.getKernel().getType().equals(Type.TYPE_COMPARTMENT) ) )
            {

                int[] values = new int[tdc.getColumnModel().getColumnCount()];

                for( Edge edge : ( (Node)de ).getEdges() )
                {
                    Reaction reaction = edge.nodes().map( Node::getKernel ).select( Reaction.class ).findFirst().orElse( null );

                    if( reaction != null )
                    {
                        int ind = tdc.getColumnModel().getColumnIndex(reaction.getName());
                        SpecieReference sp = (SpecieReference)edge.getKernel();
                        int val = Integer.parseInt(sp.getStoichiometry());

                        if( sp.getRole().equals(SpecieReference.PRODUCT) )
                        {
                            values[ind] = val;
                        }
                        else if( sp.getRole().equals(SpecieReference.REACTANT) )
                        {
                            values[ind] = -val;
                        }
                    }
                }

                TableDataCollectionUtils.addRow(tdc, var.getName(), ArrayUtils.toObject(values));
            }
        }
    }
}
