package ru.biosoft.bsa.view;

/**
 * Returns a tag by view model if available
 * @author lan
 */
public interface ViewTagger
{
    public String getTag(Object model);
}
