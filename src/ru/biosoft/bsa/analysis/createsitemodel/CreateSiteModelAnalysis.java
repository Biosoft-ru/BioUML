package ru.biosoft.bsa.analysis.createsitemodel;

import javax.annotation.Nonnull;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.JavaScriptBSA;
import ru.biosoft.bsa.transformer.SiteModelTransformer;

/**
 * Analysis that construct SiteModel based on WeightMatrix.
 * @author ivan
 */
public abstract class CreateSiteModelAnalysis<T extends CreateSiteModelParameters> extends AnalysisMethodSupport<T>
{
    public CreateSiteModelAnalysis(DataCollection<?> origin, String name, T parameters)
    {
        super(origin, name, JavaScriptBSA.class, parameters);
    }
    
    @Override
    public SiteModel justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        DataElementPath outputCollectionPath = parameters.getOutputCollection();
        if(! (outputCollectionPath.exists()))
        {
            log.info("No site model collection, creating");
            DataCollection dc = SiteModelTransformer.createCollection(outputCollectionPath);
            outputCollectionPath.save(dc);
        }
        
        DataCollection<?> outputCollection = outputCollectionPath.getDataCollection();
        
        SiteModel model = createModel(parameters.getModelName(), outputCollection);
        CollectionFactoryUtils.save(model);
        
        return model;
    }
    
    protected abstract @Nonnull SiteModel createModel(String name, DataCollection<?> origin) throws Exception;
    
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths(parameters.getInputNames(), new String[] {"outputCollection"});
        checkNotEmpty("modelName");
    }
}
