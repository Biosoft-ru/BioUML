package ru.biosoft.access;

import java.io.File;

import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Control process of creating data collection.
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */
public interface CreateDataCollectionController
{
    /** Constant for cancel operation. */
    int CANCEL        = 0;
    /** Constant for enable overwrite one file. */
    int OVERWRITE_ONE = 1;
    /** Constant for enable overwrite all files. */
    int OVERWRITE_ALL = 2;
    /** Constant indicating that all old files should be removed. */
    int REMOVE_ALL    = 3;

    /**
     * Control of creating data collection when file overwriting needed.
     * @param file File which will be overwritten.
     * @return Constant for control creating of data collection.
     * @see #CANCEL
     * @see #OVERWRITE_ONE
     * @see #OVERWRITE_ALL
     */
    int fileAlreadyExists( File file );

    int getLastAnswer();

    /**
     * Set job control for process of collection creating.
     * @param jc Job control.
     */
    void setJobControl( FunctionJobControl jc );

    /**
     * Returns job control for process of collection creating.
     * @return Job control for process of collection creating.
     */
    FunctionJobControl getJobControl();


}