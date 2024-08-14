package ru.biosoft.bsa.server;

import static ru.biosoft.util.j2html.TagCreator.div;
import static ru.biosoft.util.j2html.TagCreator.span;

import org.json.JSONArray;

import com.developmentontheedge.beans.model.ComponentFactory;

import ru.biosoft.bsa.project.Project;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.util.ControlCodeGenerator;
import ru.biosoft.util.Util;
import ru.biosoft.util.j2html.tags.Tag;

public class ProjectControlCodeGenerator implements ControlCodeGenerator
{
    @Override
    public Tag<?> getControlCode(Object value) throws Exception
    {
        long uid = Util.getUniqueId();
        JSONArray jsonProperties = JSONUtils.getModelAsJSON( ComponentFactory.getModel( value ) );
        return div().withId( "viewer_" + uid ).with( span().withClass( "table_script_node" )
                .withText( "showGenomeBrowser('viewer_" + uid + "', " + jsonProperties.toString() + ")" ) );

    }

    @Override
    public Class<?> getSupportedItemType()
    {
        return Project.class;
    }

}
