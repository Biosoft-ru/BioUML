package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("collection of sequences")
@ClassIcon("resources/sequences.gif")
public interface SequenceCollection extends DataCollection<AnnotatedSequence>
{

}
