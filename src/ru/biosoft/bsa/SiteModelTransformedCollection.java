package ru.biosoft.bsa;

import java.util.Properties;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.transformer.SiteModelTransformer;

public class SiteModelTransformedCollection extends TransformedDataCollection<Entry, SiteModel> implements SiteModelCollection, CloneableDataElement
{
    public SiteModelTransformedCollection(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);
        getInfo().getProperties().setProperty(DataCollectionConfigConstants.CAN_OPEN_AS_TABLE, String.valueOf(true));
    }

    @Override
    public DataCollection<?> clone(DataCollection parent, String name) throws CloneNotSupportedException
    {
        try
        {
            DataElementPath path = DataElementPath.create(parent, name);
            return cloneSiteModelCollection(this, path);
        }
        catch( Exception e )
        {
            throw new CloneNotSupportedException(e.getMessage());
        }
    }

    public static DataCollection<?> cloneSiteModelCollection(SiteModelCollection source, DataElementPath path) throws Exception
    {

        DataCollection<SiteModel> dc = SiteModelTransformer.createCollection(path);
        for(SiteModel model: source)
        {
            dc.put(model.clone(dc, model.getName()));
        }
        DataCollectionUtils.copyAnalysisParametersInfo( source, dc );
        path.save(dc);
        return dc;
    }
}
