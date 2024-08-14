package ru.biosoft.bsa.analysis;

import java.util.Vector;

/**
 * An utility class for profiling analysis method of sequences.
 * @pending more detailed comments
 * @pending sequence diagram illustrating the methods call order
 */
public class SequenceAnalysisProfiler
{
    /** An analysis name */
    protected String name;

    public SequenceAnalysisProfiler(String name)
    {
        this.name = name;
    }

    public SequenceAnalysisProfiler()
    {
        this("unknown");
    }

    public String makeReport()
    {
        String ln = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer(ln + ln + "Profiler report for " + name + ln);

        // make a header
        sb.append("#\tSequence\tLength\t#Sites\tTotal\tAnalys.\tCreat.\tWriting\tOther" + ln);

        // the summary statistics
        long tLength , tSiteNumber, tTotalTime, tAnalysisTime, tCreationTime, tWritingTime;
        long other, tOther = 0;
        tLength = tSiteNumber = tTotalTime = tAnalysisTime = tCreationTime = tWritingTime = tOther = 0;

        // the table body
        for(int i=0; i<sequences.size(); i++)
        {
            TimePerSequence tps = sequences.get(i);
            other = tps.getTotalTime() -  tps.getAnalysisTime() - tps.getCreationTime() - tps.getWritingTime();

            sb.append("" + (i+1));
            sb.append("\t" + tps.name);

            sb.append("\t" + tps.length);               tLength       += tps.length;
            sb.append("\t" + tps.siteNumber);           tSiteNumber   += tps.siteNumber;
            sb.append("\t" + tps.getTotalTime());       tTotalTime    += tps.getTotalTime();
            sb.append("\t" + tps.getAnalysisTime());    tAnalysisTime += tps.getAnalysisTime();
            sb.append("\t" + tps.getCreationTime());    tCreationTime += tps.getCreationTime();
            sb.append("\t" + tps.getWritingTime());     tWritingTime  += tps.getWritingTime();
            sb.append("\t" + other);                    tOther        += other;

            sb.append(ln);
        }

        // print summary statistics
        sb.append("\tTOTAL");
        sb.append("\t" + tLength);
        sb.append("\t" + tSiteNumber);
        sb.append("\t" + tTotalTime);
        sb.append("\t" + tAnalysisTime);
        sb.append("\t" + tCreationTime);
        sb.append("\t" + tWritingTime);
        sb.append("\t" + tOther);
        sb.append(ln);

        sb.append("total time: " + getTotalTime() + "ms" + ln);

        return sb.toString();
    }

    public String makeReportHTML(String title)
    {
        StringBuffer sb = new StringBuffer("<HTML><BODY>" + title);

        // make a header
        sb.append("<TABLE><TR> " +
                     "<TD>#</TD>" +
                     "<TD><b>Sequence</b></TD>" +
                     "<TD align=center><b>Length</TD>" +
                     "<TD align=center><b>#Site</TD>" +
                     "<TD align=center><b>Freq.*</TD>" +
                     "<TD width=10>&nbsp;</TD>" +
                     "<TD colspan=4 align=center><b>Analysis time (s)</b></TD></TR>" +
                     "<TR><TD colspan=6></TD><TD><i>Total</i></TD> <TD><i>Analysis</i></TD> <TD><i>Writing</i></TD> <TD><i>Other</i></TD> </TR>");
        // the summary statistics
        long tLength , tSiteNumber, tTotalTime, tAnalysisTime, tWritingTime;
        long other, tOther = 0;
        tLength = tSiteNumber = tTotalTime = tAnalysisTime = tWritingTime = tOther = 0;

        // the table body
        for(int i=0; i<sequences.size(); i++)
        {
            TimePerSequence tps = sequences.get(i);
            other = tps.getTotalTime() -  tps.getAnalysisTime() - tps.getWritingTime();

            sb.append("<TR>");
            sb.append("<TD align=right>" + (i+1) + " &nbsp;</TD>");
            sb.append("<TD>" + tps.name + "</TD>");

            sb.append("<TD align=right>" + tps.length            + "</TD>");  tLength       += tps.length;
            sb.append("<TD align=right>" + tps.siteNumber        + " &nbsp;</TD>");  tSiteNumber   += tps.siteNumber;
            
            String siteFreq = "N/A";
            if( tps.length != 0 )
                siteFreq = "" + (tps.siteNumber*1000/tps.length)/1000.0;
            sb.append("<TD>" + siteFreq + " &nbsp;</TD>");

            sb.append("<TD>&nbsp;</TD>");
            addTimeCell(sb, tps.getTotalTime());        tTotalTime    += tps.getTotalTime();
            addTimeCell(sb, tps.getAnalysisTime());     tAnalysisTime += tps.getAnalysisTime();
            addTimeCell(sb, tps.getWritingTime());      tWritingTime  += tps.getWritingTime();
            addTimeCell(sb, other);                     tOther        += other;

            sb.append("</TR>");
        }


        // print summary statistics
        sb.append("<TR><TD colspan=2 align=center><b>Total</b></TD>");
        sb.append("<TD align=right><b>" + tLength       + "</b></TD>");
        sb.append("<TD align=right><b>" + tSiteNumber   + "</b> &nbsp;</TD>");
        sb.append("<TD><b>" + (tSiteNumber*1000/tLength)/1000.0 + "</b> &nbsp;</TD>");

        sb.append("<TD>&nbsp;</TD>");
        addTimeCellBold(sb, tTotalTime);
        addTimeCellBold(sb, tAnalysisTime);
        addTimeCellBold(sb, tWritingTime);
        addTimeCellBold(sb, tOther);
        sb.append("</TR></TABLE>");

        sb.append("freq.* - frequency of sites per nucleotide");
        sb.append("</BODY></HTML>");

        return sb.toString();
    }

    private void addTimeCell(StringBuffer sb, long time)
    {
        sb.append("<TD align=right>" + time/1000.0 + " &nbsp;</TD>");
    }

    private void addTimeCellBold(StringBuffer sb, long time)
    {
        sb.append("<TD align=right><b>" + time/1000.0 + "</b> &nbsp;</TD>");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Total time routines
    //

    protected long startTime;
    protected long totalTime;

    /** @returns time when the sequence analysis was started. */
    public long getStartTime()
    {
        return startTime;
    }

    /** @returns total time of the sequence analysis. */
    public long getTotalTime()
    {
        return totalTime;
    }

    /**
     * This function should be called when the analyses is started.
     * Typically this function is called from the constructor,
     * but a developer has a possibility to specify other time.
     */
    public void start()
    {
        startTime = System.currentTimeMillis();
    }

    /** This function should be called when the whole sequence analyses is finished. */
    public void stop()
    {
        totalTime += System.currentTimeMillis() - startTime;
    }

    ////////////////////////////////////////////////////////////////////////////
    // One sequence analysis routines
    //

    /** The vector storing <code>TimePerSequence</code> items. */
    protected Vector<TimePerSequence> sequences = new Vector<>();

    /** The sequence that is currently under analysis. */
    protected TimePerSequence current = new TimePerSequence("unknown", -1);

    /**
     * This function should be called when new sequence analysis is started.
     * For each method call a new <code>TimePerSequence</code> item is created
     * and it is set up as a current.
     */
    public void startSequence(String name, int sequenceLength, int initialSiteNumber)
    {
        current = new TimePerSequence(name, sequenceLength);
        current.siteNumber = initialSiteNumber;
        sequences.add(current);
    }

    /** This function should be called when a current sequence analysis is stopped. */
    public void stopSequence(int siteNumber)
    {
        current.siteNumber = siteNumber - current.siteNumber;
        current.stop();
    }

    /**
     * This function should be called when the pure sequence analysis for the current
     * sequence is started.
     */
    public void startAnalysis()
    {
        current.startAnalysis();
    }

    /**
     * This function should be called when the pure sequence analysis for the current
     * sequence is finished.
     */
    public void stopAnalysis()
    {
        current.stopAnalysis();
    }

    /**
     * This function should be called when result structure creation for the current
     * sequence is started.
     */
    public void startCreation()
    {
        current.startCreation();
    }

    /**
     * This function should be called when result structure creation for the current
     * sequence is finished.
     */
    public void stopCreation()
    {
        current.stopCreation();
    }

    /**
     * This function should be called when the result writing for the current sequence is started.
     */
    public void startWriting()
    {
        current.startWriting();
    }

    /**
     * This function should be called when the result writing for the current
     * sequence is finished.
     */
    public void stopWriting()
    {
        current.stopWriting();
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * This class stores time intervals for one sequence analysis.
     * All the sequence analysis time is divided in following types:
     * <ul>
     * <li><i>analysisTime</i> - the pure analysis time</li>
     * <li><i>creationTime</i> - the time for result structures creation</li>
     * <li><i>writingTime</i>  - the time for result structures writing</li>
     * <li><i>totalTime</i>  - the total time for the sequence analysis</li>
     * </ul>
     */
    public static class TimePerSequence
    {
        /** The sequence name. */
        protected String name;

        /** The sequence length. */
        protected int length;

        /** Number of sites revealed during the analysis. */
        public int siteNumber;

        public TimePerSequence(String name, int sequenceLength)
        {
            this.name = name;
            this.length = sequenceLength;

            startTime = System.currentTimeMillis();
        }

        ///////////////////////////////////////////////////////////////////////
        // Total time routines
        //

        protected long startTime;
        protected long totalTime;

        /** @returns time when the sequence analysis was started. */
        public long getStartTime()
        {
            return startTime;
        }

        /** @returns total time of the sequence analysis. */
        public long getTotalTime()
        {
            return totalTime;
        }

        /**
         * This function is called when the sequence analysis is started.
         * Typically this function is called from the constructor,
         * but a developer has a possibility to specify other time of the sequence analysis.
         */
        public void start()
        {
            startTime = System.currentTimeMillis();
        }

        /** This function should be called when the whole sequence analysis is finished. */
        public void stop()
        {
            totalTime += System.currentTimeMillis() - startTime;
        }

        ///////////////////////////////////////////////////////////////////////
        // Analysis time routines
        //

        /** Time, when the pure analysis is started. */
        long analysisStart;

        /** The sequence analysis pure time. */
        long analysisTime;

        /** @returns pure time of the sequence analysis. */
        public long getAnalysisTime()
        {
            return analysisTime;
        }

        /** This function should be called when the pure sequence analysis is started. */
        public void startAnalysis()
        {
            analysisStart = System.currentTimeMillis();
        }

        /** This function should be called when the pure sequence analysis is finished. */
        public void stopAnalysis()
        {
            analysisTime += System.currentTimeMillis() - analysisStart;
        }

        ///////////////////////////////////////////////////////////////////////
        // Creation time routines
        //

        /** Time, when the data or result structure creation is started. */
        long creationStart;

        /** Time needed to data or result structure creation. */
        long creationTime;

        /** @returns time needed for data or result structure creation. */
        public long getCreationTime()
        {
            return creationTime;
        }

        /** This function should be called when the data or result structure creation is started. */
        public void startCreation()
        {
            creationStart = System.currentTimeMillis();
        }

        /**
         * This function should be called when the data or result structure creation is finished.
         */
        public void stopCreation()
        {
            creationTime += System.currentTimeMillis() - creationStart;
        }

        ///////////////////////////////////////////////////////////////////////
        // Writing time routines
        //

        /** Time, when the result writing is started. */
        long writingStart;

        /** Time needed to result writing. */
        long writingTime;

        /** @returns time needed for result writing. */
        public long getWritingTime()
        {
            return writingTime;
        }

        /** This function should be called when the result writing is started. */
        public void startWriting()
        {
            writingStart = System.currentTimeMillis();
        }

        /** This function should be called when the result writing is finished. */
        public void stopWriting()
        {
            writingTime += System.currentTimeMillis() - writingStart;
        }
    }
}
