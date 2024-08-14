package ru.biosoft.access.repository;

import java.awt.Point;

import ru.biosoft.access.core.DataElementPath;

public interface DataElementDroppable
{
    public boolean doImport(DataElementPath path, Point point);
}
