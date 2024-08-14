package ru.biosoft.access.repository;

import javax.swing.TransferHandler;

import ru.biosoft.access.core.DataElementPath;

public class DataElementImportTransferHandler extends TransferHandler
{
    private DataElementDroppable droppable;
    
    public DataElementImportTransferHandler(DataElementDroppable droppable)
    {
        this.droppable = droppable;
    }
    
    @Override
    public boolean canImport(TransferSupport support)
    {
        return support.isDataFlavorSupported(DataElementPathTransferable.getPathDataFlavor());
    }

    @Override
    public boolean importData(TransferSupport support)
    {
        if(!support.isDrop()) return false;
        if(!canImport(support)) return false;
        DataElementPath path;
        try
        {
            path = (DataElementPath)support.getTransferable().getTransferData(DataElementPathTransferable.getPathDataFlavor());
        }
        catch( Exception e )
        {
            return false;
        }
        return droppable.doImport(path, support.getDropLocation().getDropPoint());
    }
}
