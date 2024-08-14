package ru.biosoft.access.repository;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import ru.biosoft.access.core.DataElementPath;

public class DataElementPathTransferable implements Transferable
{
    private static final DataFlavor pathFlavor = new DataFlavor(DataElementPath.class, DataFlavor.javaJVMLocalObjectMimeType+";class="+ru.biosoft.access.core.DataElementPath.class.getName());
    private Transferable origin;
    private DataElementPath path;

    public DataElementPathTransferable(DataElementPath path)
    {
        origin = new StringSelection(path.toString());
        this.path = path;
    }
    
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        if(flavor.equals(pathFlavor))
            return path;
        return origin.getTransferData(flavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        DataFlavor[] originFlavors = origin.getTransferDataFlavors();
        DataFlavor[] result = new DataFlavor[originFlavors.length+1];
        result[0] = pathFlavor;
        System.arraycopy(originFlavors, 0, result, 1, originFlavors.length);
        return result;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return flavor.equals(pathFlavor) || origin.isDataFlavorSupported(flavor);
    }

    public static DataFlavor getPathDataFlavor()
    {
        return pathFlavor;
    }
}
