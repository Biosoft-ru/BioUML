package ru.biosoft.bsa.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
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
        Map<String, Set<Link>> factors = getFactors(sitesLibrary, matrixNames, getParameters().getSpecies());
        if(jobControl.isStopped()) return null;

        jobControl.popProgress();

        jobControl.pushProgress(40, 80);
        log.info("Converting...");
        ReferenceType referenceType = ReferenceTypeRegistry.getReferenceType(getParameters().getTargetType());
        Map<String, String[]> molecules = getMolecules(factors, referenceType, getParameters().getSpecies());
        if(jobControl.isStopped()) return null;
        jobControl.popProgress();

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
