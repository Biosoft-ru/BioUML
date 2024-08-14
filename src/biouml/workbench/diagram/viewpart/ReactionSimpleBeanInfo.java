package biouml.workbench.diagram.viewpart;

import biouml.model.util.ReactionEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ReactionSimpleBeanInfo extends BeanInfoEx2<ReactionSimple>
{
    public ReactionSimpleBeanInfo()
    {
        super(ReactionSimple.class);
        this.setBeanEditor( ReactionEditor.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly("name");
        add("title");
        add("formula");
        add("reversible");
        add("fast");
        add("comment");
    }
}