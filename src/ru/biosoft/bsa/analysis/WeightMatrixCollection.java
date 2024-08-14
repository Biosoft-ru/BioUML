
package ru.biosoft.bsa.analysis;

import java.util.Properties;
import java.util.logging.Logger;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.transformer.WeightMatrixTransformer;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * TransformedDataCollection wrapper to store matrices library
 *
 */
@ClassIcon("resources/matrixlib.gif")
@PropertyName("matrix library")
public class WeightMatrixCollection extends TransformedDataCollection<Entry, FrequencyMatrix> implements CloneableDataElement
{
    public WeightMatrixCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        getInfo().getProperties().setProperty(DataCollectionConfigConstants.CAN_OPEN_AS_TABLE, String.valueOf(true));
    }

    @Override
    public DataCollection clone(DataCollection parent, String name) throws CloneNotSupportedException
    {
        try
        {
            DataElementPath path = DataElementPath.create(parent, name);
            WeightMatrixTransformer.createMatrixLibrary(path);
            DataCollection<DataElement> dc = path.getDataCollection();
            if(!DataCollectionUtils.checkPrimaryElementType(dc, WeightMatrixCollection.class))
            {
                path.remove();
                throw new Exception("Error while cloning");
            }
            for(FrequencyMatrix matrix: this)
            {
                dc.put(matrix.clone(dc, matrix.getName()));
            }
            DataCollectionUtils.copyAnalysisParametersInfo( this, dc );
            return dc;
        }
        catch( Exception e )
        {
            throw new CloneNotSupportedException(e.getMessage());
        }
    }

    public static DataCollection<FrequencyMatrix> createMatrixLibrary(DataElementPath library, Logger log) throws Exception
    {
        if(!library.exists())
        {
            log.info("No matrix library found: creating...");
            WeightMatrixTransformer.createMatrixLibrary(library);
        }
        DataCollection<FrequencyMatrix> libraryDE = library.getDataCollection(FrequencyMatrix.class);
        if(!libraryDE.isAcceptable(FrequencyMatrix.class))
            throw new IllegalArgumentException("Specified matrix library has invalid type");
        return libraryDE;
    }
}
