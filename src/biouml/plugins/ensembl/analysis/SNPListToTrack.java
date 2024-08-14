package biouml.plugins.ensembl.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysis.type.GeneTableType;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.bsa.VariationElement;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.analysis.JavaScriptBSA;
import ru.biosoft.bsa.snp.SNPTableType;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.TextUtil;

@ClassIcon("resources/snp-to-track.gif")
public class SNPListToTrack extends AnalysisMethodSupport<SNPListToTrackParameters>
{
    public SNPListToTrack(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new SNPListToTrackParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkGreater("threePrimeSize", 0);
        checkGreater("fivePrimeSize", 0);
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        final int fivePrimeFlankSize = parameters.getFivePrimeSize();
        final int threePrimeFlankSize = parameters.getThreePrimeSize();
        final int maxAdditionalSize = Math.max(fivePrimeFlankSize, threePrimeFlankSize);
        EnsemblDatabase ensembl = parameters.getEnsembl();
        final DataCollection<VariationElement> variationCollection = ensembl.getVariationCollection();
        final Track geneTrack = ensembl.getGenesTrack();
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, parameters.getDestPath().getName());
        //properties.setProperty(SqlTrack.LABEL_PROPERTY, VariationElement.VARIATION_NAME);
        properties.setProperty(DataCollectionUtils.SPECIES_PROPERTY, ensembl.getSpecie().getLatinName());
        DataCollection<DataElement> outputCollection = parameters.getDestPath().getParentCollection();
        parameters.getDestPath().remove();
        final WritableTrack result = TrackUtils.createTrack( outputCollection, properties, VCFSqlTrack.class );
        final int[] statuses = new int[4];
        final TableDataCollection resultTable = new StandardTableDataCollection(null, "result");
        ColumnModel model = resultTable.getColumnModel();
        model.addColumn("Ensembl ID", String.class);
        model.addColumn("Gene symbol", String.class);
        model.addColumn("Location", String.class);
        model.addColumn("SNP_matching-Chromosome", String.class);
        model.addColumn("SNP_matching-Position", Integer.class);
        model.addColumn("SNP_matching-Allele", String.class);
        model.addColumn("SNP_matching-Strand", String.class);
        final Map<String, List<Double>> genes = new HashMap<>();
        final int columnIndex = ColumnNameSelector.NONE_COLUMN.equals( parameters.getColumn() ) ? -1
                : parameters.getSource().getColumnModel().optColumnIndex( parameters.getColumn() );
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        jobControl.pushProgress(0, 90);
        jobControl.forCollection(parameters.getSource().getNameList(), new Iteration<String>()
        {
            private boolean firstSite = true;

            private Object[] getGeneAnnotation(String snpName, Site s, VariationElement variation)
            {
                Object[] values = new Object[3];
                int score = Integer.MIN_VALUE;
                try
                {
                    DataCollection<Site> sites = geneTrack.getSites(s.getOriginalSequence().getOrigin().getCompletePath().toString(), s.getFrom() - maxAdditionalSize,
                            s.getTo() + maxAdditionalSize);
                    if(sites.getSize() > 0)
                    {
                        Interval snpInterval = new Interval(s.getFrom(), s.getTo());
                        // TODO: select all of genes
                        for(Site gene: sites)
                        {
                            Object[] curValues = new Object[3];
                            Object symbol = gene.getProperties().getValue("symbol");
                            curValues[1] = symbol == null?null:symbol.toString();   // Symbol
                            Object id = gene.getProperties().getValue("id");
                            if(id != null)
                            {
                                curValues[0] = id.toString();
                                if(columnIndex != -1)
                                {
                                    List<Double> list = genes.get(id.toString());
                                    if( list == null )
                                        list = new ArrayList<>();
                                    try
                                    {
                                        Object valueObj = (parameters.getSource().get(snpName)).getValues()[columnIndex];
                                        Double value = valueObj instanceof Double?(Double)valueObj:Double.valueOf(valueObj.toString());
                                        list.add(value);
                                    }
                                    catch( Exception e )
                                    {
                                    }
                                    genes.put(id.toString(), list);
                                } else
                                    genes.put(id.toString(), new ArrayList<Double>());
                            }
                            int curScore = Integer.MIN_VALUE;
                            Interval fivePrimeInterval = (new Interval(-fivePrimeFlankSize,-1)).translateFromSite(gene);
                            if(fivePrimeInterval.intersects(snpInterval))
                            {
                                curValues[2] = "5' region (promoter)";
                                curScore = -Math.min(Math.abs(snpInterval.getFrom()-gene.getStart()), Math.abs(snpInterval.getTo()-gene.getStart()));
                            } else
                            {
                                Interval threePrimeInterval = (new Interval(gene.getLength(), gene.getLength()+threePrimeFlankSize-1)).translateFromSite(gene);
                                if(threePrimeInterval.intersects(snpInterval))
                                {
                                    curValues[2] = "3' region";
                                    int geneEnd = gene.getStrand()==StrandType.STRAND_MINUS?gene.getFrom():gene.getTo();
                                    curScore = -Math.min(Math.abs(snpInterval.getFrom()-geneEnd), Math.abs(snpInterval.getTo()-geneEnd));
                                }
                                else
                                {
                                    Interval geneInterval = new Interval(gene.getFrom(), gene.getTo());
                                    if(geneInterval.intersects(snpInterval))
                                    {
                                        curValues[2] = "Gene";
                                        Object exonsObj = gene.getProperties().getValue("exons");
                                        if(exonsObj != null)
                                        {
                                            curValues[2] = "Intron";
                                            curScore = 1;
                                            for( String blockStr : TextUtil.split( exonsObj.toString(), ';' ) )
                                            {
                                                Interval interval;
                                                try
                                                {
                                                    interval = new Interval(blockStr);
                                                }
                                                catch( Exception e )
                                                {
                                                    continue;
                                                }
                                                interval = interval.translateFromSite(gene);
                                                if(interval.intersects(snpInterval))
                                                {
                                                    curValues[2] = "Exon";
                                                    curScore = 2;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if(curScore > score)
                            {
                                score = curScore;
                                values = curValues;
                            }
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "While obtaining genes for site "+s.getProperties().getValue(VariationElement.NAME_DESCRIPTOR.getName()), e);
                }
                Object[] fullValues = new Object[7];
                System.arraycopy(values, 0, fullValues, 0, values.length);
                fullValues[3] = s.getSequence().getName();
                fullValues[4] = s.getFrom();
                fullValues[5] = variation.getAllele();
                fullValues[6] = s.getStrand() == StrandType.STRAND_MINUS ? "-" : "+";
                return fullValues;
            }


            @Override
            public boolean run(String snpName)
            {
                try
                {
                    VariationElement variation = variationCollection.get(snpName);
                    if(variation == null)
                    {
                        statuses[0]++;
                    } else
                    {
                        Site s = variation.getSite();
                        if( s.getOriginalSequence() == null )
                        {
                            statuses[0]++;
                        }
                        else
                        {
                            s = VariationElement.extendInsertionSite( s );
                            if( firstSite && result instanceof DataCollection )
                            {
                                ( (DataCollection<?>)result ).getInfo().getProperties().setProperty( Track.SEQUENCES_COLLECTION_PROPERTY,
                                        s.getSequence().getOrigin().getOrigin().getCompletePath().toString() );
                                firstSite = false;
                            }
                            Object[] values = getGeneAnnotation( snpName, s, variation );

                            try
                            {
                                if( values[2] != null || parameters.isOutputNonMatched() )
                                {
                                    TableDataCollectionUtils.addRow( resultTable, variation.getName(), values, true );
                                    result.addSite( s );
                                    statuses[1]++;
                                }
                                else
                                {
                                    statuses[3]++;
                                }
                            }
                            catch( Exception e )
                            {
                                log.log(Level.SEVERE,  "While adding site " + variation.getName(), e );
                                statuses[2]++;
                            }
                        }
                    }
                }
                catch(Exception e)
                {
                    statuses[2]++;
                    log.log( Level.SEVERE, e.getMessage(), e );
                }
                return true;
            }
        });
        if(jobControl.isStopped())
        {
            parameters.getDestPath().remove();
            return null;
        }
        resultTable.finalizeAddition();
        result.finalizeAddition();
        CollectionFactoryUtils.save(result);
        jobControl.popProgress();

        jobControl.pushProgress(90, 95);
        TableDataCollection annotatedTable = TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, resultTable, parameters.getSource(),
                parameters.getAnnotatedPath());
        annotatedTable.setReferenceType(ReferenceTypeRegistry.getReferenceType(SNPTableType.class).toString());
        parameters.getAnnotatedPath().save(annotatedTable);
        jobControl.setPreparedness(100);
        jobControl.popProgress();

        jobControl.pushProgress(95, 100);
        createGenesTable(genes, columnIndex);
        jobControl.popProgress();

        log.info("Successfully converted: "+statuses[1]);
        log.info("Unknown SNPs: "+statuses[0]);
        if(!parameters.isOutputNonMatched())
            log.info("SNPs not matched to genes: "+statuses[3]);
        log.info("Errors during conversion: "+statuses[2]);
        return result;
    }

    private void createGenesTable(final Map<String, List<Double>> genes, final int columnIndex) throws Exception
    {
        TableDataCollection genesTable = TableDataCollectionUtils.createTableDataCollection(parameters.getOutputGenes());
        if(columnIndex > -1)
        {
            genesTable.getColumnModel().addColumn(parameters.getSource().getColumnModel().getColumn(columnIndex).getName(), Double.class);
        }
        for( Entry<String, List<Double>> geneEntry : genes.entrySet() )
        {
            Object[] values;
            if( columnIndex == -1 )
            {
                values = new Object[0];
            }
            else
            {
                values = new Object[1];
                try
                {
                    double[] vals = DoubleStreamEx.of(geneEntry.getValue()).toArray();
                    values[0] = parameters.getAggregator().aggregate(vals);
                }
                catch( Exception e )
                {
                }
            }
            TableDataCollectionUtils.addRow(genesTable, geneEntry.getKey(), values, true);
        }
        genesTable.finalizeAddition();
        ReferenceTypeRegistry.setCollectionReferenceType(genesTable, GeneTableType.class);
        CollectionFactoryUtils.save(genesTable);
        jobControl.setPreparedness(100);
    }
}
