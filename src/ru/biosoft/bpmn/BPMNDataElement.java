package ru.biosoft.bpmn;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;

@ClassIcon ( "resources/bpmn_diagram.png" )
@PropertyName ( "bpmn diagram" )
public class BPMNDataElement extends TextDataElement
{

    public BPMNDataElement(String name, DataCollection<?> origin)
    {
        super( name, origin );
    }

    public BPMNDataElement(String name, DataCollection<?> origin, String content)
    {
        super( name, origin, content );
    }

}
