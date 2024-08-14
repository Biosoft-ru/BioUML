package ru.biosoft.plugins.docker;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

import ru.biosoft.access.core.ClassIcon;

@ClassIcon( "resources/logo-jupyter.png" )
public class JupyterKernelDataElement extends DataElementSupport
{
    public JupyterKernelDataElement(String name, DataCollection<?> origin)
    {
        super(name, origin);
    }
}
