package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import biouml.standard.type.Species;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/matrices-to-molecules.gif")
public class SiteModelsToProteins extends SiteModelsToProteinsSupport<SiteModelsToProteinsParameters>
{
    public SiteModelsToProteins(DataCollection<?> origin, String name)
    {
        super(origin, name, new SiteModelsToProteinsParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        Set<String> nameList = new HashSet<>(getModels());
        String[] models = getParameters().getModels();
        if(models != null)
            nameList.retainAll(Arrays.asList(models));
        String[] matrixNames = nameList.toArray(new String[nameList.size()]);

        DataCollection<SiteModel> sitesLibrary = getParameters().getSiteModelsCollection().getDataCollection(SiteModel.class);
        jobControl.pushProgress(0, 40);
        log.info("Fetching factors...");
        Species allSpec = new Species( null, Species.ANY_SPECIES ) ;
        allSpec.setCommonName(Species.ANY_SPECIES );
        Species spec = getParameters().isIgnoreSpecies() ? allSpec : getParameters().getSpecies();
        Map<String, Set<Link>> factors = getFactors( sitesLibrary, matrixNames, spec );
        if(jobControl.isStopped()) return null;

        jobControl.popProgress();

        jobControl.pushProgress(40, 80);
        log.info("Converting...");
        ReferenceType referenceType = ReferenceTypeRegistry.getReferenceType(getParameters().getTargetType());
        new HashMap<>();
        Map<String, Set<String>> moleculesMap = new HashMap<>();
        List<Species> species = new ArrayList<>();
        if( getParameters().isIgnoreSpecies() )
        {
            DataCollection<Species> dc = Species.SPECIES_PATH.getDataCollection( Species.class );
            species = dc.stream().toList();
        }
        else
            species.add( getParameters().getSpecies() );
        for ( Species sp : species )
        {
            Map<String, String[]> molecules4Spec = getMolecules( factors, referenceType, sp );

            molecules4Spec.keySet().stream().forEach( key -> {
                Set<String> ms = Set.of( molecules4Spec.get( key ) );
                moleculesMap.computeIfAbsent( key, k -> new HashSet<String>() ).addAll( ms );
                return;
            } );
        }
        if(jobControl.isStopped()) return null;
        jobControl.popProgress();
        Map<String, String[]> molecules = moleculesMap.entrySet().stream().collect( Collectors.toMap( Map.Entry::getKey, entry -> entry.getValue().toArray( new String[0] ) ) );
        Map<String, Set<String>> references = revertReferences(molecules);

        jobControl.pushProgress(80, 99);
        log.info("Generating result...");
        TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(getParameters().getOutputTable());
        fillTable(getParameters().getSitesCollection().getDataElement(TableDataCollection.class), references, resTable);
        if(jobControl.isStopped())
        {
            resTable.getOrigin().remove(resTable.getName());
            return null;
        }
        jobControl.popProgress();
        resTable.finalizeAddition();
        ReferenceTypeRegistry.setCollectionReferenceType(resTable, referenceType.getClass());
        getParameters().getOutputTable().save(resTable);
        return resTable;
    }
}
