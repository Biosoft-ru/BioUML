package biouml.plugins.research;

import ru.biosoft.util.bean.BeanInfoEx2;

public class NotePropertiesBeanInfo extends BeanInfoEx2<NoteProperties>
{
    public NotePropertiesBeanInfo()
    {
        super(NoteProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("text");
    }
}