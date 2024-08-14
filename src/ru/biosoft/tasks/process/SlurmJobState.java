package ru.biosoft.tasks.process;

public enum SlurmJobState
{
    //From man squeue:
    
    CA,//  CANCELLED       Job was explicitly cancelled by the user or system administrator.  The job may or may not have been initiated.

    CD,//  COMPLETED       Job has terminated all processes on all nodes.

    CF,//  CONFIGURING     Job has been allocated resources, but are waiting for them to become ready for use (e.g. booting).

    CG,//  COMPLETING      Job is in the process of completing. Some processes on some nodes may still be active.

    F,//   FAILED          Job terminated with non-zero exit code or other failure condition.

    NF,//  NODE_FAIL       Job terminated due to failure of one or more allocated nodes.

    PD,//  PENDING         Job is awaiting resource allocation.

    PR,//  PREEMPTED       Job terminated due to preemption.

    R,//   RUNNING         Job currently has an allocation.

    S,//   SUSPENDED       Job has an allocation, but execution has been suspended.

    TO,//  TIMEOUT         Job terminated upon reaching its time limit.
}
