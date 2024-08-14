package biouml.workbench.diagram;

import java.util.ListResourceBundle;

import javax.swing.Action;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                //--- Actions ---------------------------------------------------/
                {LineVertexTypeAction.KEY + Action.SMALL_ICON, "vertexType.gif"},
                {LineVertexTypeAction.KEY + Action.NAME, "Line"},
                {LineVertexTypeAction.KEY + Action.SHORT_DESCRIPTION, "Edit type of vertex in the path"},
                {LineVertexTypeAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-line-type"},

                {QuadricVertexTypeAction.KEY + Action.SMALL_ICON, "vertexType.gif"},
                {QuadricVertexTypeAction.KEY + Action.NAME, "Quadric"},
                {QuadricVertexTypeAction.KEY + Action.SHORT_DESCRIPTION, "Edit type of vertex in the path"},
                {QuadricVertexTypeAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-quadric-type"},

                {CubicVertexTypeAction.KEY + Action.SMALL_ICON, "vertexType.gif"},
                {CubicVertexTypeAction.KEY + Action.NAME, "Cubic"},
                {CubicVertexTypeAction.KEY + Action.SHORT_DESCRIPTION, "Edit type of vertex in the path"},
                {CubicVertexTypeAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-cubic-type"},

                {RemoveVertexAction.KEY + Action.SMALL_ICON, "removeVertex.gif"},
                {RemoveVertexAction.KEY + Action.NAME, "Remove vertex"},
                {RemoveVertexAction.KEY + Action.SHORT_DESCRIPTION, "Remove selected vertex from the path"},
                {RemoveVertexAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-remove-vertex"},

                {AddVertexAction.KEY + Action.SMALL_ICON, "addVertex.gif"},
                {AddVertexAction.KEY + Action.NAME, "Add vertex"},
                {AddVertexAction.KEY + Action.SHORT_DESCRIPTION, "Add new vertex to the path"},
                {AddVertexAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-add-vertex"},

                {RotateNodeAction.KEY + Action.SMALL_ICON, "rotate.gif"},
                {RotateNodeAction.KEY + Action.NAME, "Rotate"},
                {RotateNodeAction.KEY + Action.SHORT_DESCRIPTION, "Rotate node clockwise"},
                {RotateNodeAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-rotate"},

                { SetInitialValuesAction.KEY + Action.SMALL_ICON           , "setValues.gif"},
                { SetInitialValuesAction.KEY + Action.NAME                 , "Set initial values"},
                { SetInitialValuesAction.KEY + Action.SHORT_DESCRIPTION    , "Set initial values"},
                { SetInitialValuesAction.KEY + Action.ACTION_COMMAND_KEY   , "cmd-gnrt-dgr"},
        };
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the string is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable t )
        {
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}