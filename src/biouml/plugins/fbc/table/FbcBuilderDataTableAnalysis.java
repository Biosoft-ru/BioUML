package biouml.plugins.fbc.table;

import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.fbc.FbcConstant;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Reaction;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon ( "resources/FluxBalanceDataTable.gif" )
public class FbcBuilderDataTableAnalysis extends AnalysisMethodSupport<FbcBuilderDataTableAnalysisParameters> implements FbcConstant
{
    public FbcBuilderDataTableAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new FbcBuilderDataTableAnalysisParameters());
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();
        checkPaths();
        checkDefault(parameters.getLowerBoundDefault(), "lower bound");
        checkDefault(parameters.getEqualsDefault(), "equals");
        checkDefault(parameters.getUpperBoundDefault(), "upper bound");
    }

    private void checkDefault(String defaultStr, String paramName)
    {
        try
        {
            Double.valueOf(defaultStr);
        }
        catch( NumberFormatException e )
        {
            if( !defaultStr.isEmpty() )
                throw new IllegalArgumentException(
                        "Default value of " + paramName + " is invalid. It must be empty or double, but was '" + defaultStr + "'.");
        }
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = parameters.getDiagramPath().getDataElement(Diagram.class);
        TableDataCollection tdc = getFbcData(diagram);
        DataElementPath path = parameters.getFbcResultPath();
        DataCollection<?> origin = path.getParentCollection();
        String name = path.getName();
        origin.remove(name);
        TableDataCollection toPut = copyToResultTable(origin, name, tdc);
        CollectionFactoryUtils.save(toPut);
        return toPut;
    }

    public TableDataCollection getFbcData(Diagram diagram)
    {
        if( diagram == null )
            return null;

        TableDataCollection tdc = new StandardTableDataCollection(null, "");
        List<Node> reactionsNode = DiagramUtility.getReactionNodes(diagram);
        DynamicProperty dp = diagram.getAttributes().getProperty("Packages");
        boolean isFbc = dp != null ? StreamEx.of((String[])dp.getValue()).anyMatch("fbc"::equals) : false;

        tdc.getColumnModel().addColumn("Formula", String.class);
        tdc.getColumnModel().addColumn("Greater", String.class);
        tdc.getColumnModel().addColumn("Equal", String.class);
        tdc.getColumnModel().addColumn("Less", String.class);
        tdc.getColumnModel().addColumn("Coefficient Objective Function", Double.class);

        for( Node reaction : reactionsNode )
        {
            String name = reaction.getName();
            String formula = ( (Reaction)reaction.getKernel() ).getFormula();
            String less = "", equal = "", greater = "";
            double coefficient = 0;
            if( isFbc )
            {
                DynamicPropertySet dps = reaction.getAttributes();
                DynamicProperty dpr = dps.getProperty(FBC_BOUNDS);
                if( dpr != null )
                {
                    FluxBounds fluxBounds = (FluxBounds)dpr.getValue();
                    for( int i = 0; i < fluxBounds.sign.size(); i++ )
                    {
                        String sign = fluxBounds.sign.get(i);
                        String value = fluxBounds.value.get(i);
                        if( FBC_LESS_EQUAL.equals(sign) )
                            less = value;
                        else if( FBC_GREATER_EQUAL.equals(sign) )
                            greater = value;
                        else if( FBC_EQUAL.equals(sign) )
                            equal = value;
                    }
                }
                dpr = dps.getProperty(FBC_OBJECTIVES);
                String activObj = diagram.getAttributes().getValueAsString(FBC_ACTIVE_OBJECTIVE);
                if( dpr != null )
                {
                    FluxObjFunc fluxObj = (FluxObjFunc)dpr.getValue();
                    if( fluxObj.idObj.contains(activObj) )
                    {
                        int index = fluxObj.idObj.indexOf(activObj);
                        coefficient = fluxObj.coefficient.get(index);
                    }
                }
            }
            else
            {
                greater = parameters.getLowerBoundDefault();
                equal = parameters.getEqualsDefault();
                less = parameters.getUpperBoundDefault();
                coefficient = parameters.getFbcObjective();
            }
            Object[] reactionData = new Object[] {formula, greater, equal, less, coefficient};
            TableDataCollectionUtils.addRow(tdc, name, reactionData);
        }
        return tdc;
    }
    private TableDataCollection copyToResultTable(DataCollection<?> origin, String name, TableDataCollection tdc)
    {
        if( origin == null || tdc == null )
            return null;

        TableDataCollection st = TableDataCollectionUtils.createTableDataCollection(origin, name);
        tdc.columns().forEach(st.getColumnModel()::addColumn);
        tdc.stream().forEach(rde -> TableDataCollectionUtils.addRow(st, rde.getName(), rde.getValues()));
        return st;
    }
}
