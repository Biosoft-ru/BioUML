package biouml.plugins.hemodynamics._test;

import biouml.plugins.simulation.TableElementPreprocessor;
import ru.biosoft.analysis.Util.CubicSpline;

public class PulseWavePatientInfo extends GenericPatientInfo
{

    public PulseWavePatientInfo(String str, String[] propertyNames)
    {
        super(str, propertyNames);
        aortalProfile = PressureProfile.createAortalProfile(this);
        radialProfile = PressureProfile.createRadialProfile(this);
    }

    PressureProfile aortalProfile;
    PressureProfile radialProfile;
    
    public static class PressureProfile
    {
        enum Type
        {
            AORTA, RADIAL;
        }

        Type type;
        private double[] time;
        private double[] pressure;
        private CubicSpline spline;
        private double tf;

        public String[] getFormulas(String argName)
        {
            return new String[] {"mod(time," + tf + ")", TableElementPreprocessor.generateFormula(spline, argName)};
        }

        private PressureProfile(double[] time, double[] pressure, Type type)
        {
            this.time = time;
            this.pressure = pressure;
            this.type = type;
            this.spline = new CubicSpline(time, pressure);
            tf = this.time[this.time.length - 1];
        }

        public double getPressure(double time)
        {
            return spline.getValue( time % tf);
        }

        public double[] getPressures(double[] time)
        {
            double[] result = new double[time.length];
            for( int i = 0; i < time.length; i++ )
                result[i] = getPressure(time[i]);
            return result;
        }

        public void shift(double delta)
        {
            for( int i = 0; i < time.length; i++ )
                time[i] += delta;
            spline = new CubicSpline(time, pressure);
        }

        public static PressureProfile createAortalProfile(GenericPatientInfo info)
        {
            double t0 = 0;
            double t1 = info.getDoubleValue("a_t1") / 1000;
            double t2 = info.getDoubleValue("a_t2") / 1000;
            double tes = info.getDoubleValue("ed_ms") / 1000;
            double tf = info.getDoubleValue("tf") / 1000;
            double ps = info.getDoubleValue("a_sp");
            double pd = info.getDoubleValue("a_dp");
            double pes = info.getDoubleValue("sp_end");
            double p1he = info.getDoubleValue("p1he");

            return createAortalProfile(t0, t1, t2, tes, tf, ps, pd, p1he, pes);
        }

        public static PressureProfile createRadialProfile(GenericPatientInfo info)
        {
            double t0 = 0;
            double t1 = info.getDoubleValue("r_t1") / 1000;
            double t2 = info.getDoubleValue("r_t2") / 1000;
            double tf = info.getDoubleValue("tf") / 1000;
            double ps = info.getDoubleValue("r_sp");
            double pd = info.getDoubleValue("r_dp");
            double aix = info.getDoubleValue("r_aix");

            return createRadialProfile(t0, t1, t2, tf, ps, pd, aix);
        }

        public static PressureProfile createAortalProfile(double t0, double t1, double t2, double tes, double tf, double ps, double pd,
                double p1he, double pes)
        {
            double[] time = new double[] {t0, t1, t2, tes, tf};
            double[] pressure = new double[] {pd, p1he + pd, ps, pes, pd};
            return new PressureProfile(time, pressure, Type.AORTA);
        }

        public static PressureProfile createRadialProfile(double t0, double t1, double t2, double tf, double ps, double pd, double aix)
        {
            double[] time = new double[] {t0, t1, t2, tf};
            double[] pressures = new double[] {pd, ps, aix * ( ps - pd ) / 100 + ps, pd};
            return new PressureProfile(time, pressures, Type.RADIAL);
        }
    }

}
