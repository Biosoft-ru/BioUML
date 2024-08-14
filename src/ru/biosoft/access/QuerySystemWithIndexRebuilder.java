package ru.biosoft.access;

import java.io.IOException;

import javax.swing.JFrame;

import ru.biosoft.access.core.QuerySystem;

/**
 * Query system which can check whether indexes present and allows to rebuild indexes
 */
public interface QuerySystemWithIndexRebuilder extends QuerySystem
{
    /**
     * Test if this query system has indexes
     * @return
     * @throws IOException
     */
    public boolean testHaveIndex() throws IOException;
    
    public void showRebuildIndexesUI(JFrame parent) throws Exception;

}
