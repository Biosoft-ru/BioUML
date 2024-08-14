package biouml.plugins.keynodes;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class DirectionEditor extends GenericComboBoxEditor
{
    public static final String UPSTREAM = "Upstream";
    public static final String DOWNSTREAM = "Downstream";
    public static final String BOTH = "Both";

    @Override
    public String[] getAvailableValues()
    {
        return getAvailableValuesStatic(isDirectionBothAvailable(getBean()));
    }

    public static String[] getAvailableValuesStatic(boolean isDirectionBothAvailable)
    {
        return isDirectionBothAvailable ? new String[] {UPSTREAM, DOWNSTREAM, BOTH} : new String[] {UPSTREAM, DOWNSTREAM};
    }

    private boolean isDirectionBothAvailable(Object bean)
    {
        try
        {
            return (Boolean)bean.getClass().getMethod("isDirectionBothAvailable").invoke(bean);
        }
        catch( Exception e )
        {
            return false;
        }
    }
}