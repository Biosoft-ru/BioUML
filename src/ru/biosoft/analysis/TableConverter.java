package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.MatchingStep;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.MatchingPathWriter;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.journal.ProjectUtils;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableColumnMatchingHub;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.TextUtil2;

@ClassIcon("resources/convert-table.gif")
public class TableConverter extends TableConverterSupport<TableConverterParameters>
{
    public TableConverter(DataCollection<?> origin, String name)
    {
        super(origin, name, new TableConverterParameters());
    }

    protected TableConverter(DataCollection<?> origin, String name, TableConverterParameters parameters)
    {
        super(origin, name, parameters);
    }

    @Override
    public TableDataCollection[] justAnalyzeAndPut() throws Exception
    {
        validateParameters();

        jobControl.pushProgress(0, 2);
        log.info("Preparing...");
        final TableDataCollection source = getParameters().getSourceTable().getDataElement(TableDataCollection.class);
        String[] ids = source.names().toArray( String[]::new );
        if(jobControl.isStopped()) return null;
        MatchingStep[] matchingPath = getMatchingPlan();
        if(!getParameters().getIdsColumnName().equals(ColumnNameSelector.NONE_COLUMN))
        {
            TableColumnMatchingHub columnMatchingHub = new TableColumnMatchingHub(source, getParameters().getIdsColumnName());
            Properties fromProperties = BioHubSupport.createProperties( getParameters().getSpecies().getLatinName(),
                    source.getReferenceType() + " (table rows)" );
            MatchingStep fromStep = new MatchingStep(fromProperties);
            MatchingStep step;
            if(matchingPath.length == 0)
            {
                Properties toProperties = BioHubSupport.createProperties( getParameters().getSpecies(), getParameters().getSourceTypeObject() );
                step = new MatchingStep(toProperties);
            } else
            {
                step = matchingPath[0].getFrom();
            }
            step.update(columnMatchingHub, fromStep, 1.0, 1);
            MatchingStep[] newMatchingPath = new MatchingStep[matchingPath.length+1];
            newMatchingPath[0] = step;
            System.arraycopy(matchingPath, 0, newMatchingPath, 1, matchingPath.length);
            matchingPath = newMatchingPath;
        }
        jobControl.popProgress();

        jobControl.pushProgress(2, 50);

        log.info("Matching...");
        Map<String, String[]> references = getReferences(matchingPath, ids);
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;
        Set<String> unmatched = getUnmatched(references);
        log.info("Matched rows: "+(references.size()-unmatched.size()));
        log.info("Unmatched rows: "+(unmatched.size()));

        if(getParameters().getUnmatchedTable() != null)
        {
            log.info("Saving unmatched rows...");
            final TableDataCollection unmatchedTable = TableDataCollectionUtils.createTableDataCollection(getParameters().getUnmatchedTable());
            ColumnModel oldCm = source.getColumnModel();
            ColumnModel newCm = unmatchedTable.getColumnModel();
            for( TableColumn tc : oldCm )
            {
                newCm.addColumn(newCm.cloneTableColumn(tc));
            }
            jobControl.pushProgress(50, 60);
            jobControl.forCollection(unmatched, element -> {
                try
                {
                    unmatchedTable.addRow(source.get(element).clone());
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Cannot add row "+element, e);
                }
                return true;
            });
            if(jobControl.isStopped())
            {
                getParameters().getUnmatchedTable().remove();
                return null;
            }
            unmatchedTable.finalizeAddition();
            DataCollectionUtils.copyPersistentInfo(unmatchedTable, source);
            CollectionFactoryUtils.save(unmatchedTable);
            jobControl.popProgress();
            jobControl.pushProgress(60, 100);
        } else
        {
            jobControl.pushProgress(50, 100);
        }

        log.info("Preparing row list...");
        jobControl.pushProgress(0, 10);
        filterReferencesOnMaxMatches(references);
        Map<String, Set<String>> revReferences = revertReferences(references);
        log.info("Resulting rows: "+revReferences.size());
        jobControl.popProgress();
        checkReferences(revReferences, ids);
        if(jobControl.isStopped())
        {
            if(getParameters().getUnmatchedTable() != null) getParameters().getUnmatchedTable().remove();
            return null;
        }

        log.info("Generating output...");
        jobControl.pushProgress(10, 100);
        final TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(getParameters().getOutputTable());
        fillTable(source, revReferences, result);
        if(jobControl.isStopped())
        {
            getParameters().getOutputTable().remove();
            if(getParameters().getUnmatchedTable() != null) getParameters().getUnmatchedTable().remove();
            return null;
        }
        result.getInfo().getProperties().setProperty(DataCollectionUtils.SPECIES_PROPERTY, getOutputSpecies().getLatinName());
        ReferenceTypeRegistry.setCollectionReferenceType(result, getParameters().getTargetTypeObject());
        CollectionFactoryUtils.save(result);
        writeMatchingPath( result, matchingPath );
        jobControl.popProgress();
        jobControl.popProgress();
        return getParameters().getUnmatchedTable() == null ? new TableDataCollection[] {result} : new TableDataCollection[] {
                result, getParameters().getUnmatchedTable().getDataElement(TableDataCollection.class)};
    }

    protected Species getOutputSpecies()
    {
        return getParameters().getSpecies();
    }

    protected void writeMatchingPath(TableDataCollection result, MatchingStep[] matchingPath)
    {
        List<String> bioHubs = new ArrayList<>();
        String ensemblDBStr = null;
        for( MatchingStep step : matchingPath )
        {
            BioHub bioHub = step.getBioHub();
            if( bioHub == null )
            {
                bioHubs.add( "" );
                continue;
            }
            StringBuilder sb = new StringBuilder( bioHub.getName() );
            if( step.getType().getDisplayName().contains( "Ensembl" ) )
            {
                if( ensemblDBStr == null )
                {
                    String speciesName = getParameters().getSpecies().getLatinName();
                    String ensembl = "Ensembl (" + speciesName + ")";
                    DataElementPath ensemblPath = ProjectUtils.getPreferredDatabasePaths( getParameters().getOutputTable() ).get( ensembl );
                    if( ensemblPath != null )
                    {
                        DataCollection<?> dc = ensemblPath.optDataCollection();
                        String vp;
                        if( dc != null && ( vp = dc.getInfo().getProperty( "version" ) ) != null )
                            ensemblDBStr = ensembl + " " + vp + "; " + ensemblPath.toString();
                        else
                            ensemblDBStr = ensemblPath.toString();
                    }
                }
                if( ensemblDBStr != null )
                    sb.append( " (" ).append( ensemblDBStr ).append( ")" );
            }
            bioHubs.add( sb.toString() );
        }
        Properties properties = result.getInfo().getProperties();
        properties.put( MatchingPathWriter.MATCHING_PATH_PROP, TextUtil2.toString( bioHubs.toArray( new String[0] ) ) );
        CollectionFactoryUtils.save( result );
    }

    /**
     * @return steps to perform the matching
     */
    protected MatchingStep[] getMatchingPlan() throws Exception
    {
        DataElementPath projectPath = ProjectUtils.getProjectPath( getParameters().getOutputTable() );
        Properties inputProperties, outputProperties;
        if( projectPath == null )
        {
            inputProperties = BioHubSupport.createProperties( getOutputSpecies(), getParameters().getSourceTypeObject() );
            outputProperties = BioHubSupport.createProperties( getOutputSpecies(), getParameters().getTargetTypeObject() );
        }
        else
        {
            inputProperties = BioHubSupport.createProperties( getOutputSpecies(), getParameters().getSourceTypeObject(), projectPath );
            outputProperties = BioHubSupport.createProperties( getOutputSpecies(), getParameters().getTargetTypeObject(), projectPath );
        }
        MatchingStep[] matchingPath = BioHubRegistry.getMatchingPath(inputProperties, outputProperties);
        if( matchingPath == null )
            throw new Exception( "Unable to convert '" + getParameters().getSourceTypeObject() + "' to '"
                    + getParameters().getTargetTypeObject() + "': check parameters." );
        log.info("Matching plan:");
        log.info("* "+getParameters().getSourceTypeObject().toString());
        for(MatchingStep step: matchingPath)
        {
            log.info("* "+step.getType().getDisplayName());
        }
        return matchingPath;
    }

    /**
     * @param references
     * @return
     */
    protected Set<String> getUnmatched(Map<String, String[]> references)
    {
        return StreamEx.ofKeys(references, val -> val.length == 0).toSet();
    }

    private void checkReferences(Map<String, Set<String>> references, String[] ids)
    {
        if(references == null || references.isEmpty())
        {
            ReferenceType detectedType = null;
            log.warning("Nothing was matched: result will be empty");
            detectedType = ReferenceTypeRegistry.detectReferenceType(ids);
            if(!(detectedType.equals(getParameters().getSourceTypeObject())))
            {
                log.info("Maybe you have specified wrong input type");
                log.info("Try suggested type: "+detectedType.getDisplayName());
            }
        }
    }

    /**
     * @return name of new column which contain IDs of source table
     */
    @Override
    protected String getSourceColumnName()
    {
        String type;
        if( getParameters().getIdsColumnName().equals(ColumnNameSelector.NONE_COLUMN) )
        {
            type = getParameters().getSourceTypeObject().getSource();
        }
        else
        {
            String referenceType = getParameters().getSourceTable().getDataElement(TableDataCollection.class).getReferenceType();
            type = referenceType == null ? ReferenceTypeRegistry.getDefaultReferenceType().toString() : referenceType;
        }
        return type + " ID";
    }

    private Map<String, String[]> getReferences(MatchingStep[] matchingPath, String[] ids)
            throws Exception
    {
        SubFunctionJobControl subJobControl = new SubFunctionJobControl(jobControl);
        Map<String, String[]> references = BioHubRegistry.getReferences(ids, matchingPath, subJobControl);
        if(jobControl.isStopped()) return null;
        subJobControl.validate();
        return references;
    }

    private void filterReferencesOnMaxMatches(Map<String, String[]> references)
    {
        int maxMatches = getParameters().getMaxMatches();
        if(references != null && maxMatches > 0)
        {
            Iterator<String[]> iterator = references.values().iterator();
            String[] referenceValue;
            while(iterator.hasNext())
            {
                referenceValue = iterator.next();
                if(referenceValue != null && referenceValue.length > maxMatches)
                    iterator.remove();
            }
        }
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        if(getParameters().getSourceTypeObject() == null)
            throw new IllegalArgumentException("Please specify source type");
        if(getParameters().getTargetTypeObject() == null)
            throw new IllegalArgumentException("Please specify target type");
        if( !getParameters().getIdsColumnName().equals( ColumnNameSelector.NONE_COLUMN ) )
        {
            String columnName = getParameters().getIdsColumnName();
            TableDataCollection table = getParameters().getSourceTable().optDataElement( TableDataCollection.class );
            int idx = table.getColumnModel().optColumnIndex( columnName );
            if( idx == -1 )
                throw new IllegalArgumentException( "Input table does not contain the column specified." );
        }
    }
}
