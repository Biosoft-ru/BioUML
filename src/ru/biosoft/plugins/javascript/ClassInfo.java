package ru.biosoft.plugins.javascript;

import ru.biosoft.access.core.DataCollection;

/**
 *
 * @todo implimentation
 */
public class ClassInfo extends HostObjectInfo
{
    /**
     * Creates info for the specified Java class.
     *
     * @param name - class name
     * @param parent - <code>DataCollection</code> to which this class info belongs.
     * Generally it is <code>plugins/Javascript/classes</code>.
     */
    public ClassInfo(String name, DataCollection parent)
    {
        super(name, parent);
    }
}
