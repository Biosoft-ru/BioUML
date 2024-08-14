package ru.biosoft.bsa.macs14;

public class MACS14Profiler
{
    
    long startTime, endTime;

    public void start()
    {
        startTime = System.currentTimeMillis();
    }
    public void end()
    {
        endTime = System.currentTimeMillis();
    }

    long startReadingTime, endReadingTime;
    int inputTrackSize;

    public void startReadingTrack(int size)
    {
        startReadingTime = System.currentTimeMillis();
        inputTrackSize = size;
    }
    public void endReadingTrack()
    {
        endReadingTime = System.currentTimeMillis();
    }

    long startCalcTime, endCalcTime;

    public void startCalcStats()
    {
        startCalcTime = System.currentTimeMillis();
    }
    public void endCalcStats()
    {
        endCalcTime = System.currentTimeMillis();
    }

    long startPeakCall, endPeakCall;

    public void startPeakCall()
    {
        startPeakCall = System.currentTimeMillis();
    }
    public void endPeakCall()
    {
        endPeakCall = System.currentTimeMillis();
    }

    long startCalculatingFDR, endCalculatingFDR;

    public void startCalculatingFDR()
    {
        startCalculatingFDR = System.currentTimeMillis();
    }
    public void endCalculatingFDR()
    {
        endCalculatingFDR = System.currentTimeMillis();
    }

    long startStoringOutput, endStoringOutput;
    int outputTrackSize;

    public void startStoringOutput(int size)
    {
        startStoringOutput = System.currentTimeMillis();
        outputTrackSize = size;
    }
    public void endStoringOutput()
    {
        endStoringOutput = System.currentTimeMillis();
    }
    
    @Override
    public String toString()
    {
        return "MACS14 profile:\n" + "total time = " + (endTime - startTime) + "ms\n"
        + "reading track time = " + (endReadingTime - startReadingTime) + "ms\n"
        + "reading track speed = " + (((double)inputTrackSize) * 1000 / (endReadingTime - startReadingTime)) + " Site/s\n"
        + "calculating stats = " + (endCalcTime - startCalcTime) + "ms\n"
        + "calling peak = " + (endPeakCall - startPeakCall) + "ms\n"
        + "calculate FDR = " + (endCalculatingFDR - startCalculatingFDR) + "ms\n"
        + "store output = " + (endStoringOutput - startStoringOutput) + "ms\n"
        + "store output speed = " + (((double)outputTrackSize) * 1000 / (endStoringOutput - startStoringOutput)) + "Peak/s\n";
    }

}
