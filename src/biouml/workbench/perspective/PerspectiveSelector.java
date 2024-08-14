package biouml.workbench.perspective;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class PerspectiveSelector extends GenericComboBoxEditor
{
    @Override
    protected String[] getAvailableValues()
    {
        return PerspectiveRegistry.perspectives().map( Perspective::getTitle ).toArray( String[]::new );
    }
}