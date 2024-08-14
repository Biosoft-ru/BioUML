package biouml.plugins.ensembl.access;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SequenceCollection;

/**
 * @author lan
 *
 */
public class EnsemblSequenceCollection extends SqlDataCollection<AnnotatedSequence> implements SequenceCollection
{
    public EnsemblSequenceCollection(DataCollection parent, Properties properties) throws LoggedException
    {
        super(parent, properties);
    }
}
