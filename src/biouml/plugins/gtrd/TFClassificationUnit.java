package biouml.plugins.gtrd;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.classification.ClassificationUnitAsSQL;

public class TFClassificationUnit extends ClassificationUnitAsSQL
{
    public TFClassificationUnit(DataCollection<?> parent, Properties properties)
    {
        super( parent, properties );
    }
}
