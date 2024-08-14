package biouml.plugins.hemodynamics;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.standard.simulation.ResultListener;

public class HemodynamicsObserver implements ResultListener
{

    public double error = 1E-6;
    public double searchTime = 0.8;
    private int cycleIndex = 0;
    ArterialBinaryTreeModel model;
    private double currentStage;
    private boolean done;
    private double[] diastolicPressureStart;
    private double[] diastolicPressureEnd;
    private double[] diastolicPressureMiddle;
    
    public int skipCycles = 4;
    
    @Override
    public void start(Object model)
    {
        if( ! ( model instanceof ArterialBinaryTreeModel ) )
            throw new IllegalArgumentException("Wrong type of model: ArterialBinaryTreeModel required");

        this.model = (ArterialBinaryTreeModel)model;
        List<SimpleVessel> list = ( (ArterialBinaryTreeModel)model ).vessels;
        vesselInfos = new HashMap<>();
        for( SimpleVessel vessel : list )
            vesselInfos.put(vessel.name, new VesselInfo(vessel));

        cycleIndex = 0;
        done = false;
        currentStage = this.model.systole;
        startSearch = false;
        startTime = 0;
        diastolicPressureStart = new double[this.model.size()];
        diastolicPressureEnd = new double[this.model.size()];
        diastolicPressureMiddle = new double[this.model.size()];
    }

    public double startTime = 0;
    boolean startSearch = false;

    @Override
    public void add(double t, double[] y) throws Exception
    {
        if( done )
            return;

        if ( startSearch && t - startTime > searchTime)
        {
            complete();
            done = true;
        }
            
        
        if( currentStage == 0 && model.systole == 1 ) //diastole => systole
        {
            cycleIndex++;

            if( cycleIndex < skipCycles )
                return;

            if( cycleIndex > skipCycles && !startSearch )
            {
                startSearch = true;
                startTime = t;
//                System.out.println("Start: t = " + t);
                for (SimpleVessel v: model.vessels)
                {
                    diastolicPressureStart[v.index] = v.pressure[0];
                    diastolicPressureEnd[v.index] = v.pressure[v.pressure.length - 1];
                    diastolicPressureMiddle[v.index] = v.pressure[v.pressure.length/2];
//                    System.out.println(v.name+": " + diastolicPressureStart[v.index]+",  "+v.pressure[v.pressure.length - 1]);
                }
            }
            
           
        }

        currentStage = model.systole;

        if( !startSearch )
            return;

        times.add(t);
        for( VesselInfo info : vesselInfos.values() )
        {
            SimpleVessel vessel = info.vessel;
            double pressureStart = vessel.getPressure()[0];
            double pressureMiddle = vessel.pressure[vessel.pressure.length/2];
            double pressureEnd = vessel.pressure[vessel.pressure.length - 1];

//            if( !info.foundMinAtStart )
//            {
//                info.minPressureStart = diastolicPressureStart[vessel.index];
//                info.minPressureStartTime = t;
//                info.foundMinAtStart = true;
////                System.out.println("Start found at t =+ " + t+" p = "+ info.minPressureStart);
//            }
//
//            if( !info.foundMinAtEnd && pressureEnd > diastolicPressureEnd[vessel.index] + error )
//            {
//                info.minPressureEnd = pressureEnd;
//                info.minPressureEndTime = t;
//                info.foundMinAtEnd = true;
////                System.out.println("Finish found at t =+ " + t+" p = "+ info.minPressureEnd);
//            }
//
//            if( !info.foundMinAtMiddle && pressureMiddle > diastolicPressureMiddle[vessel.index] + error )
//            {
//                info.minPressureMiddle = pressureMiddle;
//                info.minPressureMiddleTime = t;
//                info.foundMinAtMiddle = true;
////                System.out.println("Middle found at t =+ " + t+" p = "+ info.minPressureMiddle);
//            }
            
            if (pressureStart > info.maxPressureStart)
            {
                info.maxPressureStartTime = t;
                info.maxPressureStart = pressureStart;
            }
            
            if (pressureEnd > info.maxPressureEnd)
            {
                info.maxPressureEndTime = t;
                info.maxPressureEnd = pressureEnd;
            }
            
            if (pressureMiddle > info.maxPressureMiddle)
            {
                info.maxPressureMiddleTime = t;
                info.maxPressureMiddle = pressureMiddle;
            }
            
            if (pressureStart < info.minPressureStart)
            {
                info.minPressureStartTime = t;
                info.minPressureStart = pressureStart;
            }
            
            if (pressureEnd < info.minPressureEnd)
            {
                info.minPressureEndTime = t;
                info.minPressureEnd = pressureEnd;
            }
            
            if (pressureMiddle < info.minPressureMiddle)
            {
                info.minPressureMiddleTime = t;
                info.minPressureMiddle = pressureMiddle;
            }
        }
    }


    Writer reportWriter;

    public void setWriter(Writer writer)
    {
        this.reportWriter = writer;
    }

    public void writeTimeCourseData() throws IOException
    {
        if (reportWriter == null)
            return;

        for( VesselInfo info : vesselInfos.values() )
        {
            StringBuilder nextStringStart = new StringBuilder(info.vessel.title).append("_Start\t");
            StringBuilder nextStringEnd = new StringBuilder(info.vessel.title).append("_End\t");
            for( int i = 0; i < info.pressureStart.size(); i++ )
            {
                if( i % 10 < 0.0001);//Math.abs(i / 10 - Math.round(i / 10)) < 0.0001 )
                {
                    nextStringStart.append(info.pressureStart.get(i)).append("\t");
                    nextStringEnd.append(info.pressureEnd.get(i)).append("\t");
                }
            }
            reportWriter.write(nextStringStart.append("\n").toString().replaceAll("\\.", ","));
            reportWriter.write(nextStringEnd.append("\n").toString().replaceAll("\\.", ","));
        }
        StringBuilder timeStart = new StringBuilder("time\t");
        for( int i = 0; i < times.size(); i++ )
        {
            if(i % 10 < 0.0001)
            {
                timeStart.append(times.get(i)).append("\t");
            }
        }
        reportWriter.write(timeStart.toString());
    }

    public void complete()// throws IOException
    {
        VesselInfo rootInfo = vesselInfos.get(model.root.name);

        for( VesselInfo info : vesselInfos.values() )
        {
//            Collections.sort(info.pressureEnd);
//            Collections.sort(info.pressureStart);
//            double medianPressureEnd = ( info.pressureEnd.get(info.pressureEnd.size() - 1) + info.pressureEnd.get(0) ) / 2;
//            double medianPressureStart = ( info.pressureStart.get(info.pressureStart.size() - 1) + info.pressureStart.get(0) ) / 2;
//            int medianIndexEnd = 0;
//            int medianIndexStart = 0;
//            double error = Double.POSITIVE_INFINITY;
//            for( int i = 0; i < info.pressureEnd.size(); i++ )
//            {
//                double nextError = Math.abs(info.pressureStart.get(i) - medianPressureStart);
//                if( nextError < error )
//                {
//                    medianIndexStart = i;
//                    error = nextError;
//                }
//            }
//
//            error = Double.POSITIVE_INFINITY;
//            for( int i = 0; i < info.pressureEnd.size(); i++ )
//            {
//                double nextError = Math.abs(info.pressureEnd.get(i) - medianPressureEnd);
//                if( nextError < error )
//                {
//                    medianIndexEnd = i;
//                    error = nextError;
//                }
//            }

//            info.midPressureStartTime = times.get(medianIndexStart);
            //            info.midPressureEndTime = times.get(medianIndexEnd);
            //            info.midPressureEnd = info.pressureEnd.get(medianIndexEnd);
            //            info.midPressureStart = info.pressureStart.get(medianIndexStart);

            SimpleVessel vessel = info.vessel;

            info.velocityMin = vessel.length / ( info.minPressureEndTime - info.minPressureStartTime );
            info.velocityMin1 = vessel.length /  2 / ( info.minPressureMiddleTime - info.minPressureStartTime );
            info.velocityMin2 = vessel.length / 2 / ( info.minPressureEndTime - info.maxPressureMiddleTime );
            
            info.velocityMax = vessel.length / ( info.maxPressureEndTime - info.maxPressureStartTime );
            info.velocityMax1 = vessel.length / 2 / ( info.maxPressureMiddleTime - info.maxPressureStartTime );
            info.velocityMax2 = vessel.length / 2 /( info.maxPressureEndTime - info.maxPressureMiddleTime );
            
            double length = vessel.length;
            SimpleVessel parent = vessel.parent;
            while( parent != null )
            {
                length += parent.length;
                parent = parent.parent;
            }

            info.velocityForBranch = length / ( info.minPressureEndTime - rootInfo.minPressureStartTime );
            
            System.out.println("vmin: "+ info.velocityMin);
            System.out.println("vmin1: "+ info.velocityMin1);
            System.out.println("start min at: "+info.minPressureStartTime);
            System.out.println("middle min at: "+info.minPressureMiddleTime);
            System.out.println("end min at: "+info.minPressureEndTime);
            
            System.out.println("vmax: "+ info.velocityMax);
            System.out.println("vmax1: "+info.velocityMax1);
            System.out.println("start at: "+info.maxPressureStartTime);
            System.out.println("middle at: "+info.maxPressureMiddleTime);
            
            
            System.out.println("end at: "+info.maxPressureEndTime);
            
        }
    }

    private Map<String, VesselInfo> vesselInfos;

    public Map<String, VesselInfo> getVesselInfos()
    {
        return vesselInfos;
    }


    public void setVesselInfos(Map<String, VesselInfo> vesselInfos)
    {
        this.vesselInfos = vesselInfos;
    }

    List<Double> times = new ArrayList<>();
    /**
     * Technical class for collection of vessels data
     */
    public static class VesselInfo
    {
        public SimpleVessel vessel;

        public VesselInfo(SimpleVessel vessel)
        {
            this.vessel = vessel;
        }

        public double minPressureStartTime = Double.POSITIVE_INFINITY;
        public double minPressureEndTime = Double.POSITIVE_INFINITY;
        public double minPressureMiddleTime = Double.POSITIVE_INFINITY;
        public double minPressureMiddle = Double.POSITIVE_INFINITY;
        public double minPressureStart = Double.POSITIVE_INFINITY;
        public double minPressureEnd =Double.POSITIVE_INFINITY;

        public double maxPressureStartTime = Double.POSITIVE_INFINITY;
        public double maxPressureMiddleTime = Double.POSITIVE_INFINITY;
        public double maxPressureEndTime = Double.POSITIVE_INFINITY;
        public double maxPressureStart = 0;
        public double maxPressureMiddle = 0;
        public double maxPressureEnd = 0;
        
        

//        public double midPressureStart;// = Double.POSITIVE_INFINITY;
//        public double midPressureEnd;// = Double.POSITIVE_INFINITY;
//        public double midPressureStartTime = 0;
//        public double midPressureEndTime = 0;
        List<Double> pressureStart = new ArrayList<>();
        List<Double> pressureEnd = new ArrayList<>();
        public double velocityMid;
        public double velocityMax;
        public double velocityMax1;
        public double velocityMax2;
        
        public double velocityMin;
        public double velocityMin1;
        public double velocityMin2;
        public double velocityForBranch;

        public boolean foundMinAtStart;
        public boolean foundMinAtEnd;
        public boolean foundMinAtMiddle;
        
        public boolean isLeaf()
        {
            return vessel.left == null && vessel.right == null;
        }
    }
}
