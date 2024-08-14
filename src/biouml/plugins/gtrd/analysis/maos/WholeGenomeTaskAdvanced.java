package biouml.plugins.gtrd.analysis.maos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import biouml.plugins.enrichment.FunctionalHubConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.analysis.maos.ChrTask;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.WholeGenomeTask;

public class WholeGenomeTaskAdvanced extends WholeGenomeTask
{

    public WholeGenomeTaskAdvanced(AdvancedParameters parameters, Logger analysisLog, AnalysisJobControl progress)
    {
        super( parameters, analysisLog, progress );
    }
    
    @Override
    protected IResultHandler createResultHandler()
    {
       return new ResultHandlerAdvanced( getParameters(), analysisLog );
    }
    
    @Override
    protected ChrTask createChrTask(DataElementPath chrPath)
    {
        return new ChrTaskAdvanced(chrPath, getParameters(), resultHandler, analysisLog, gtrdMetadata, uniprot2pathway, uniprot2disease);
    }

    private AdvancedParameters getParameters()
    {
        return (AdvancedParameters)parameters;
    }
    
    private GTRDMetadata gtrdMetadata;
    
    @Override
    public Object[] run() throws Exception
    {
        progress.pushProgress( 0, 5 );
        gtrdMetadata = new GTRDMetadata( getParameters(), progress, analysisLog );
        progress.popProgress();
        
        progress.pushProgress( 5, 10 );
        if(getParameters().isAddTranspathAnnotation())
        {
            analysisLog.info( "Loading transpath pathways." );
            loadTranspathPathways();
            analysisLog.info( "Transpath pathways loaded." );
        }
        progress.popProgress();
        
        progress.pushProgress( 10, 15 );
        if(getParameters().isAddPsdAnnotation())
        {
            analysisLog.info( "Loading HumanPSD." );
            loadHumanPSD();
            analysisLog.info( "HumanPSD loaded." );
        }
        progress.popProgress();

        progress.pushProgress( 15, 100 );
        Object[] result = super.run();
        progress.popProgress();
        
        return result;
    }

   
    private Map<String, Set<String>> uniprot2pathway = new HashMap<>();
    private void loadTranspathPathways()
    {
        Set<String> uniprots = gtrdMetadata.uniprot2tfClass.keySet();

        Properties inputProps = new Properties();
        inputProps.setProperty( BioHub.TYPE_PROPERTY, "Proteins: UniProt" );
        inputProps.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );

        Properties outputProps = new Properties();
        outputProps.setProperty( BioHub.TYPE_PROPERTY, "Genes: Ensembl" );
        outputProps.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        
        Map<String, String[]> uniprotToEns = BioHubRegistry.getReferences( uniprots.toArray(new String[0]), inputProps, outputProps, null );
        
        Set<String> ensemblGenes = new HashSet<>();
        for(String[] ids : uniprotToEns.values())
            for(String id : ids)
                ensemblGenes.add( id );
        
        BioHubInfo transpathHubInfo = getParameters().getTranspathHub();
        if(transpathHubInfo == null)
            return;
        BioHub transpathHub = transpathHubInfo.getBioHub();
        
        CollectionRecord collection = new CollectionRecord(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD, true);
        CollectionRecord ensembl = new CollectionRecord(getParameters().getEnsemblPath(), true);
        TargetOptions dbOptions = new TargetOptions(collection, ensembl);
        
        Element[] elements = new Element[ensemblGenes.size()];
        int i = 0;
        for(String ensId : ensemblGenes)
            elements[i++] = new Element("stub/%//"+ensId) ;
        Map<Element, Element[]> mapping = transpathHub.getReferences(elements, dbOptions, null, 1, -1);//gene2pathway

        Map<String, String[]> gene2Pathway = new HashMap<>();
        mapping.forEach( (gene,pathways) -> {
            if(pathways == null || pathways.length == 0)
                return;
            String geneId = gene.getAccession();
            String[] pathwayIds = new String[pathways.length];
            for(int j = 0; j < pathways.length; j++)
            {
                String pathwayId = pathways[j].getAccession();
                pathwayIds[j] = pathwayId;
            }
            gene2Pathway.put( geneId, pathwayIds );
        } );
        
        
        for(String uniprot : uniprots)
        {
            String[] ensIds = uniprotToEns.get( uniprot );
            if(ensIds == null)
                continue;
            for(String ensId : ensIds)
            {
                String[] pathways = gene2Pathway.get( ensId );
                if(pathways == null)
                    continue;
                for(String pathway : pathways)
                {
                    uniprot2pathway.computeIfAbsent( uniprot, k->new HashSet<>() ).add( pathway );
                }
            }
        }
    }
    
    
    
    private Map<String, Set<String>> uniprot2disease = new HashMap<>();
    private void loadHumanPSD()
    {
        Set<String> uniprots = gtrdMetadata.uniprot2tfClass.keySet();

        Properties inputProps = new Properties();
        inputProps.setProperty( BioHub.TYPE_PROPERTY, "Proteins: UniProt" );
        inputProps.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );

        Properties outputProps = new Properties();
        outputProps.setProperty( BioHub.TYPE_PROPERTY, "Genes: Ensembl" );
        outputProps.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        
        Map<String, String[]> uniprotToEns = BioHubRegistry.getReferences( uniprots.toArray(new String[0]), inputProps, outputProps, null );
        
        Set<String> ensemblGenes = new HashSet<>();
        for(String[] ids : uniprotToEns.values())
            for(String id : ids)
                ensemblGenes.add( id );
        
        BioHubInfo psdHubInfo = getParameters().getPsdHub();
        if(psdHubInfo == null)
            return;
        BioHub psdHub = psdHubInfo.getBioHub();
        
        CollectionRecord collection = new CollectionRecord(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD, true);
        CollectionRecord ensembl = new CollectionRecord(getParameters().getEnsemblPath(), true);
        TargetOptions dbOptions = new TargetOptions(collection, ensembl);
        
        Element[] elements = new Element[ensemblGenes.size()];
        int i = 0;
        for(String ensId : ensemblGenes)
            elements[i++] = new Element("stub/%//"+ensId) ;
        Map<Element, Element[]> mapping = psdHub.getReferences(elements, dbOptions, null, 1, -1);//gene2disease

        Map<String, String[]> gene2Disease = new HashMap<>();
        mapping.forEach( (gene,pathways) -> {
            if(pathways == null || pathways.length == 0)
                return;
            String geneId = gene.getAccession();
            String[] diseaseList = new String[pathways.length];
            for(int j = 0; j < pathways.length; j++)
            {
                String diseaseId = pathways[j].getAccession();
                diseaseList[j] = diseaseId;
            }
            gene2Disease.put( geneId, diseaseList );
        } );
        
        
        for(String uniprot : uniprots)
        {
            String[] ensIds = uniprotToEns.get( uniprot );
            if(ensIds == null)
                continue;
            for(String ensId : ensIds)
            {
                String[] diseaseList = gene2Disease.get( ensId );
                if(diseaseList == null)
                    continue;
                for(String disease : diseaseList)
                {
                    uniprot2disease.computeIfAbsent( uniprot, k->new HashSet<>() ).add( disease );
                }
            }
        }
    }
}