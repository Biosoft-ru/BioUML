package biouml.plugins.modelreduction;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import Jama.Matrix;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.MathContext;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.type.Reaction;

import ru.biosoft.util.Pair;


public class MetabolicControlAnalysis extends SteadyStateAnalysis
{
    protected static final Logger log = Logger.getLogger(MetabolicControlAnalysis.class.getName());

    public MetabolicControlAnalysis(DataCollection origin, String name) throws Exception
    {
        super(origin, name, new MetabolicControlAnalysisParameters());
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        try
        {
            parameters = (MetabolicControlAnalysisParameters)params;
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Wrong parameters");
        }
    }

    @Override
    public MetabolicControlAnalysisParameters getParameters()
    {
        return (MetabolicControlAnalysisParameters)parameters;
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        MetabolicControlAnalysisResults results = performAnalysis();
        if( results != null )
        {
            TableDataCollection stoichiometry = results.getStoichiometry();

            DataCollection origin = DataCollectionUtils.createSubCollection(getParameters().getOutput());

            List<TableDataCollection> toPut = new ArrayList<>();

            toPut.add(getElastisitiesToPut(origin, "Elastisities", results.getElasticities(), stoichiometry));
            toPut.add(getCCToPut(origin, "Concentration controls", results.getCC(), stoichiometry));
            toPut.add(getFCToPut(origin, "Flux controls", results.getFC(), stoichiometry));

            toPut.add(getElastisitiesToPut(origin, "Unscaled elastisities", results.getUnscaledElasticities(), stoichiometry));
            toPut.add(getCCToPut(origin, "Unscaled concentration controls", results.getUnscaledCC(), stoichiometry));
            toPut.add(getFCToPut(origin, "Unscaled flux controls", results.getUnscaledFC(), stoichiometry));

            return saveResults(origin, toPut);
        }
        return null;
    }

    public MetabolicControlAnalysisResults performAnalysis() throws Exception
    {
        Diagram diagram = parameters.getEngineWrapper().getDiagram();

        if( diagram != null && diagram.getRole() instanceof EModel )
        {
            if( diagram.getType() instanceof CompositeDiagramType )
                throw new IllegalArgumentException("The analysis can not be applied to the composite diagrams.");

            EModel emodel = diagram.getRole(EModel.class);
            StreamEx<Equation> explicitRateEquations = emodel.getEquations(new AnalysisUtils.MathRateEquationFilter());

            if( explicitRateEquations.count() != 0  )
            {
                log.log(Level.SEVERE, "The analysis method is not applicable for a diagram with explicit rate equations.");
            }
            else
            {
                Map<String, Double> steadyStateValues = findSteadyState(diagram);
                if( steadyStateValues != null )
                {
                    resolver = emodel.getVariableResolver(EModel.VARIABLE_NAME_BY_TITLE_BRIEF);
                    resolvedVariables = new HashMap<>();
                    return performAnalysis(steadyStateValues);
                }
                log.info("Steady state is not reached.");
            }
        }
        return null;
    }

    public MetabolicControlAnalysisResults performAnalysis(Map<String, Double> steadyStateValues) throws Exception
    {
        Diagram diagram = parameters.getEngineWrapper().getDiagram();

        MetabolicControlAnalysisResults results = new MetabolicControlAnalysisResults();

        TableDataCollection stoichiometry = StoichiometricAnalysis.getStoichiometricMatrix(diagram);
        results.setStoichiometry(stoichiometry);

        String[] formulas = initFormulas(diagram, stoichiometry);
        String[] species = stoichiometry.stream().map( RowDataElement::getName ).toArray( String[]::new );

        EModel model = diagram.getRole(EModel.class);
        AnalysisUtils.loadFunctionDeclarations(model);
        AnalysisUtils.loadEquations(model);

        MathContext context = new MathContext(steadyStateValues);
        double[] rates = AnalysisUtils.calculateFormulas(formulas, context);
        double[][] gradients = AnalysisUtils.calculateGradients(formulas, species, context);

        Pair<double[][], double[][]> elasticities = calculateElasticities(stoichiometry, context, rates, gradients);
        Pair<double[][], double[][]> cc = calculateCC(stoichiometry, context, rates, gradients);
        Pair<double[][], double[][]> fc = calculateFC(stoichiometry, cc, rates, gradients);

        if( elasticities != null )
        {
            results.setUnscaledElasticities(elasticities.getFirst());
            results.setElasticities(elasticities.getSecond());
        }

        if( cc != null )
        {
            results.setUnscaledCC(cc.getFirst());
            results.setCC(cc.getSecond());
        }

        if( fc != null )
        {
            results.setUnscaledFC(fc.getFirst());
            results.setFC(fc.getSecond());
        }

        return results;
    }

    private String[] initFormulas(Diagram diagram, TableDataCollection stoichiometry) throws Exception
    {
        resolvedReactions = new HashMap<>();

        String[] formulas = new String[stoichiometry.getColumnModel().getColumnCount()];
        for( int i = 0; i < formulas.length; ++i )
        {
            String reaction = stoichiometry.getColumnModel().getColumn(i).getName();
            DiagramElement de = diagram.findDiagramElement(reaction);
            resolvedReactions.put(de.getName(), de.getTitle());
            formulas[i] = ( (Reaction)de.getKernel() ).getFormula();
        }
        return formulas;
    }

    /**
     * The formula for the elasticities calculation can be found here: http://en.wikipedia.org/wiki/Elasticity_Coefficient
     * 
     * @return pair of the unscaled (first) and scaled (second) elastisities.
     */
    private Pair<double[][], double[][]> calculateElasticities(TableDataCollection stoichiometry, MathContext steadyStateValues,
            double[] rates, double[][] gradients)
    {
        int n = rates.length;
        int m = stoichiometry.getSize();

        double[][] elasticities = new double[n][m];
        for( int i = 0; i < n; ++i )
        {
            for( int j = 0; j < m; ++j )
            {
                double speciesValue = steadyStateValues.get(stoichiometry.getName(j), 0);
                elasticities[i][j] = speciesValue / rates[i] * gradients[i][j];
            }
        }
        return new Pair<>(gradients, elasticities);
    }

    /**
     * un_CC = - L * (N_R * Dx * L)^(-1) * N_R,
     * 
     * where un_CC determines unscaled concentration control matrix,
     * N = L * N_R is the stoichiometric matrix of the system,
     * Dx is the gradients matrix.
     * 
     * The detailed information about this formula can be found in the article:
     * C. Reder. Metabolic control theory: a structural approach. J. Theor. Biol., 135:175-201 (1988)
     * (Section 3b, Proposition 4)
     * 
     * The result matrix CC is obtained from the matrix un_CC after scaling each element (i,j) by the
     * factor rates[j]/species[i]
     * 
     * @return pair of the unscaled (first) and scaled (second) concentration control coefficients.
     */
    private Pair<double[][], double[][]> calculateCC(TableDataCollection stoichiometry, MathContext steadyStateValues,
            double[] rates, double[][] gradients) throws Exception
    {
        StoichiometricMatrixDecomposition smd = new StoichiometricMatrixDecomposition(stoichiometry);

        if( smd.getReducedStoichiometry() != null && smd.getLinkMatrix() != null )
        {
            Matrix N_R = new Matrix(smd.getReducedStoichiometry());
            Matrix L = new Matrix(smd.getLinkMatrix());
            Matrix Dx = new Matrix(gradients);

            Matrix un_CC = N_R.times(Dx.times(L));
            if( un_CC.det() == 0 )
            {
                log.info("Can not calculate control matrixes. Jacobian matrix of the model is singular.");
                return null;
            }

            un_CC = un_CC.inverse();
            un_CC = L.times(un_CC).times(N_R);
            un_CC.timesEquals( -1.0);

            double[][] cc = new double[un_CC.getRowDimension()][un_CC.getColumnDimension()];
            for( int i = 0; i < stoichiometry.getSize(); ++i )
            {
                for( int j = 0; j < rates.length; ++j )
                {
                    cc[i][j] = un_CC.get(i, j) * rates[j] / steadyStateValues.get(stoichiometry.getName(i), 0);
                }
            }
            return new Pair<>(un_CC.getArray(), cc);
        }
        return null;
    }

    /**
     * un_FC = Id + Dx * un_CC,
     * 
     * where un_FC determines unscaled flux control matrix, un_CC is the unscaled concentration control matrix,
     * Id is the identity matrix.
     * 
     * The detailed information about this formula can be found in the article:
     * C. Reder. Metabolic control theory: a structural approach. J. Theor. Biol., 135:175-201 (1988)
     * (Section 3b, Proposition 4)
     * 
     * The result matrix FC is obtained from the matrix un_FC after scaling each element (i,j) by the
     * factor rates[j]/rates[i]
     * 
     * @return pair of the unscaled (first) and scaled (second) flux control coefficients.
     */
    private Pair<double[][], double[][]> calculateFC(TableDataCollection stoichiometry, Pair<double[][], double[][]> cc, double[] rates,
            double[][] gradients)
    {
        if( cc != null )
        {
            int n = stoichiometry.getColumnModel().getColumnCount();

            Matrix Id = Matrix.identity(n, n);
            Matrix Dx = new Matrix(gradients);
            Matrix un_CC = new Matrix(cc.getFirst());

            Matrix un_FC = Id.plus(Dx.times(un_CC));

            double[][] fc = new double[un_FC.getRowDimension()][un_FC.getColumnDimension()];
            for( int i = 0; i < rates.length; ++i )
            {
                for( int j = 0; j < rates.length; ++j )
                {
                    fc[i][j] = un_FC.get(i, j) * rates[j] / rates[i];
                }
            }
            return new Pair<>(un_FC.getArray(), fc);
        }
        return null;
    }

    private @Nullable TableDataCollection getElastisitiesToPut(@Nonnull DataCollection origin, @Nonnull String name, double[][] matrix,
            TableDataCollection stoichiometry) throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, name);

        int n = stoichiometry.getColumnModel().getColumnCount();
        int m = stoichiometry.getSize();

        tdc.getColumnModel().addColumn( "Reactions", DataType.Text );
        for( int j = 0; j < m; ++j )
        {
            String species = stoichiometry.getName(j);
            tdc.getColumnModel().addColumn( resolve( species ), DataType.Float );
        }

        if( matrix != null )
        {
            for( int i = 0; i < n; ++i )
            {
                Object[] row = generateRow( resolve( stoichiometry.getColumnModel().getColumn( i ).getName() ), matrix[i] );
                TableDataCollectionUtils.addRow( tdc, generateID( i ), row );
            }
        }
        return tdc;
    }

    private @Nullable TableDataCollection getCCToPut(@Nonnull DataCollection origin, @Nonnull String name, double[][] matrix, TableDataCollection stoichiometry)
            throws Exception
    {
        if(matrix == null)
            return null;
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, name);

        tdc.getColumnModel().addColumn( "Entities", DataType.Text );
        for( TableColumn column : stoichiometry.getColumnModel() )
        {
            tdc.getColumnModel().addColumn(column.getName(), column.getDisplayName(), column.getShortDescription(), Double.class,
                    column.getExpression());
        }

        for( int i = 0; i < stoichiometry.getSize(); ++i )
        {
            Object[] row = generateRow( resolve(stoichiometry.getName(i)), matrix[i] );
            TableDataCollectionUtils.addRow(tdc, generateID(i), row);
        }
        return tdc;
    }

    private @Nullable TableDataCollection getFCToPut(@Nonnull DataCollection origin, @Nonnull String name, double[][] matrix, TableDataCollection stoichiometry)
            throws Exception
    {
        if(matrix == null)
            return null;
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, name);

        tdc.getColumnModel().addColumn( "Reactions", DataType.Text );
        for( TableColumn column : stoichiometry.getColumnModel() )
        {
            tdc.getColumnModel().addColumn(column.getName(), column.getDisplayName(), column.getShortDescription(), Double.class,
                    column.getExpression());
        }

        for( int i = 0; i < stoichiometry.getColumnModel().getColumnCount(); ++i )
        {
            Object[] row = generateRow( resolve( stoichiometry.getColumnModel().getColumn( i ).getName() ), matrix[i] );
            TableDataCollectionUtils.addRow( tdc, generateID( i ), row );
        }
        return tdc;
    }

    private VariableResolver resolver;
    private Map<String, String> resolvedVariables;
    private Map<String, String> resolvedReactions;

    private String resolve(String name)
    {
        if( resolver == null )
            return name;

        if( resolvedReactions.containsKey(name) )
            return resolvedReactions.get(name);

        if( resolvedVariables.containsKey(name) )
            return resolvedVariables.get(name);

        String resolved = resolver.resolveVariable(name);
        resolvedVariables.put(name, resolved);

        return resolved;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Metabolic control analysis results
    //

    public static class MetabolicControlAnalysisResults
    {
        private TableDataCollection stoichiometry;
        public TableDataCollection getStoichiometry()
        {
            return stoichiometry;
        }
        public void setStoichiometry(TableDataCollection stoichiometry)
        {
            this.stoichiometry = stoichiometry;
        }

        private double[][] elasticities;
        public double[][] getElasticities()
        {
            return elasticities;
        }
        public void setElasticities(double[][] elasticities)
        {
            this.elasticities = elasticities;
        }

        private double[][] unscaledElasticities;
        public double[][] getUnscaledElasticities()
        {
            return unscaledElasticities;
        }
        public void setUnscaledElasticities(double[][] unscaledElasticities)
        {
            this.unscaledElasticities = unscaledElasticities;
        }

        /**
         * Concentration controls
         */
        private double[][] cc;

        public double[][] getCC()
        {
            return cc;
        }
        public void setCC(double[][] cc)
        {
            this.cc = cc;
        }

        /**
         * Unscaled concentration controls
         */
        private double[][] unscaledCC;

        public double[][] getUnscaledCC()
        {
            return unscaledCC;
        }
        public void setUnscaledCC(double[][] unscaledCC)
        {
            this.unscaledCC = unscaledCC;
        }

        /**
         * Flux controls
         */
        private double[][] fc;

        public double[][] getFC()
        {
            return fc;
        }
        public void setFC(double[][] fc)
        {
            this.fc = fc;
        }

        /**
         * Unscaled flux controls
         */
        private double[][] unscaledFC;

        public double[][] getUnscaledFC()
        {
            return unscaledFC;
        }
        public void setUnscaledFC(double[][] unscaledFC)
        {
            this.unscaledFC = unscaledFC;
        }
    }

    private TableDataCollection[] saveResults(DataCollection origin, List<TableDataCollection> results) throws Exception
    {
        return StreamEx.of(results).nonNull().peek( origin::put ).toArray( TableDataCollection[]::new );
    }
}
