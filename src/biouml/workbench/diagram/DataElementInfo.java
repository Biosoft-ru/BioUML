package biouml.workbench.diagram;

import biouml.standard.type.Base;

import biouml.model.DiagramElement;
import biouml.model.Module;
import ru.biosoft.access.core.DataElement;

/**
 * Wrapper for data element for clipboard
 */
public class DataElementInfo
{
    public DataElementInfo(DataElement dataElement)
    {
        this.dataElement = dataElement;
    }

    @Override
    public String toString()
    {
        return "DiagramElementInfo, module=" + getModuleName() + ", name=" + getKernelName() + ", type=" + getKernelType() + ", title="
                + getKernelTitle();
    }

    ///////////////////////////////////////////////////////////////////
    // Properties
    //

    private DataElement dataElement;
    public DataElement getDataElement()
    {
        return dataElement;
    }

    public String getModuleName()
    {
        Module module = Module.optModule(dataElement);
        if( module != null )
        {
            return module.getName();
        }

        return null;
    }

    public Base getKernel()
    {
        if( dataElement instanceof DiagramElement )
        {
            return ( (DiagramElement)dataElement ).getKernel();
        }
        else if( dataElement instanceof Base )
        {
            return (Base)dataElement;
        }
        return null;
    }

    public String getKernelName()
    {
        return getKernel() == null ? getDataElement().getName() : getKernel().getName();
    }

    public String getKernelTitle()
    {
        return getKernel() == null ? null : getKernel().getTitle();
    }

    public String getKernelType()
    {
        return getKernel() == null ? null : getKernel().getType();
    }
}