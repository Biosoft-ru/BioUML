package biouml.plugins.enrichment;

import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ExternalURLMapper;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.analysis.TableConverter;
import ru.biosoft.analysis.TableConverterParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.access.TableRowsExporter;
import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.plugins.expression.ExpressionFilter;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

public class MapOnPathwayAction extends SaveHitsAction
{
    private static final String KERNEL_REFERENCE_TYPE_PROPERTY = "kernelReferenceType";
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isApplicable(Object object)
    {
        if( !super.isApplicable(object) )
            return false;
        if( ! ( object instanceof TableDataCollection ) )
            return false;
        TableDataCollection table = (TableDataCollection)object;
        if( table.isEmpty() )
            return false;
        DataElementPath diagramPath = ExternalURLMapper.getPathToLinkedElement(table.getAt(0));
        if( diagramPath == null )
            return false;
        Class<? extends DataElement> type = DataCollectionUtils.getElementType(diagramPath);
        if( type == null || !Diagram.class.isAssignableFrom(type) )
            return false;
        DataCollection<? extends DataElement> parentCollection = diagramPath.optParentCollection();
        if( parentCollection == null )
            return false;
        String kernelReferenceTypeStr = parentCollection.getInfo().getProperty(KERNEL_REFERENCE_TYPE_PROPERTY);
        ReferenceType referenceType = ReferenceTypeRegistry.optReferenceType(kernelReferenceTypeStr);
        if( referenceType == null || referenceType == ReferenceTypeRegistry.getDefaultReferenceType() )
            return false;
        return true;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log){
            @Override
            protected void doRun() throws JobControlException
            {
                DataElementPath destination = (DataElementPath)((DynamicPropertySet)properties).getValue("target");
                DataElementPath hitsPath = destination.getSiblingPath(destination.getName()+" hits_tmp");
                DataElementPath convertedPath = destination.getSiblingPath(destination.getName()+" hits");
                try
                {
                    FunctionalClassificationParameters parameters = (FunctionalClassificationParameters)AnalysisParametersFactory.read((DataElement)model);
                    TableDataCollection source = parameters.getSource();
                    TableDataCollection table = (TableDataCollection)model;
                    List<RowDataElement> hitsDE = getHits(table, source, selectedItems);
                    TableRowsExporter.exportTable(hitsPath, source, hitsDE, new SubFunctionJobControl( this, 0, 10 ));
                    setPreparedness(10);
                    Diagram diagram = ExternalURLMapper.getPathToLinkedElement(selectedItems.get(0)).getDataElement(Diagram.class);
                    TableConverter tableConverter = new TableConverter(null, "Table converter");
                    TableConverterParameters tableConverterParameters = tableConverter.getParameters();
                    tableConverterParameters.setSourceTable(hitsPath);
                    tableConverterParameters.setSpecies(parameters.getSpecies());
                    String targetTypeName = diagram.getOrigin().getInfo().getProperty(KERNEL_REFERENCE_TYPE_PROPERTY);
                    if(targetTypeName == null)
                        throw new DataElementReadException(diagram.getOrigin(), KERNEL_REFERENCE_TYPE_PROPERTY);
                    ReferenceType targetType = ReferenceTypeRegistry.getReferenceType(targetTypeName);
                    tableConverterParameters.setTargetType(targetType.toString());
                    tableConverterParameters.setOutputTable(convertedPath);
                    tableConverter.justAnalyzeAndPut();
                    setPreparedness(50);
                    ExpressionFilter filter = new ExpressionFilter("Hits");
                    filter.setEnabled(true);
                    filter.getProperties().setTable(convertedPath);
                    setPreparedness(60);
                    Diagram result = diagram.clone(destination.optParentCollection(), destination.getName());
                    result.setFilterList(new DiagramFilter[] {filter});
                    result.setDiagramFilter(filter);
                    destination.save(result);
                    hitsPath.remove();
                    setPreparedness(100);
                    resultsAreReady(new Object[]{result});
                }
                catch( Exception e )
                {
                    try
                    {
                        destination.remove();
                    }
                    catch( Exception e1 )
                    {
                    }
                    try
                    {
                        convertedPath.remove();
                    }
                    catch( Exception e1 )
                    {
                    }
                    try
                    {
                        hitsPath.remove();
                    }
                    catch( Exception e1 )
                    {
                    }
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        DataElementPath sourcePath = DataElementPath.create((DataElement)model);
        return getTargetProperties(TableDataCollection.class,
                DataElementPath.create(sourcePath.optParentCollection(), sourcePath.getName() + " " + selectedItems.get(0).getName())
                        .uniq());
    }
    
    @Override
    public void validateParameters(Object tdc, List<DataElement> selectedItems) throws LoggedException
    {
        super.validateParameters(tdc, selectedItems);
        DataElement item = selectedItems.get(0);
        DataElementPath path = ExternalURLMapper.getPathToLinkedElement(item);
        if(path == null)
            throw new DataElementReadException(item, "path to diagram");
        path.getDataElement(Diagram.class);
    }
}
