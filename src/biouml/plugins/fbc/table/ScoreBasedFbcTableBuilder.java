package biouml.plugins.fbc.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/FluxBalanceDataTableScore.gif")
public class ScoreBasedFbcTableBuilder extends AnalysisMethodSupport<ScoreBasedFbcTableBuilderParameters>
{
    public ScoreBasedFbcTableBuilder(DataCollection<?> origin, String name)
    {
        super( origin, name, new ScoreBasedFbcTableBuilderParameters() );
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();
        String scoreColumnName = parameters.getScoreColumnName();
        boolean isScoreColumnUnspecified = parameters.getInputEnzymes() == null || scoreColumnName == null || scoreColumnName.isEmpty()
                || scoreColumnName.equals( ScoreBasedFbcTableBuilderParameters.NONE );
        if( isScoreColumnUnspecified && parameters.getObjectiveTable() == null )
        {
            throw new IllegalArgumentException(
                    "Score column from the enzymes table or column from the table with objective function values must be specified!" );
        }
        if( !isScoreColumnUnspecified )
        {
            TableDataCollection enzTable = parameters.getInputEnzymes().getDataElement( TableDataCollection.class );
            enzTable.getColumnModel().getColumn( scoreColumnName );

            if( parameters.isCorrelation() && enzTable.stream().map( rde -> Math.abs( (Double)rde.getValue( scoreColumnName ) ) )
                    .filter( Objects::nonNull )
                    .anyMatch( score -> score > 1 ) )
                throw new IllegalArgumentException( "Option 'Is score correlation' is enabled, but selected column '"
                        + scoreColumnName + "' with enzymes scores contains elements which are not correlation."
                        + " Please, disable 'Is score correlation' option or select another score column." );
        }
        String maxColumnName = parameters.getMaxColumnName();
        if( maxColumnName != null && !maxColumnName.isEmpty() && !maxColumnName.equals( ScoreBasedFbcTableBuilderParameters.NONE ) )
            parameters.getInputEnzymes().getDataElement( TableDataCollection.class ).getColumnModel().getColumn( maxColumnName );

        String objectiveColumnName = parameters.getObjectiveColumnName();
        if( parameters.getObjectiveTable() != null && objectiveColumnName != null && !objectiveColumnName.isEmpty()
                && !objectiveColumnName.equals( ScoreBasedFbcTableBuilderParameters.NONE ) )
            parameters.getObjectiveTable().getDataElement( TableDataCollection.class ).getColumnModel().getColumn( objectiveColumnName );
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        DataElementPath path = parameters.getOutputPath();
        DataCollection<?> origin = path.getParentCollection();
        String name = path.getName();
        origin.remove( name );
        TableDataCollection tdc = createFbcTable( origin, name );
        CollectionFactoryUtils.save( tdc );
        return tdc;
    }

    /**
     * Creates and fills fbc-table with given parent and name
     * @param parent parent collection
     * @param tableName name of the fbc table
     * @return table with given parent and name, which contains fbc data
     */
    private @Nonnull TableDataCollection createFbcTable(@Nonnull DataCollection<?> parent, @Nonnull String tableName)
    {
        Diagram diagram = parameters.getInputDiagram().getDataElement( Diagram.class );
        DataElementPath pathToEnzymes = parameters.getInputEnzymes();
        Map<String, Double> enzymesToScore = getIdToValueMap( pathToEnzymes, parameters.getScoreColumnName() );
        Map<String, Double> enzymesToBound = getIdToValueMap( pathToEnzymes, parameters.getMaxColumnName() );

        DataElementPath pathToObjective = parameters.getObjectiveTable();
        Map<String, Double> objectiveFunction = getIdToValueMap( pathToObjective, parameters.getObjectiveColumnName() );

        boolean isObjectiveSpecified = !objectiveFunction.isEmpty();
        if( objectiveFunction.isEmpty() && pathToObjective != null )
        {
            pathToObjective.getDataElement( TableDataCollection.class ).getNameList()
                    .forEach( name -> objectiveFunction.put( name, 1.0 ) );
        }

        List<Node> reactionNodes = DiagramUtility.getReactionNodes( diagram );
        List<FBCData> data = new ArrayList<>();
        double norm = 0;
        for( Node reactionNode : reactionNodes )
        {
            Reaction reaction = (Reaction)reactionNode.getKernel();

            String name = reactionNode.getName();
            String formula = reaction.getFormula();
            double upperBound = getUpperBound( reaction, enzymesToBound );
            double coefficient = getCoefficient( reaction, enzymesToScore );
            double multiplier = objectiveFunction.isEmpty() ? 1.0 : objectiveFunction.getOrDefault( name, 0.0 );

            data.add( new FBCData( name, formula, upperBound, reaction.isReversible(), coefficient, multiplier ) );
            norm = Math.max(norm, upperBound);
        }

        boolean isScoreSpecified = !enzymesToScore.isEmpty();
        boolean isMaxSpecified = !enzymesToBound.isEmpty();

        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( parent, tableName );
        if( isMaxSpecified )
            tdc.getColumnModel().addColumn( parameters.getMaxColumnName() + " (upper bound)", Double.class );
        if( isScoreSpecified )
            tdc.getColumnModel().addColumn( parameters.getScoreColumnName() + " (score column)", Double.class );
        if( isObjectiveSpecified )
            tdc.getColumnModel().addColumn( parameters.getObjectiveColumnName() + " (additional objective)", Double.class );
        tdc.getColumnModel().addColumn( "Formula", String.class );
        tdc.getColumnModel().addColumn( "Greater", String.class );
        tdc.getColumnModel().addColumn( "Equal", String.class );
        tdc.getColumnModel().addColumn( "Less", String.class );
        tdc.getColumnModel().addColumn( "Coefficient Objective Function", Double.class );

        boolean isCoeffCorrelation = parameters.isCorrelation() && !enzymesToScore.isEmpty();
        norm = parameters.isNorm() ? norm : 1.0;
        for( FBCData d : data )
        {
            d.setNorm( norm );
            d.addAsRow( tdc, isCoeffCorrelation, isScoreSpecified, isMaxSpecified, isObjectiveSpecified );
        }

        return tdc;
    }

    /**
     * Creates a map id to value (takes value from given table and column)
     * @param pathToTable path to table with values
     * @param columnName name of the column with values
     * @return
     */
    private @Nonnull Map<String, Double> getIdToValueMap(DataElementPath pathToTable, String columnName)
    {
        Map<String, Double> result = new HashMap<>();
        if( pathToTable == null || columnName == null || columnName.isEmpty()
                || columnName.equals( ScoreBasedFbcTableBuilderParameters.NONE ) )
            return result;
        TableDataCollection tdc = pathToTable.getDataElement( TableDataCollection.class );
        for( RowDataElement rde : tdc )
        {
            double value;
            try
            {
                value = Double.parseDouble( rde.getValueAsString( columnName ) );
            }
            catch( NumberFormatException e )
            {
                value = 0.0;
            }
            catch( NullPointerException npe )
            {
                throw new IllegalArgumentException( "Cannot find column '" + columnName + "' in table '" + tdc.getName() + "'" );
            }
            result.put( rde.getName(), value );
        }
        return result;
    }

    /**
     * Constructs upper bound for the reaction flux.
     * @param reaction reaction to get upper bound to.
     * @param enzymesToBound map with keys-enzymes and double values which will be used to construct bound.
     * @return upper bound. If map is empty it returns <code>1.0</code>.
     * If enzymes which modify reaction are not included in the map, the minimal value from the map will be selected.
     */
    private double getUpperBound(Reaction reaction, Map<String, Double> enzymesToBound)
    {
        if( enzymesToBound.isEmpty() )
            return 1.0;

        double tableMin = StreamEx.of( enzymesToBound.values() ).nonNull().mapToDouble( Double::doubleValue ).map( Math::abs ).min()
                .getAsDouble();
        return reaction.stream().remove( SpecieReference::isReactantOrProduct ).map( SpecieReference::getSpecieName )
                .map( enzymesToBound::get ).nonNull().mapToDouble( Double::doubleValue ).map( Math::abs ).min().orElse( tableMin );
    }

    /**
     * Finds max coefficient of related enzymes using <b>enzymesScore</b> map. If there are no enzymes found returns <code>-8.0</code>.
     * @param reaction reaction to get coefficient to.
     * @param enzymesScore global map of enzymes scores.
     * @return objective function coefficient.
     */
    private double getCoefficient(Reaction reaction, Map<String, Double> enzymesScore)
    {
        if( enzymesScore.isEmpty() )
            return 1.0;
        List<Double> scores = reaction.stream().remove( SpecieReference::isReactantOrProduct ).map( SpecieReference::getSpecieName )
                .map( enzymesScore::get ).nonNull().toList();
        //TODO: rework
        double max = parameters.isCorrelation() && !scores.isEmpty() ? 0.0 : -8.0;
        for( double score : scores )
        {
            if( parameters.isCorrelation() )
            {
                if( Math.abs( max ) < Math.abs( score ) )
                    max = score;
            }
            else if( max < score )
                max = score;
        }
        if( reaction.isReversible() && max == -8.0 )
            max = 0.0;
        return max;
    }

    private static class FBCData
    {
        final boolean isReversible;
        final double upperBound;
        final String reactionName;
        final String formula;
        final double coefficient;
        final double multiplier;

        public FBCData(String reactionName, String formula, double upperBound, boolean isReversible, double coefficient, double multiplier)
        {
            this.reactionName = reactionName;
            this.upperBound = upperBound;
            this.isReversible = isReversible;
            this.formula = formula;
            this.coefficient = coefficient;
            this.multiplier = multiplier;
        }
        /**
         * Adds fbc data as a row in the given table data collection.
         * @param tdc table data collection to save in
         * @param isCoeffCorrelation flag to show if objective function coefficient should be processed as correlation
         * @param addScore flag to show if not processed coefficient value should be added to a row
         * @param addUpperBound flag to show if not processed upper bound value should be added to a row
         * @param addObjective flag to show if multiplier value should be added to a row
         */
        public void addAsRow(TableDataCollection tdc, boolean isCoeffCorrelation, boolean addScore, boolean addUpperBound,
                boolean addObjective)
        {
            String upper = String.valueOf( upperBound / norm );
            String lowerBound = isReversible ? "-" + upper : "0.0";

            double resultCoef = coefficient;
            if( isCoeffCorrelation )
            {
                resultCoef = Math.abs( resultCoef );
                if( resultCoef >= 0.75 && resultCoef <= 1.0 ) // high
                    resultCoef = 20.0;
                else if( resultCoef < 0.75 && resultCoef > 0.25 ) // medium
                    resultCoef = 15.0;
                else if( resultCoef <= 0.25 && resultCoef > 0 ) // low
                    resultCoef = 10.0;
                else if ( resultCoef != 0 ) // absent
                    resultCoef = -8.0;
                // we ignore elements with 'coeff == 0' in the objective function
            }
            resultCoef *= multiplier;

            List<Object> reactionData = new ArrayList<>();
            if( addUpperBound )
                reactionData.add( upperBound );
            if( addScore )
                reactionData.add( coefficient );
            if( addObjective )
                reactionData.add( multiplier );
            reactionData.add( formula );
            reactionData.add( lowerBound );
            reactionData.add( "" );
            reactionData.add( upper );
            reactionData.add( resultCoef );

            TableDataCollectionUtils.addRow( tdc, reactionName, reactionData.toArray() );
        }

        double norm = 1.0;
        /**
         * Set value of the <code>norm</code> parameter.
         * If new value is less than zero it does nothing.
         * @param norm new value of the norm
         */
        protected void setNorm(double norm)
        {
            if( norm > 0 )
                this.norm = norm;
        }
    }

}
