package ru.biosoft.gui;

/**
 * Utility interface that can be used to define whether tsome data can be explored
 * by corresponding view or editor part.
 */
public interface ModelValidator
{
    public boolean canExplore(Object model, ViewPart part);
}
