package ru.biosoft.gui;

import com.developmentontheedge.beans.undo.Transactable;

/**
 * Interface to be implimented by editor for some part of data.
 */
public interface EditorPart extends Transactable, ViewPart
{
    /** Save changes into model. */
    public void save();
}
