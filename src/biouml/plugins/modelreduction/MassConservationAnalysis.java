package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.standard.diagram.CompositeDiagramType;

/**
 * Computation of the conservation laws is based on the Gauss-Jordan method
 * {@link Util.GaussJordanElimination}
 * 
 * @author helenka
 *
 */
public class MassConservationAnalysis extends AnalysisMethodSupport<MassConservationAnalysisParameters>
{
    public MassConservationAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new MassConservationAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        if( parameters.getInput() == null || parameters.getInput().optDataElement() == null )
            throw new IllegalArgumentException("Please specify diagram path.");

        if( parameters.getOutput() == null || ! ( parameters.getOutput().optParentCollection() instanceof FolderCollection ) )
            throw new IllegalArgumentException("Please specify the collection to save results of the analysis.");
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = parameters.getInput().getDataElement(Diagram.class);

        if( diagram.getRole() instanceof EModel )
        {
            if( diagram.getType() instanceof CompositeDiagramType )
                throw new IllegalArgumentException("The analysis can not be applied to the composite diagrams.");

            List<TableDataCollection> results = new ArrayList<>();

            DataCollection<?> origin = DataCollectionUtils.createSubCollection(parameters.getOutput());

            TableDataCollection stoichiometry = StoichiometricAnalysis.getStoichiometricMatrix(diagram);
            if( stoichiometry != null )
            {
                StoichiometricMatrixDecomposition smd = new StoichiometricMatrixDecomposition(stoichiometry);

                EModel emodel = diagram.getRole(EModel.class);
                resolver = emodel.getVariableResolver(2);
                resolvedVariables = new HashMap<>();

                results.add(StoichiometricAnalysis.getStoichiometricMatrixToPut(origin, "Stoichiometry", diagram, stoichiometry));
                results.add(getReducedStoichiometryToPut(origin, "Reduced stoichiometry", smd));
                results.add(getLinkMatrixToPut(origin, "Link matrix", smd));
                results.add(getConservationLawsToPut(origin, "Conservation laws", smd, emodel));
                return saveResults(origin, results);
            }
        }
        return null;
    }

    private @Nullable TableDataCollection getReducedStoichiometryToPut(@Nonnull DataCollection<?> origin, @Nonnull String name, StoichiometricMatrixDecomposition smd)
            throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, name);

        TableDataCollection stoichiometry = smd.getSourceStoichiometry();
        List<String> independentSpecies = smd.getIndependentSpecies();
        double[][] matrix = smd.getReducedStoichiometry();

        if( independentSpecies != null && matrix != null )
        {
            for( TableColumn column : stoichiometry.getColumnModel() )
            {
                tdc.getColumnModel().addColumn(column.getName(), column.getDisplayName(), column.getShortDescription(), column.getValueClass(),
                        column.getExpression());
            }

            for( int i = 0; i < independentSpecies.size(); ++i )
            {
                int[] values = new int[matrix[i].length];
                for( int j = 0; j < matrix[i].length; ++j )
                {
                    values[j] = (int)matrix[i][j];
                }

                String title = resolveVariable(independentSpecies.get(i));
                TableDataCollectionUtils.addRow(tdc, title, ArrayUtils.toObject(values));
            }
        }
        return tdc;
    }

    private @Nullable TableDataCollection getLinkMatrixToPut(@Nonnull DataCollection<?> origin, @Nonnull String name, StoichiometricMatrixDecomposition smd)
            throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, name);

        TableDataCollection stoichiometry = smd.getSourceStoichiometry();
        List<String> independentSpecies = smd.getIndependentSpecies();
        double[][] matrix = smd.getLinkMatrix();

        if( independentSpecies != null && matrix != null )
        {
            for( String species : independentSpecies )
            {
                tdc.getColumnModel().addColumn(species, resolveVariable(species), "", Double.class, "");
            }

            for( int i = 0; i < stoichiometry.getSize(); ++i )
            {
                String species = stoichiometry.getName(i);
                TableDataCollectionUtils.addRow(tdc, resolveVariable(species), ArrayUtils.toObject(matrix[i]));
            }
        }
        return tdc;
    }

    private TableDataCollection getConservationLawsToPut(@Nonnull DataCollection<?> origin, @Nonnull String name, StoichiometricMatrixDecomposition smd,
            EModel emodel) throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, name);

        TableDataCollection stoichiometry = smd.getSourceStoichiometry();
        List<String> dependentSpecies = smd.getDependentSpecies();
        List<String> independentSpecies = smd.getIndependentSpecies();
        double[][] linkMatrix = smd.getLinkMatrix();

        if( dependentSpecies != null && linkMatrix != null )
        {
            tdc.getColumnModel().addColumn("Dependent species", String.class);
            tdc.getColumnModel().addColumn("Expressions", String.class);
            tdc.getColumnModel().addColumn("Conserved amounts", Double.class);

            int id = 0;

            for( int i = 0; i < stoichiometry.getSize(); ++i )
            {
                String species = stoichiometry.getName(i);
                if( dependentSpecies.contains(species) )
                {
                    StringBuilder expression = new StringBuilder(resolveVariable(species));
                    double amount = emodel.getVariable(species).getInitialValue();

                    for( int j = 0; j < independentSpecies.size(); ++j )
                    {
                        if( linkMatrix[i][j] != 0.0 )
                        {
                            String title = resolveVariable(independentSpecies.get(j));
                            expression.append(generateTerm(title, linkMatrix[i][j]));
                            amount -= linkMatrix[i][j] * emodel.getVariable(independentSpecies.get(j)).getInitialValue();
                        }
                    }

                    String title = resolveVariable(species);
                    TableDataCollectionUtils.addRow(tdc, Integer.toString(++id), new Object[] {title, expression.toString(), amount});
                }
            }
        }
        return tdc;
    }

    private String generateTerm(String variable, double factor)
    {
        String str = " ";
        str += ( factor < 0 ) ? "+ " : "- ";
        if( Math.abs(factor) != 1.0 )
        {
            str += Math.abs(factor) + " * ";
        }
        str += variable;
        return str;
    }

    private VariableResolver resolver;
    private Map<String, String> resolvedVariables;

    private String resolveVariable(String variable)
    {
        if( resolver == null )
            return variable;

        if( resolvedVariables.containsKey(variable) )
            return resolvedVariables.get(variable);

        String resolved = resolver.resolveVariable(variable);
        resolvedVariables.put(variable, resolved);

        return resolved;
    }

    private TableDataCollection[] saveResults(DataCollection origin, List<TableDataCollection> results)
    {
        return StreamEx.of(results).nonNull().peek( origin::put ).toArray( TableDataCollection[]::new );
    }
}
