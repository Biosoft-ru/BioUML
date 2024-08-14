package ru.biosoft.analysis;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.PropertyInfo;
import biouml.model.Diagram;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

@ClassIcon("resources/annotate-table.gif")
public class Annotate extends AnalysisMethodSupport<AnnotateParameters>
{
    int unMatched = 0;
    int notFound = 0;

    public Annotate(DataCollection<?> origin, String name) throws Exception
    {
        super(origin, name, new AnnotateParameters());
    }

    @Override
    public void validateParameters()
    {
        checkPaths(new String[] {"inputTablePath", "annotationCollectionPath"}, parameters.getOutputNames());
        checkAnnotationCollection();
        checkNotEmpty("annotationColumns");
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        try
        {
            TableDataCollection table = parameters.getInputTable();

            DataCollection<?> annotationCollection = parameters.getAnnotationCollection();
            PropertyInfo[] annotationProperties = parameters.getAnnotationColumns();
            String[] annotations = StreamEx.of( annotationProperties ).map( PropertyInfo::getName ).toArray( String[]::new );
            
            jobControl.pushProgress(0, 80);

            //Annotating through BioHub
            TableDataCollection annotationTable = matchViaMatchingHub(annotationCollection, table, annotations, parameters.getSpecies());
                
            if(annotationTable == null)
                annotationTable = matchViaBioHub(annotationCollection, table, annotations);

            if( annotationTable == null ) //BioHub annotation failed, do manually
            {
                log.info("BioHub can not be initialized.");
                log.info("Will do annotation only with keys matching...");

                annotationTable = matchViaKeys(annotationCollection, table, annotations);
            }
            jobControl.popProgress();
            if(jobControl.isStopped()) return null;

            log.info("Writing annotations...");
            String[] newColumnNames = new String[annotations.length];
            String newColumnNameSuffix = " (" + annotationCollection.getName()+")";
            Set<String> columns = getFilteredColumns(table, annotationTable);
            Set<String> lowerCaseColumns = new HashSet<>();
            for(String column: columns) lowerCaseColumns.add(column.toLowerCase());
            for(int i=0; i<newColumnNames.length; i++)
            {
                String newName = annotationProperties[i].getDisplayName().replaceFirst("^.+\\/", "");
                if(lowerCaseColumns.contains(newName.toLowerCase()))
                    newName = annotationProperties[i].getDisplayName().replaceFirst("^.+\\/", "")+newColumnNameSuffix;
                newColumnNames[i] = newName;
                int j=0;
                while(lowerCaseColumns.contains(newColumnNames[i].toLowerCase()) || newColumnNames[i].equals(DataCollectionConfigConstants.NAME_PROPERTY))
                {
                    newColumnNames[i] = newName+"_"+(++j);
                }
            }
            String[] columnsArray = columns.toArray(new String[columns.size()]);
            TableDataCollection result = TableDataCollectionUtils.join(TableDataCollectionUtils.RIGHT_JOIN, annotationTable, table,
                    parameters.getOutputTablePath(), annotations, columnsArray, newColumnNames, columnsArray);
            jobControl.setPreparedness(95);
            try
            {
                Object annObject = annotationCollection.iterator().next();
                if(annObject != null)
                {
                    ComponentModel model = ComponentFactory.getModel(annObject);
                    for(int i=0; i<annotations.length; i++)
                    {
                        Property property = model.findProperty(annotations[i]);
                        if(property != null && ReferenceTypeRegistry.getTypeForDescriptor(property.getDescriptor()) != null)
                        {
                            result.getColumnModel()
                                    .getColumn(newColumnNames[i])
                                    .setValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY,
                                            ReferenceTypeRegistry.getTypeForDescriptor(property.getDescriptor()).toString());
                        }
                    }
                }
            }
            catch( Exception e )
            {
            }
            DataCollectionUtils.copyPersistentInfo(result, table);
            parameters.getOutputTablePath().save(result);
            jobControl.setPreparedness(100);
            return result;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Analysis failed for some reason: " + ex.getLocalizedMessage());
        }
        return null;
    }



    /**
     * @param table
     * @param annotationTable
     * @return
     */
    protected Set<String> getFilteredColumns(TableDataCollection table, TableDataCollection annotationTable)
    {
        Set<String> usedSources = new HashSet<>();
        if(parameters.isReplaceDuplicates())
        {
            for(TableColumn column: annotationTable.getColumnModel())
            {
                String annotationSource = column.getValue(TableColumn.ANNOTATION_SOURCE_PROPERTY);
                if(annotationSource != null) usedSources.add(annotationSource);
            }
        }
        Set<String> result = new LinkedHashSet<>();
        for(TableColumn column: table.getColumnModel())
        {
            String annotationSource = column.getValue(TableColumn.ANNOTATION_SOURCE_PROPERTY);
            if(annotationSource == null || !usedSources.contains(annotationSource))
                result.add(column.getName());
        }
        return result;
    }

    protected TableDataCollection matchViaKeys(final DataCollection<?> annotationCollection, DataCollection<?> table, final String[] annotations)
            throws Exception
    {
        final TableDataCollection annotationTable = createAnnotationTable(annotationCollection, annotations);
        final int n = annotations.length;
        jobControl.forCollection(table.getNameList(), key -> {
            Object[] rowValues = new Object[n];
            DataElement element = null;
            try
            {
                element = annotationCollection.get(key);
            }
            catch( Exception e1 )
            {
            }
            if( element == null )
            {
                unMatched++;
                return true;
            }
            for( int i = 0; i < n; i++ )
            {
                Object annotation = null;
                try
                {
                    annotation = BeanUtil.getBeanPropertyValue(element, annotations[i]);
                }
                catch( Exception e )
                {
                }
                rowValues[i] = ( annotation != null ) ? annotation : "null";
            }
            TableDataCollectionUtils.addRow(annotationTable, key, rowValues);
            return true;
        });

        log.info("Number of input IDs: " + table.getSize());
        log.info("Number of unmatched IDs: " + unMatched);
        log.info("Matching keys finished");
        return annotationTable;
    }

    protected TableDataCollection matchViaBioHub(final DataCollection<?> annotationCollection, DataCollection<?> table, final String[] annotations)
            throws Exception
    {
        final TableDataCollection annotationTable = createAnnotationTable(annotationCollection, annotations);

        CollectionRecord collection = new CollectionRecord(annotationCollection.getCompletePath(), true);
        TargetOptions dbOptions = new TargetOptions(collection);
        BioHub hub = BioHubRegistry.getBioHub(dbOptions);

        if( hub == null )
            return null;

        Element[] startElements = table.names().map( key -> new Element( "stub/%//" + key ) ).toArray( Element[]::new );
        if(jobControl.isStopped()) return null;

        log.info("Getting data from biohub, please wait...");
        jobControl.pushProgress(0, 50);
        final Map<Element, Element[]> references = hub.getReferences(startElements, dbOptions, null, 1, -1);
        jobControl.popProgress();

        unMatched = 0;
        final int n = annotations.length;

        jobControl.pushProgress(50, 100);
        jobControl.forCollection(references.keySet(), elem -> {
            Element[] refs = references.get(elem);

            String tableID = elem.getPath().substring(elem.getPath().lastIndexOf("//") + 2);
            if( refs == null || refs.length == 0 )
            {
                unMatched++;
                return true;
            }

            String idFromCollection = refs[0].getAccession(); //we pick only first found id
            DataElement element = null;
            try
            {
                element = annotationCollection.get(idFromCollection);
            }
            catch( Exception e1 )
            {
            }
            if(element == null)
            {
                unMatched++;
                return true;
            }

            Object[] rowValues = new Object[n];
            for( int i = 0; i < n; i++ )
            {
                Object annotation = null;
                try
                {
                    annotation = BeanUtil.getBeanPropertyValue(element, annotations[i]);
                }
                catch( Exception e )
                {
                }
                rowValues[i] = ( annotation != null ) ? annotation : "null";
            }
            TableDataCollectionUtils.addRow(annotationTable, tableID, rowValues);
            return true;
        });
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;

        log.info("Number of input IDs: " + table.getSize());
        log.info("Number of unmatched IDs: " + unMatched);
        log.info("Matching fields via BioHub finished");
        return annotationTable;
    }

    protected TableDataCollection matchViaMatchingHub(final DataCollection<?> annotationCollection, DataCollection<?> table,
            final String[] annotations, Species species) throws Exception
    {
        ReferenceType inputType = ReferenceTypeRegistry.getElementReferenceType(table);
        ReferenceType outputType = ReferenceTypeRegistry.getElementReferenceType(annotationCollection);
        if( inputType == ReferenceTypeRegistry.getDefaultReferenceType() || outputType == ReferenceTypeRegistry.getDefaultReferenceType() )
            return null;
        Properties input = BioHubSupport.createProperties( species, inputType );
        Properties output = BioHubSupport.createProperties( species, outputType );
        if(BioHubRegistry.getMatchingPath(input, output) == null)
            return null;
        if(! inputType.equals(outputType) )
            log.info("Matching from '"+inputType.getDisplayName()+"' to '"+outputType.getDisplayName()+"'");

        String[] ids = table.names().toArray( String[]::new );

        jobControl.pushProgress(0, 70);
        final Map<String, String[]> references = BioHubRegistry.getReferences(ids, input, output, new SubFunctionJobControl(jobControl));
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;
        
        if(references == null)
        {
            log.log(Level.SEVERE, "Cannot match via matching hub");
            return null;
        }
        
        jobControl.pushProgress(70, 100);
        final TableDataCollection annotationTable = createAnnotationTable(annotationCollection, annotations);
        unMatched = 0;
        notFound = 0;
        final int n = annotations.length;
        log.info("Adding annotation...");
        jobControl.forCollection(table.getNameList(), key -> {
            Object[] rowValues = new Object[n];
            List<Set<String>> valuesList = new ArrayList<>();
            for(int i1=0; i1<n; i1++) valuesList.add(new TreeSet<String>());
            String[] refs = references.get(key);
            if(refs == null || refs.length == 0)
            {
                unMatched++;
                return true;
            }
            boolean matched = false;
            for(String ref: refs)
            {
                DataElement element = null;
                try
                {
                    element = annotationCollection.get(ref);
                }
                catch( Exception e1 )
                {
                }
                if( element == null )
                    continue;
                matched = true;
                for( int i2 = 0; i2 < n; i2++ )
                {
                    Object annotation = null;
                    try
                    {
                        annotation = BeanUtil.getBeanPropertyValue(element, annotations[i2]);
                    }
                    catch( Exception e )
                    {
                    }
                    if(annotation == null) continue;
                    if(annotation.getClass().isArray())
                        annotation = StreamEx.of((Object[])annotation).joining(", ");
                    valuesList.get(i2).add(annotation.toString());
                }
            }
            if(!matched)
            {
                notFound++;
                return true;
            }
            for(int i3=0; i3<n; i3++)
            {
                rowValues[i3] = String.join(", ", valuesList.get(i3));
            }
            TableDataCollectionUtils.addRow(annotationTable, key, rowValues);
            return true;
        });
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;
    
        log.info("Number of input IDs: " + table.getSize());
        log.info("Unmatched: " + unMatched);
        log.info("Matched, but absent in annotation: " + notFound);
        log.info("Matched and annotated: " + (table.getSize()-unMatched-notFound));
        log.info("Matching keys finished");
        return annotationTable;
    }

    /**
     * Creates table with annotations in the memory
     * @param annotationCollection collection to take annotations from
     * @param annotations list of properties to use for annotation
     * @return
     */
    protected TableDataCollection createAnnotationTable(DataCollection<?> annotationCollection, String[] annotations)
    {
        TableDataCollection annotationTable = new StandardTableDataCollection(null, "annotations");
        DataElement de = annotationCollection.stream().findFirst().orElse( null );
        for( String colName : annotations )
        {
            DataType dataType;
            try
            {
                dataType = DataType.fromClass( BeanUtil.getBeanPropertyType( de, colName ) );
            }
            catch( Exception e1 )
            {
                log.log( Level.WARNING, "Error during annotation table creating: cannot find property " + colName + " for bean " + de.getClass() );
                dataType = null;
            }
            TableColumn column = dataType != null ? annotationTable.getColumnModel().addColumn( colName, dataType ) : annotationTable
                    .getColumnModel().addColumn( colName, String.class );
            column.setValue(TableColumn.ANNOTATION_SOURCE_PROPERTY, annotationCollection.getCompletePath()+"/*/"+colName);
        }
        return annotationTable;
    }
    
    private void checkAnnotationCollection()
    {
        DataCollection<?> dc = parameters.getAnnotationCollection();
        if( Diagram.class.isAssignableFrom(dc.getDataElementType()) )
            throw new InvalidParameterException("Annotation by collection with diagrams is not available. Please, select another collection.");
    }
}
