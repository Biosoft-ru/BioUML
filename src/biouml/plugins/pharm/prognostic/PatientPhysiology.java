package biouml.plugins.pharm.prognostic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.developmentontheedge.beans.BeanInfoEx;

import one.util.streamex.StreamEx;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;


public class PatientPhysiology extends OptionEx
{
    private GeneralData generalData = new GeneralData();
    private Pressure pressure = new Pressure();
    private ECG ecg = new ECG();
    private HeartUltrasound heartUltrasound = new HeartUltrasound();
    private BloodTest bloodTest = new BloodTest();
    private Biochemistry biochemistry = new Biochemistry();
    private Diseases diseases = new Diseases();
    private Genetics genetics = new Genetics();
    private CalculatedParameters calculatedParameters = new CalculatedParameters( generalData, pressure, ecg, heartUltrasound, bloodTest,
            biochemistry );

    public PatientPhysiology()
    {
        diseases.addPropertyChangeListener( new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                String pname = evt.getPropertyName();
                if( pname.equals( "lvdd" ) || pname.equals( "mr" ) || pname.equals( "tr" ) || pname.equals( "ar" ) || pname.equals( "pr" )
                        || pname.equals( "chf" ) )
                {
                    firePropertyChange( "*", null, null );
                }
            }
        } );
    }

    public GeneralData getGeneralData()
    {
        return generalData;
    }

    public void setGeneralData(GeneralData generalData)
    {
        this.generalData = generalData;
    }

    public Pressure getPressure()
    {
        return pressure;
    }
    public void setPressure(Pressure pressure)
    {
        this.pressure = pressure;
    }

    public ECG getEcg()
    {
        return ecg;
    }
    public void setEcg(ECG ecg)
    {
        this.ecg = ecg;
    }

    public HeartUltrasound getHeartUltrasound()
    {
        return heartUltrasound;
    }
    public void setHeartUltrasound(HeartUltrasound heartUltrasound)
    {
        this.heartUltrasound = heartUltrasound;
    }

    public BloodTest getBloodTest()
    {
        return bloodTest;
    }
    public void setBloodTest(BloodTest bloodTest)
    {
        this.bloodTest = bloodTest;
    }

    public Biochemistry getBiochemistry()
    {
        return biochemistry;
    }
    public void setBiochemistry(Biochemistry biochemistry)
    {
        this.biochemistry = biochemistry;
    }

    public Diseases getDiseases()
    {
        return diseases;
    }
    public void setDiseases(Diseases diseases)
    {
        this.diseases = diseases;
    }

    public Genetics getGenetics()
    {
        return genetics;
    }
    public void setGenetics(Genetics genetics)
    {
        this.genetics = genetics;
    }

    public CalculatedParameters getCalculatedParameters()
    {
        return calculatedParameters;
    }
    public void setCalculatedParameters(CalculatedParameters calculatedParameters)
    {
        this.calculatedParameters = calculatedParameters;
    }

    public static class GeneralData extends OptionEx
    {
        private double age = 40;
        private double height = 170;
        private double weight = 70;
        protected Gender sex = Gender.MAN;
        protected Race race = Race.CAUCASOID;

        public double getAge()
        {
            return age;
        }
        public void setAge(double age)
        {
            double oldValue = this.age;
            this.age = age;
            firePropertyChange( "age", oldValue, age );
        }

        public double getHeight()
        {
            return height;
        }
        public void setHeight(double height)
        {
            double oldValue = this.height;
            this.height = height;
            firePropertyChange( "height", oldValue, height );
        }

        public double getWeight()
        {
            return weight;
        }
        public void setWeight(double weight)
        {
            double oldValue = this.weight;
            this.weight = weight;
            firePropertyChange( "weight", oldValue, weight );
        }

        public String getSex()
        {
            return sex.toString();
        }
        public void setSex(String newSex)
        {
            Gender oldValue = this.sex;
            this.sex = Gender.getValue( newSex );
            firePropertyChange( "sex", oldValue, this.sex );
        }

        public String getRace()
        {
            return race.toString();
        }
        public void setRace(String newRace)
        {
            Race oldValue = this.race;
            this.race = Race.getValue( newRace );
            firePropertyChange( "race", oldValue, this.race );
        }
    }

    public static class GeneralDataBeanInfo extends BeanInfoEx
    {
        public GeneralDataBeanInfo()
        {
            super( GeneralData.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "age" ).title( "AGE" ).add();
            property( "height" ).title( "HEIGHT" ).add();
            property( "weight" ).title( "WEIGHT" ).add();
            property( "sex" ).title( "SEX" ).tags( Gender.getValues() ).add();
            property( "race" ).title( "RACE" ).tags( Race.getValues() ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class Pressure extends OptionEx
    {
        private double ps = 140;
        private double pd = 90;

        public double getPs()
        {
            return ps;
        }
        public void setPs(double ps)
        {
            double oldValue = this.ps;
            this.ps = ps;
            firePropertyChange( "ps", oldValue, ps );
        }

        public double getPd()
        {
            return pd;
        }
        public void setPd(double pd)
        {
            double oldValue = this.pd;
            this.pd = pd;
            firePropertyChange( "pd", oldValue, pd );
        }
    }

    public static class PressureBeanInfo extends BeanInfoEx
    {
        public PressureBeanInfo()
        {
            super( Pressure.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "ps" ).title( "PS" ).add();
            property( "pd" ).title( "PD" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class ECG extends OptionEx
    {
        private double hr = 70;

        public double getHr()
        {
            return hr;
        }
        public void setHr(double hr)
        {
            double oldValue = this.hr;
            this.hr = hr;
            firePropertyChange( "hr", oldValue, hr );
        }
    }

    public static class ECGBeanInfo extends BeanInfoEx
    {
        public ECGBeanInfo()
        {
            super( ECG.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "hr" ).title( "HR" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public class HeartUltrasound extends OptionEx
    {
        private double sv = Double.NaN;
        private double ef = Double.NaN;

        public double getSv()
        {
            return sv;
        }
        public void setSv(double sv)
        {
            double oldValue = this.sv;
            this.sv = sv;
            firePropertyChange( "sv", oldValue, sv );
        }

        public double getEf()
        {
            return ef;
        }
        public void setEf(double ef)
        {
            double oldValue = this.ef;
            this.ef = ef;
            firePropertyChange( "ef", oldValue, ef );
        }
    }

    public static class HeartUltrasoundBeanInfo extends BeanInfoEx
    {
        public HeartUltrasoundBeanInfo()
        {
            super( HeartUltrasound.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "sv" ).title( "SV" ).add();
            property( "ef" ).title( "EF" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class BloodTest extends OptionEx
    {
        private double he = Double.NaN;
        private double hct = Double.NaN;

        public double getHe()
        {
            return he;
        }
        public void setHe(double he)
        {
            this.he = he;
        }

        public double getHct()
        {
            return hct;
        }
        public void setHct(double hct)
        {
            double oldValue = this.hct;
            this.hct = hct;
            firePropertyChange( "hct", oldValue, hct );
        }
    }

    public static class BloodTestBeanInfo extends BeanInfoEx
    {
        public BloodTestBeanInfo()
        {
            super( BloodTest.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "he" ).title( "HE" ).add();
            property( "hct" ).title( "HCT" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class Biochemistry extends OptionEx
    {
        private double tp = Double.NaN;
        private double glucose = Double.NaN;
        private double urea = Double.NaN;
        private double creatinine = Double.NaN;
        private double potassium = Double.NaN;
        private double sodium = Double.NaN;

        public double getTp()
        {
            return tp;
        }
        public void setTp(double tp)
        {
            this.tp = tp;
        }

        public double getGlucose()
        {
            return glucose;
        }
        public void setGlucose(double glucose)
        {
            this.glucose = glucose;
        }

        public double getUrea()
        {
            return urea;
        }
        public void setUrea(double urea)
        {
            this.urea = urea;
        }

        public double getCreatinine()
        {
            return creatinine;
        }
        public void setCreatinine(double creatinine)
        {
            double oldValue = this.creatinine;
            this.creatinine = creatinine;
            firePropertyChange( "creatinine", oldValue, creatinine );
        }

        public double getPotassium()
        {
            return potassium;
        }
        public void setPotassium(double potassium)
        {
            this.potassium = potassium;
        }

        public double getSodium()
        {
            return sodium;
        }
        public void setSodium(double sodium)
        {
            this.sodium = sodium;
        }
    }

    public static class BiochemistryBeanInfo extends BeanInfoEx
    {
        public BiochemistryBeanInfo()
        {
            super( Biochemistry.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "tp" ).title( "TP" ).add();
            property( "glucose" ).title( "GLUCOSE" ).add();
            property( "urea" ).title( "UREA" ).add();
            property( "creatinine" ).title( "CREATININE" ).add();
            property( "potassium" ).title( "POTASSIUM" ).add();
            property( "sodium" ).title( "SODIUM" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class Diseases extends OptionEx
    {
        private Diagnosis hypertension = Diagnosis.UNKNOWN;

        private Diagnosis chf = Diagnosis.UNKNOWN;
        private Classification chfType = NYHA.CLASS_I;

        private Diagnosis ph = Diagnosis.UNKNOWN;

        private Diagnosis lvdd = Diagnosis.UNKNOWN;

        private Diagnosis lvh = Diagnosis.UNKNOWN;

        private Diagnosis crf = Diagnosis.UNKNOWN;

        private Diagnosis atherosclerosis = Diagnosis.UNKNOWN;

        private Diagnosis mr = Diagnosis.UNKNOWN;
        private Classification mrType = RegurgitationStage.MILD;

        private Diagnosis tr = Diagnosis.UNKNOWN;
        private Classification trType = RegurgitationStage.MILD;

        private Diagnosis ar = Diagnosis.UNKNOWN;
        private Classification arType = RegurgitationStage.MILD;

        private Diagnosis pr = Diagnosis.UNKNOWN;
        private Classification prType = RegurgitationStage.MILD;

        /**
         * Arterial hypertension
         */
        public String getHypertension()
        {
            return hypertension.toString();
        }
        public void setHypertension(String newChoice)
        {
            Diagnosis oldValue = this.hypertension;
            this.hypertension = Diagnosis.getValue( newChoice );
            firePropertyChange( "hypertension", oldValue, this.hypertension );
        }

        /**
         * Chronic heart failure
         */
        public String getChf()
        {
            return chf.toString();
        }
        public void setChf(String newChoice)
        {
            Diagnosis oldValue = this.chf;
            this.chf = Diagnosis.getValue( newChoice );
            firePropertyChange( "chf", oldValue, this.chf );
        }

        /**
         * Functional class (NYHA)
         */
        public String getChfType()
        {
            return chfType.toString();
        }
        public void setChfType(String newClass)
        {
            this.chfType = NYHA.getValue( newClass );
        }
        public boolean isChfTypeHidden()
        {
            return !chf.equals( Diagnosis.YES );
        }

        /**
         * Pulmonary hypertension
         */
        public String getPh()
        {
            return ph.toString();
        }
        public void setPh(String newChoice)
        {
            Diagnosis oldValue = this.ph;
            this.ph = Diagnosis.getValue( newChoice );
            firePropertyChange( "ph", oldValue, this.ph );
        }

        /**
         * LV diastolic dysfunction
         */
        public String getLvdd()
        {
            return lvdd.toString();
        }
        public void setLvdd(String newChoice)
        {
            Diagnosis oldValue = this.lvdd;
            this.lvdd = Diagnosis.getValue( newChoice );
            firePropertyChange( "lvdd", oldValue, this.lvdd );
        }

        /**
         * LV hypertrophy
         */
        public String getLvh()
        {
            return lvh.toString();
        }
        public void setLvh(String newChoice)
        {
            Diagnosis oldValue = this.lvh;
            this.lvh = Diagnosis.getValue( newChoice );
            firePropertyChange( "lvh", oldValue, this.lvh );
        }

        /**
         * Chronic renal failure"
         */
        public String getCrf()
        {
            return crf.toString();
        }
        public void setCrf(String newChoice)
        {
            Diagnosis oldValue = this.crf;
            this.crf = Diagnosis.getValue( newChoice );
            firePropertyChange( "crf", oldValue, this.crf );
        }

        /**
         * Carotid and/or coronary atherosclerosis
         */
        public String getAtherosclerosis()
        {
            return atherosclerosis.toString();
        }
        public void setAtherosclerosis(String newChoice)
        {
            this.atherosclerosis = Diagnosis.getValue( newChoice );
        }

        /**
         * Mitral regurgitation
         */
        public String getMr()
        {
            return mr.toString();
        }
        public void setMr(String newChoice)
        {
            Diagnosis oldValue = this.mr;
            this.mr = Diagnosis.getValue( newChoice );
            firePropertyChange( "mr", oldValue, this.mr );
        }

        /**
         * Mitral regurgitation stage
         */
        public String getMrType()
        {
            return mrType.toString();
        }
        public void setMrType(String newStage)
        {
            this.mrType = RegurgitationStage.getValue( newStage );
        }
        public boolean isMrTypeHidden()
        {
            return !mr.equals( Diagnosis.YES );
        }

        /**
         * Tricuspid regurgitation
         */
        public String getTr()
        {
            return tr.toString();
        }
        public void setTr(String newChoice)
        {
            Diagnosis oldValue = this.tr;
            this.tr = Diagnosis.getValue( newChoice );
            firePropertyChange( "tr", oldValue, this.tr );
        }

        /**
         * Tricuspid regurgitation stage
         */
        public String getTrType()
        {
            return trType.toString();
        }
        public void setTrType(String newStage)
        {
            this.trType = RegurgitationStage.getValue( newStage );
        }
        public boolean isTrTypeHidden()
        {
            return !tr.equals( Diagnosis.YES );
        }

        /**
         * Aortic regurgitation
         */
        public String getAr()
        {
            return ar.toString();
        }
        public void setAr(String newChoice)
        {
            Diagnosis oldValue = this.ar;
            this.ar = Diagnosis.getValue( newChoice );
            firePropertyChange( "ar", oldValue, this.ar );
        }

        /**
         * Aortic regurgitation stage
         */
        public String getArType()
        {
            return arType.toString();
        }
        public void setArType(String newStage)
        {
            this.arType = RegurgitationStage.getValue( newStage );
        }
        public boolean isArTypeHidden()
        {
            return !ar.equals( Diagnosis.YES );
        }

        /**
         * Pulmonary regurgitation
         */
        public String getPr()
        {
            return pr.toString();
        }
        public void setPr(String newChoice)
        {
            Diagnosis oldValue = this.pr;
            this.pr = Diagnosis.getValue( newChoice );
            firePropertyChange( "pr", oldValue, this.pr );
        }

        /**
         * Pulmonary regurgitation stage
         */
        public String getPrType()
        {
            return prType.toString();
        }
        public void setPrType(String newStage)
        {
            this.prType = RegurgitationStage.getValue( newStage );
        }
        public boolean isPrTypeHidden()
        {
            return !pr.equals( Diagnosis.YES );
        }
    }

    public static class DiseasesBeanInfo extends BeanInfoEx2<Diseases>
    {
        public DiseasesBeanInfo()
        {
            super( Diseases.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "hypertension" ).title( "HYPERTENSION" ).tags( Diagnosis.getValues() ).add();

            property( "chf" ).title( "CHF" ).tags( Diagnosis.getValues() ).add();
            property( "chfType" ).title( "CHF_CLASS" ).tags( NYHA.getValues() ).hidden( "isChfTypeHidden" ).add();

            property( "ph" ).title( "PH" ).tags( Diagnosis.getValues() ).add();

            property( "lvdd" ).title( "LVDD" ).tags( Diagnosis.getValues() ).add();

            property( "lvh" ).title( "LVH" ).tags( Diagnosis.getValues() ).add();

            property( "crf" ).title( "CRF" ).tags( Diagnosis.getValues() ).add();

            property( "atherosclerosis" ).title( "ATHEROSCLEROSIS" ).tags( Diagnosis.getValues() ).add();

            property( "mr" ).title( "MR" ).tags( Diagnosis.getValues() ).add();
            property( "mrType" ).title( "MR_STAGE" ).tags( RegurgitationStage.getValues() ).hidden( "isMrTypeHidden" ).add();

            property( "tr" ).title( "TR" ).tags( Diagnosis.getValues() ).add();
            property( "trType" ).title( "TR_STAGE" ).tags( RegurgitationStage.getValues() ).hidden( "isTrTypeHidden" ).add();

            property( "ar" ).title( "AR" ).tags( Diagnosis.getValues() ).add();
            property( "arType" ).title( "AR_STAGE" ).tags( RegurgitationStage.getValues() ).hidden( "isArTypeHidden" ).add();

            property( "pr" ).title( "PR" ).tags( Diagnosis.getValues() ).add();
            property( "prType" ).title( "PR_STAGE" ).tags( RegurgitationStage.getValues() ).hidden( "isPrTypeHidden" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class Genetics extends OptionEx
    {
        private GeneADRB1 ADRB1 = new GeneADRB1();
        public GeneADRB1 getADRB1()
        {
            return ADRB1;
        }
        public void setADRB1(GeneADRB1 ADRB1)
        {
            this.ADRB1 = ADRB1;
        }
    }

    public static class GeneticsBeanInfo extends BeanInfoEx2<Genetics>
    {
        public GeneticsBeanInfo()
        {
            super( Genetics.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "ADRB1" ).title( "GENE_ADRB1" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class GeneADRB1 extends OptionEx
    {
    	private Arg389Gly arg389Gly = Arg389Gly.UNKNOWN;
        public String getArg389Gly()
        {
            return arg389Gly.toString();
        }
        public void setArg389Gly(String newValue)
        {
        	Arg389Gly oldValue = this.arg389Gly;
            this.arg389Gly = Arg389Gly.getGeneticVariant( newValue );
            firePropertyChange( "arg389Gly", oldValue, this.arg389Gly );
        }

        /**
         * Petersen M, Andersen JT, Jimenez-Solem E, et al.
         * Effect of the Arg389Gly β₁-adrenoceptor polymorphism on plasma renin activity and heart rate,
         * and the genotype-dependent response to metoprolol treatment.
         * Clin Exp Pharmacol Physiol. 2012. 39(9):779-785.
         * doi: 10.1111/j.1440-1681.2012.05736.x
         * (mL/units per L were converted to pg/mL)
         */
        public double getPRC()
        {
            switch( arg389Gly )
            {
                case GLY_GLY:
                    return 6.0;
                case ARG_GLY:
                    return 7.8;
                case ARG_ARG:
                    return 13.2;
			default:
				return Double.NaN;
            }
        }
    }

    public static class GeneADRB1BeanInfo extends BeanInfoEx2<GeneADRB1>
    {
        public GeneADRB1BeanInfo()
        {
            super( GeneADRB1.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "arg389Gly" ).title( "POLYMORPHISM_ARG389GLY" ).tags( Arg389Gly.getGeneticVariants() ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static class CalculatedParameters extends OptionEx
    {
        private double bv;
        private double map;
        private double cl;
        private double co = Double.NaN;
        private double lvedv = Double.NaN;
        private double lvesv = Double.NaN;
        private double vis = Double.NaN;
        private double gfr = Double.NaN;

        private GeneralData generalData;
        private Pressure pressure;
        private ECG ecg;
        private HeartUltrasound heartUltrasound;
        private BloodTest bloodTest;
        private Biochemistry biochemistry;

        public CalculatedParameters(GeneralData generalData, Pressure pressure, ECG ecg, HeartUltrasound heartUltrasound,
                BloodTest bloodTest, Biochemistry biochemistry)
        {
            this.generalData = generalData;
            this.pressure = pressure;
            this.ecg = ecg;
            this.heartUltrasound = heartUltrasound;
            this.bloodTest = bloodTest;
            this.biochemistry = biochemistry;

            this.generalData.addPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    String pname = evt.getPropertyName();
                    if( pname.equals( "sex" ) )
                    {
                        calcGfr();
                        calcBv();
                    }
                    else if( pname.equals( "age" ) || pname.equals( "race" ) )
                    {
                        calcGfr();
                    }
                    else if( pname.equals( "height" ) || pname.equals( "weight" ) )
                    {
                        calcBv();
                    }
                }
            } );

            this.pressure.addPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    String pname = evt.getPropertyName();
                    if( pname.equals( "ps" ) || pname.equals( "pd" ) )
                        calcMap();
                }
            } );

            this.ecg.addPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    if( evt.getPropertyName().equals( "hr" ) )
                    {
                        calcCl();
                        calcCo();
                    }
                }
            } );

            this.heartUltrasound.addPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    String pname = evt.getPropertyName();
                    if( pname.equals( "sv" ) )
                    {
                        calcCo();
                        calcLvedv();
                        calcLvesv();
                    }
                    if( pname.equals( "ef" ) )
                    {
                        calcLvedv();
                        calcLvesv();
                    }
                }
            } );

            this.bloodTest.addPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    if( evt.getPropertyName().equals( "hct" ) )
                        calcVis();
                }
            } );

            this.biochemistry.addPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    if( evt.getPropertyName().equals( "creatinine" ) )
                        calcGfr();
                }
            } );

            calcBv();
            calcMap();
            calcCl();
        }

        /**
         * Total blood volume (Nadler equation), ml
         */
        public double getBv()
        {
            return bv;
        }
        public void setBv(double bv)
        {
            double oldValue = this.bv;
            this.bv = bv;
            firePropertyChange( "bv", oldValue, bv );
        }
        /**
         * Nadler SB, Hidalgo JU, Bloch T. Prediction of blood volume in normal human adults.
         * Surgery, 1962, 51:224–232.
         */
        public void calcBv()
        {
            double h = generalData.getHeight();
            double w = generalData.getWeight();

            double bv = 1000;
            switch( generalData.sex )
            {
                case MAN:
                    bv *= 0.3669 * Math.pow( 0.01 * h, 3 ) + 0.03219 * w + 0.6041;
                    break;
                case WOMAN:
                    bv *= 0.3561 * Math.pow( 0.01 * h, 3 ) + 0.03308 * w + 0.1833;
                    break;
            }
            setBv( bv );
        }

        /**
         * Mean arterial pressure, mmHg
         */
        public double getMap()
        {
            return map;
        }
        public void setMap(double map)
        {
            double oldValue = this.map;
            this.map = map;
            firePropertyChange( "map", oldValue, map );
        }
        /**
         * Moran D, Epstein Y, Keren G, Laor A, Sherez J, Shapiro Y.
         * Calculation of mean arterial pressure during exercise as a function of heart rate.
         * Applied human sciences, 1995, 14(6): 293–295.
         */
        public void calcMap()
        {
            setMap( ( 2 * pressure.getPd() + pressure.getPs() ) / 3 );
        }

        /**
         * Cardiac cycle length, sec
         */
        public double getCl()
        {
            return cl;
        }
        public void setCl(double cl)
        {
            double oldValue = this.cl;
            this.cl = cl;
            firePropertyChange( "cl", oldValue, cl );
        }
        public void calcCl()
        {
            setCl( 60 / ecg.getHr() );
        }

        /**
         * Cardiac output, l/min
         */
        public double getCo()
        {
            return co;
        }
        public void setCo(double co)
        {
            double oldValue = this.co;
            this.co = co;
            firePropertyChange( "co", oldValue, co );
        }
        /**
         * Cattermole GN, Leung PY, Ho GY, Lau PW, Chan CP, Chan SS, Smith BE, Graham CA, Rainer TH.
         * The normal ranges of cardiovascular parameters measured using the ultrasonic cardiac output monito.
         * Physiological Reports, 2017, 5(6):e13195.
         */
        public void calcCo()
        {
            setCo( ecg.getHr() * heartUltrasound.getSv() / 1000 );
        }

        /**
         * Left ventricular end-diastolic volume, ml
         */
        public double getLvedv()
        {
            return lvedv;
        }
        public void setLvedv(double lvedv)
        {
            double oldValue = this.lvedv;
            this.lvedv = lvedv;
            firePropertyChange( "lvedv", oldValue, lvedv );
        }
        /**
         * Maceira AM, Prasad SK, Khan M, Pennell DJ.
         * Normalized left ventricular systolic and diastolic function by
         * steady state free precession cardiovascular magnetic resonance.
         * Journal of Cardiovascular Magnetic Resonance, 2006, 8:417–426.
         */
        public void calcLvedv()
        {
            setLvedv( heartUltrasound.getSv() * 100 / heartUltrasound.getEf() );
        }

        /**
         * Left ventricular end-systolic volume, ml
         */
        public double getLvesv()
        {
            return lvesv;
        }
        public void setLvesv(double lvesv)
        {
            double oldValue = this.lvesv;
            this.lvesv = lvesv;
            firePropertyChange( "lvesv", oldValue, lvesv );
        }
        public void calcLvesv()
        {
            setLvesv( lvedv - heartUltrasound.getSv() );
        }

        /**
         * Blood viscosity, cP
         */
        public double getVis()
        {
            return vis;
        }
        public void setVis(double vis)
        {
            double oldValue = this.vis;
            this.vis = vis;
            firePropertyChange( "vis", oldValue, vis );
        }
        /**
         * Hund SJ, Kameneva MV, Antaki JF. A quasi-mechanistic mathematical representation for blood viscosity.
         * Fluids, 2017, 2(1): 10.
         */
        public void calcVis()
        {
            double hct = bloodTest.getHct();
            setVis( 1.23 * Math.pow( ( 1 - hct / 99 ), ( -1.7 - 9.86 * Math.exp( -0.0607 * hct ) ) ) );
        }

        /**
         * Glomerular filtration rate, l/min (CKD-EPI)
         */
        public double getGfr()
        {
            return gfr;
        }
        public void setGfr(double gfr)
        {
            double oldValue = this.gfr;
            this.gfr = gfr;
            firePropertyChange( "gfr", oldValue, gfr );
        }
        /**
         * CKD-EPI (Chronic Kidney Disease Epidemiology Collaboration);
         * Levey AS, Stevens LA, Schmid CH, et al. A New Equation to Estimate Glomerular Filtration Rate.
         * Ann Intern Med, 2009, 150:604-612.
         */
        public void calcGfr()
        {
            double age = generalData.getAge();
            double scr = biochemistry.getCreatinine() / 88.42; //μmol/L to mg/dl

            double kappa = 0;
            double alpha = 0;
            double factor = 0;

            switch( generalData.sex )
            {
                case MAN:
                {
                    switch( generalData.race )
                    {
                        case CAUCASOID:
                            factor = 141;
                            break;
                        case NEGROID:
                            factor = 163;
                            break;
                    }
                    kappa = 0.9;
                    if( scr <= 0.9 )
                        alpha = -0.411;
                    else
                        alpha = -1.209;
                    break;
                }
                case WOMAN:
                {
                    switch( generalData.race )
                    {
                        case CAUCASOID:
                            factor = 144;
                            break;
                        case NEGROID:
                            factor = 166;
                            break;
                    }
                    kappa = 0.7;
                    if( scr <= 0.7 )
                        alpha = -0.329;
                    else
                        alpha = -1.209;
                    break;
                }
            }

            //Multiplier 0.001 converts units from ml/min to l/min 
            setGfr( 0.001 * factor * Math.pow( Math.min( scr / kappa, 1 ), alpha ) * Math.pow( Math.max( scr / kappa, 1 ), -1.209 )
                    * Math.pow( 0.993, age ) );
        }
    }

    public static class CalculatedParametersBeanInfo extends BeanInfoEx
    {
        public CalculatedParametersBeanInfo()
        {
            super( CalculatedParameters.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "bv" ).title( "BV" ).readOnly().add();
            property( "map" ).title( "MAP" ).readOnly().add();
            property( "cl" ).title( "CL" ).readOnly().add();
            property( "co" ).title( "CO" ).readOnly().add();
            property( "lvedv" ).title( "LVEDV" ).readOnly().add();
            property( "lvesv" ).title( "LVESV" ).readOnly().add();
            property( "vis" ).title( "VIS" ).readOnly().add();
            property( "gfr" ).title( "GFR" ).readOnly().add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }

    public static enum Gender
    {
        MAN, WOMAN;

        public String toString()
        {
            switch( this )
            {
                case MAN:
                    return MessageBundle.getMessage( "MAN" );
                case WOMAN:
                    return MessageBundle.getMessage( "WOMAN" );
                default:
                    return null;
            }
        }

        public static Gender getValue(String name)
        {
            for( Gender value : values() )
                if( value.toString().equals( name ) )
                    return value;
            return null;
        }

        public static String[] getValues()
        {
            return StreamEx.of( values() ).map( key -> key.toString() ).toArray( String[]::new );
        }
    }

    public static enum Race
    {
        CAUCASOID, NEGROID;

        public String toString()
        {
            switch( this )
            {
                case CAUCASOID:
                    return MessageBundle.getMessage( "CAUCASOID" );
                case NEGROID:
                    return MessageBundle.getMessage( "NEGROID" );
                default:
                    return null;
            }
        }

        public static Race getValue(String name)
        {
            for( Race value : values() )
                if( value.toString().equals( name ) )
                    return value;
            return null;
        }

        public static String[] getValues()
        {
            return StreamEx.of( values() ).map( key -> key.toString() ).toArray( String[]::new );
        }
    }

    public static enum Diagnosis
    {
        UNKNOWN, YES, NO;

        public String toString()
        {
            switch( this )
            {
                case UNKNOWN:
                    return MessageBundle.getMessage( "UNKNOWN" );
                case YES:
                    return MessageBundle.getMessage( "YES" );
                case NO:
                    return MessageBundle.getMessage( "NO" );
                default:
                    return null;
            }
        }

        public static Diagnosis getValue(String name)
        {
            for( Diagnosis value : values() )
                if( value.toString().equals( name ) )
                    return value;
            return null;
        }

        public static String[] getValues()
        {
            return StreamEx.of( values() ).map( key -> key.toString() ).toArray( String[]::new );
        }
    }

    public static enum NYHA implements Classification
    {
        CLASS_I, CLASS_II, CLASS_III, CLASS_IV;

        public String toString()
        {
            switch( this )
            {
                case CLASS_I:
                    return MessageBundle.getMessage( "CLASS_I" );
                case CLASS_II:
                    return MessageBundle.getMessage( "CLASS_II" );
                case CLASS_III:
                    return MessageBundle.getMessage( "CLASS_III" );
                case CLASS_IV:
                    return MessageBundle.getMessage( "CLASS_IV" );
                default:
                    return null;
            }
        }

        public static NYHA getValue(String name)
        {
            for( NYHA value : values() )
                if( value.toString().equals( name ) )
                    return value;
            return null;
        }

        public static String[] getValues()
        {
            return StreamEx.of( values() ).map( key -> key.toString() ).toArray( String[]::new );
        }

        public String[] getItems()
        {
            return StreamEx.of( values() ).map( key -> key.name() ).toArray( String[]::new );
        }

        public String getSelectedItem()
        {
            return this.name();
        }
    }

    public static enum LVDDType implements Classification
    {
        TYPE_1, TYPE_2, TYPE_3;

        public String toString()
        {
            switch( this )
            {
                case TYPE_1:
                    return MessageBundle.getMessage( "TYPE_1" );
                case TYPE_2:
                    return MessageBundle.getMessage( "TYPE_2" );
                case TYPE_3:
                    return MessageBundle.getMessage( "TYPE_3" );
                default:
                    return null;
            }
        }

        public static LVDDType getValue(String name)
        {
            for( LVDDType value : values() )
                if( value.toString().equals( name ) )
                    return value;
            return null;
        }

        public static String[] getValues()
        {
            return StreamEx.of( values() ).map( key -> key.toString() ).toArray( String[]::new );
        }

        public String[] getItems()
        {
            return StreamEx.of( values() ).map( key -> key.name() ).toArray( String[]::new );
        }

        public String getSelectedItem()
        {
            return this.name();
        }
    }

    public static enum RegurgitationStage implements Classification
    {
        MILD, MODERATE, SEVERE;

        public String toString()
        {
            switch( this )
            {
                case MILD:
                    return MessageBundle.getMessage( "MILD" );
                case MODERATE:
                    return MessageBundle.getMessage( "MODERATE" );
                case SEVERE:
                    return MessageBundle.getMessage( "SEVERE" );
                default:
                    return null;
            }
        }

        public static RegurgitationStage getValue(String name)
        {
            for( RegurgitationStage value : values() )
                if( value.toString().equals( name ) )
                    return value;
            return null;
        }

        public static String[] getValues()
        {
            return StreamEx.of( values() ).map( key -> key.toString() ).toArray( String[]::new );
        }

        public String[] getItems()
        {
            return StreamEx.of( values() ).map( key -> key.name() ).toArray( String[]::new );
        }

        public String getSelectedItem()
        {
            return this.name();
        }
    }

    public interface Classification
    {
        public String[] getItems();
        public String getSelectedItem();
    }

    public static enum Arg389Gly
    {
        UNKNOWN, GLY_GLY, ARG_GLY, ARG_ARG;

        public String toString()
        {
            switch( this )
            {
                case UNKNOWN:
                    return MessageBundle.getMessage( "UNKNOWN" );
                case GLY_GLY:
                    return MessageBundle.getMessage( "GLY_GLY" );
                case ARG_GLY:
                    return MessageBundle.getMessage( "ARG_GLY" );
                case ARG_ARG:
                    return MessageBundle.getMessage( "ARG_ARG" );
                default:
                    return null;
            }
        }

        public static Arg389Gly getGeneticVariant(String geneticVariant)
        {
            for( Arg389Gly value : values() )
                if( value.toString().equals( geneticVariant ) )
                    return value;
            return null;
        }

        public static String[] getGeneticVariants()
        {
            return StreamEx.of( values() ).map( key -> key.toString() ).toArray( String[]::new );
        }
    }
}
