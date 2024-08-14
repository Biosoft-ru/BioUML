package biouml.plugins.pharm.prognostic;

import org.jfree.util.Log;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

//@PropertyName ( "Настройки" )
@PropertyName ( "Settings" )
public class TreatmentSystemParameters extends AbstractAnalysisParameters
{
    private GeneralData generalData = new GeneralData();
    private BloodTest bloodTest = new BloodTest();
    private Biochemistry biochemistry = new Biochemistry();
    private ECG ecg = new ECG();
    private HeartUltrasound heartUltrasound = new HeartUltrasound();
    private Pressure pressure = new Pressure();

    private DataElementPath resultPath;

    private DataElementPath slowPopulationPath = DataElementPath.create( "databases/Virtual Human Support/Temp/Data/Hypertension_94" );
    private DataElementPath fastPopulationPath = DataElementPath.create( "databases/Virtual Human Support/Temp/Data/Population" );
    
    private DataElementPath fastRegimeDiagramPath = DataElementPath.create( "databases/Virtual Human Support/Temp/Models/Complex model Hallow 2014" );
    private DataElementPath slowRegimeDiagramPath = DataElementPath.create( "databases/Virtual Human Support/Temp/models_new/Complex model Combined");
    
    //    private final static String EDARBY_50_MG = "Лозартан 50 мг";
    //    private final static String PRESTANSE_50_MG = "Лозартан 50 мг";
    private final static String DIROTON_5_MG = "Диротон 5 мг";

    public final static String PLACEBO = "Плацебо";
    public final static String LOSARTAN_50_MG = "Лозартан 50 мг";
    public final static String LOSARTAN_100_MG = "Лозартан 100 мг";
    public final static String ALISKIREN_150_MG = "Алискирен 150 мг";
    public final static String ALISKIREN_300_MG = "Алискирен 300 мг";
    public final static String AMLODIPINE_5_MG = "Амлодипин 5 мг";
    public final static String AMLODIPINE_10_MG = "Амлодипин 10 мг";
    public final static String ENALAPRIL_20_MG = "Эналаприл 20 мг";
    public final static String HCTZ_12_5_MG = "Гидрохлортиазид 12.5 мг";
    public final static String HCTZ_25_MG = "Гидрохлортиазид 25 мг";
    public final static String LORTENZA_5_100_MG = "Лортенза 5/100 мг";
    public final static String BISOPROLOL_10_MG = "Бисопролол 10 мг";

    private final static String PLACEBO_ENG = "Placebo";
    public final static String LOSARTAN_50_MG_ENG = "Losartan 50 mg";
    public final static String LOSARTAN_100_MG_ENG = "Losartan 100 mg";
    public final static String ALISKIREN_150_MG_ENG = "Aliskiren 150 мг";
    public final static String ALISKIREN_300_MG_ENG = "Aliskiren 300 мг";
    public final static String AMLODIPINE_5_MG_ENG = "Amlodipine 5 мг";
    public final static String AMLODIPINE_10_MG_ENG = "Amlodipine 10 мг";
    public final static String ENALAPRIL_20_MG_ENG = "Enalapril 20 мг";
    public final static String HCTZ_12_5_MG_ENG = "Hydrochlorthiazide 12.5 mg";
    public final static String HCTZ_25_MG_ENG = "Hydrochlorthiazide 25 mg";
    public final static String LORTENZA_5_100_MG_ENG = "Lortenza 5/100 mg";
    public final static String BISOPROLOL_10_MG_ENG = "Bisopolol 10 mg";

    protected final static String FAST = "Экспресс";
    protected final static String NORMAL = "Стандартный";
    private final static String SLOW = "Детальный";

    protected final static String FAST_ENG = "Express";
    protected final static String NORMAL_ENG = "Standard";
    private final static String SLOW_ENG = "Detailed";

    public static String[] availableRegimes_eng = new String[] {FAST_ENG, NORMAL_ENG};
    public static String[] availableRegimes = new String[] {FAST, NORMAL};//, SLOW};

    public static String[] availableDrugs = new String[] {ALISKIREN_150_MG, ALISKIREN_300_MG, AMLODIPINE_5_MG, AMLODIPINE_10_MG,
            HCTZ_12_5_MG, HCTZ_25_MG, LORTENZA_5_100_MG, LOSARTAN_50_MG, LOSARTAN_100_MG, PLACEBO, ENALAPRIL_20_MG, BISOPROLOL_10_MG};

    public static String[] availableDrugs_eng = new String[] {ALISKIREN_150_MG_ENG, ALISKIREN_300_MG_ENG, AMLODIPINE_5_MG_ENG,
            AMLODIPINE_10_MG_ENG, HCTZ_12_5_MG_ENG, HCTZ_25_MG_ENG, LORTENZA_5_100_MG_ENG, LOSARTAN_50_MG_ENG, LOSARTAN_100_MG_ENG,
            PLACEBO_ENG, ENALAPRIL_20_MG_ENG, BISOPROLOL_10_MG_ENG};

    private String[] drugs = new String[] {LOSARTAN_100_MG_ENG, ENALAPRIL_20_MG_ENG, AMLODIPINE_10_MG_ENG};

    private int populationSize = 200;

    private double time = 200;

    private String regime = FAST;

    private boolean showPlots = false;

        @PropertyName ( "Общая информация" )
//    @PropertyName ( "General info " )
    public GeneralData getGeneralData()
    {
        return generalData;
    }
    public void setGeneralData(GeneralData generalData)
    {
        this.generalData = generalData;
    }

        @PropertyName ( "Общий анализ крови" )
//    @PropertyName ( "Blood test" )
    public BloodTest getBloodTest()
    {
        return bloodTest;
    }
    public void setBloodTest(BloodTest bloodTest)
    {
        this.bloodTest = bloodTest;
    }

        @PropertyName ( "Электрокардиография" )
//    @PropertyName ( "Electrocardiography" )
    public ECG getEcg()
    {
        return ecg;
    }
    public void setEcg(ECG ecg)
    {
        this.ecg = ecg;
    }

        @PropertyName ( "УЗИ сердца" )
//    @PropertyName ( "Heart ultrasound " )
    public HeartUltrasound getHeartUltrasound()
    {
        return heartUltrasound;
    }
    public void setHeartUltrasound(HeartUltrasound heartUltrasound)
    {
        this.heartUltrasound = heartUltrasound;
    }

        @PropertyName ( "Мониторинг АД" )
//    @PropertyName ( "Arterial pressures" )
    public Pressure getPressure()
    {
        return pressure;
    }
    public void setPressure(Pressure pressure)
    {
        this.pressure = pressure;
    }

        @PropertyName ( "Тестируемые препараты" )
//    @PropertyName ( "Drugs" )
    public String[] getDrugs()
    {
        return drugs;
    }
    public void setDrugs(String[] drugs)
    {
        this.drugs = drugs;
    }

    public static class Biochemistry
    {
        double glu = 4.62;
        double chol = 4.32;
        double chol2 = 1.25;
        double sodium = 143;
        double potassium = 4.5;

                @PropertyName ( "Глюкоза, ммоль/л" )
//        @PropertyName ( "Glucose, mmol/l" )
        public double getGlu()
        {
            return glu;
        }
        public void setGlu(double glu)
        {
            this.glu = glu;
        }

                @PropertyName ( "Холестерин ЛПНП, ммоль/л" )
//        @PropertyName ( "Cholesterol LDL, mmol/l" )
        public double getChol()
        {
            return chol;
        }
        public void setChol(double chol)
        {
            this.chol = chol;
        }

                @PropertyName ( "Холестерин ЛПВП, ммоль/л" )
//        @PropertyName ( "Cholesterol HDL, mmol/l" )
        public double getChol2()
        {
            return chol2;
        }
        public void setChol2(double chol2)
        {
            this.chol2 = chol2;
        }

                @PropertyName ( "Натрий, ммоль/л" )
//        @PropertyName ( "Sodium, mmol/l" )
        public double getSodium()
        {
            return sodium;
        }
        public void setSodium(double sodium)
        {
            this.sodium = sodium;
        }

                @PropertyName ( "Калий, ммоль/л" )
//        @PropertyName ( "Potassium, mmol/l" )
        public double getPotassium()
        {
            return potassium;
        }
        public void setPotassium(double potassium)
        {
            this.potassium = potassium;
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
            add( "glu" );
            add( "chol" );
            add( "chol2" );
            add( "sodium" );
            add( "potassium" );
        }
    }

    public static class BloodTest
    {
        double he = 160;
        double hematocrit = 49.4;


                @PropertyName ( "Гемоглобин, г/л" )
        @PropertyDescription ( "Гемоглобин" )
//        @PropertyName ( "Hemoglobin, g/l" )
        public double getHe()
        {
            return he;
        }
        public void setHe(double he)
        {
            this.he = he;
        }

                @PropertyName ( "Гематокрит, %" )
        @PropertyDescription ( "Гематокрит" )
//        @PropertyName ( "Hematocrit, %" )
        public double getHematocrit()
        {
            return hematocrit;
        }
        public void setHematocrit(double hematocrit)
        {
            this.hematocrit = hematocrit;
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
            add( "he" );
            add( "hematocrit" );
        }
    }

    public static class ECG
    {
        double hr = 70;

                @PropertyName ( "Частота сердечных сокращений, уд/мин" )
        @PropertyDescription ( "Частота сердечных сокращений" )
//        @PropertyName ( "Heart rate, beats/min" )
        public double getHr()
        {
            return hr;
        }
        public void setHr(double hr)
        {
            this.hr = hr;
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
            add( "hr" );
        }
    }

    public static class HeartUltrasound
    {
        double ef = 70;
        double co = 41;

//                @PropertyName ( "Фракция Выброса, %" )
        @PropertyDescription ( "Фракция выброса, %" )
//        @PropertyName ( "Ejection Fraction, %" )
                @PropertyName ( "Фракция Выброса, %" )
        public double getEF()
        {
            return ef;
        }
        public void setEF(double ef)
        {
            this.ef = ef;
        }

                @PropertyName ( "Ударный Объем, мл" )
        @PropertyDescription ( "Ударный объем, мл" )
//        @PropertyName ( "Stroke volume, ml" )
        public double getCo()
        {
            return co;
        }
        public void setCo(double co)
        {
            this.co = co;
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
            add( "co" );
            add( "ef" );
        }
    }

    public static class Pressure
    {
        double ps = 160;
        double pd = 100;

                @PropertyName ( "Систолическое артериальное давление, мм рт. ст." )
        @PropertyDescription ( "Систолическое артериальное давление" )
//        @PropertyName ( "Systolic pressure" )
        public double getPs()
        {
            return ps;
        }
        public void setPs(double ps)
        {
            this.ps = ps;
        }

                @PropertyName ( "Диастолическое артериальное давление, мм рт. ст." )
        @PropertyDescription ( "Диастолическое артериальное давление" )
//        @PropertyName ( "Diastolic pressure" )
        public double getPd()
        {
            return pd;
        }
        public void setPd(double pd)
        {
            this.pd = pd;
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
            add( "ps" );
            add( "pd" );
        }
    }

    public static class GeneralData
    {
        private double height = 160;
        private double weight = 70;
        private double age = 40;

                @PropertyName ( "Возраст, лет" )
        @PropertyDescription ( "Возраст." )
//        @PropertyName ( "Age, years" )
        public double getAge()
        {
            return age;
        }
        public void setAge(double age)
        {
            this.age = age;
        }

                @PropertyName ( "Рост, см" )
        @PropertyDescription ( "Рост." )
//        @PropertyName ( "Height, cm" )
        public double getHeight()
        {
            return height;
        }
        public void setHeight(double height)
        {
            this.height = height;
        }

                @PropertyName ( "Вес, кг" )
        @PropertyDescription ( "Вес." )
//        @PropertyName ( "Weight, kg" )
        public double getWeight()
        {
            return weight;
        }
        public void setWeight(double weight)
        {
            this.weight = weight;
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
            add( "age" );
            add( "height" );
            add( "weight" );
        }
    }

        @PropertyName ( "Режим" )
//    @PropertyName ( "Regime" )
    public String getRegime()
    {
        return regime;
    }
    public void setRegime(String regime)
    {
        this.regime = regime;
    }

        @PropertyName ( "Биохимия" )
//    @PropertyName ( "Biochemistry" )
    public Biochemistry getBiochemistry()
    {
        return biochemistry;
    }
    public void setBiochemistry(Biochemistry biochemistry)
    {
        this.biochemistry = biochemistry;
    }

        @PropertyName ( "Результат" )
//    @PropertyName ( "Result path" )
    public DataElementPath getResultPath()
    {
        return resultPath;
    }
    public void setResultPath(DataElementPath resultPath)
    {
        this.resultPath = resultPath;
    }

        @PropertyName ( "Популяция, экспресс" )
//    @PropertyName ( "Population path" )
    public DataElementPath getFastPopulationPath()
    {
        return fastPopulationPath;
    }
    public void setFastPopulationPath(DataElementPath fastPopulationPath)
    {
        this.fastPopulationPath = fastPopulationPath;
    }

    @PropertyName ( "Популяция, стандарт" )
    // @PropertyName ( "Population path" )
    public DataElementPath getSlowPopulationPath()
    {
        return slowPopulationPath;
    }
    public void setSlowPopulationPath(DataElementPath slowPopulationPath)
    {
        this.slowPopulationPath = slowPopulationPath;
    }

    public Diagram getDiagram(String drug)
    {
        if( getRegime().equals( FAST ) )
        {
            Diagram diagram = this.fastRegimeDiagramPath.getDataElement( Diagram.class );
            EModel role = diagram.getRole( EModel.class );

            for( String param : getDrugParameters() )
            {
                if( role.getVariable( param ) != null )
                    role.getVariable( param ).setInitialValue( 0.0 );
            }
            String paramName = getParameter( drug );
            Variable var = role.getVariable( paramName );
            if (var == null)
                System.out.println( "Variable "+paramName+" not found" );
            else
            role.getVariable( getParameter( drug ) ).setInitialValue( 1.0 );
            return diagram;
        }
        else if( getRegime().equals( NORMAL ) )
        {
            return getNormalDiagram( this.slowRegimeDiagramPath, drug );
        }
        return null;
    }

    public static Diagram getNormalDiagram(DataElementPath path, String drug)
    {
        Diagram diagram = path.getDataElement( Diagram.class );

        EModel heartRole = ( (SubDiagram)diagram.get( "Heart" ) ).getDiagram().getRole( EModel.class );
        EModel kidneyRole = ( (SubDiagram)diagram.get( "Kidney" ) ).getDiagram().getRole( EModel.class );

        for( String param : getDrugParameters() )
        {
            Variable heartVar = heartRole.getVariable( param );

            if( heartVar != null )
                heartVar.setInitialValue( 0.0 );

            Variable kidneyVar = kidneyRole.getVariable( param );
            if( kidneyVar != null )
                kidneyVar.setInitialValue( 0.0 );
        }

        String paramName = getParameter( drug );

        Variable heartVar = heartRole.getVariable( paramName );
        if( heartVar != null )
            heartVar.setInitialValue( 1.0 );

        Variable kidneyVar = kidneyRole.getVariable( paramName );
        if( kidneyVar != null )
            kidneyVar.setInitialValue( 1.0 );

        return diagram;
    }

    public static String[] getDrugParameters()
    {
        return new String[] {"Losartan_50", "Losartan_100", "Lortenza_5_100", "Aliskiren_150", "Aliskiren_300", "Amlodipine_5",
                "Amlodipine_10", "Enalapril_20", "HCTZ_12_5", "HCTZ_25", "Lisinopril_5", "Bisoprolol"};
    }


    public static String getParameter(String drug)
    {
        switch( drug )
        {
            case LOSARTAN_50_MG:
            {
                return "Losartan_50";
            }
            case LOSARTAN_100_MG:
            {
                return "Losartan_100";
            }
            case ALISKIREN_150_MG:
            {
                return "Aliskiren_150";
            }
            case ALISKIREN_300_MG:
            {
                return "Aliskiren_300";
            }
            case AMLODIPINE_5_MG:
            {
                return "Amlodipine_5";
            }
            case AMLODIPINE_10_MG:
            {
                return "Amlodipine_10";
            }
            case ENALAPRIL_20_MG:
            {
                return "Enalapril_20";
            }
            case HCTZ_12_5_MG:
            {
                return "HCTZ_12_5";
            }
            case HCTZ_25_MG:
            {
                return "HCTZ_25";
            }
            case DIROTON_5_MG:
            {
                return "Lisinopril_5";
            }
            case LORTENZA_5_100_MG:
            {
                return "Lortenza_5_100";
            }
            case BISOPROLOL_10_MG:
            {
                return "Bisoprolol";
            }
            case PLACEBO:
                return "";
        }
        return null;
    }

        @PropertyName ( "Размер популяции" )
//    @PropertyName ( "Population size" )
    public int getPopulationSize()
    {
        return populationSize;
    }
    public void setPopulationSize(int populationSize)
    {
        this.populationSize = populationSize;
    }

        @PropertyName ( "Период лечения, ч." )
//    @PropertyName ( "Treatment duration, hours" )
    public double getTime()
    {
        return time;
    }
    public void setTime(double time)
    {
        this.time = time;
    }

    //    @PropertyName("Графики")
    @PropertyName ( "Plots" )
    public boolean isShowPlots()
    {
        return showPlots;
    }
    public void setShowPlots(boolean showPlots)
    {
        this.showPlots = showPlots;
    }
    
    @PropertyName("Fast regime model")
    public DataElementPath getFastRegimeDiagramPath()
    {
        return fastRegimeDiagramPath;
    }
    public void setFastRegimeDiagramPath(DataElementPath fastRegimeDiagramPath)
    {
        this.fastRegimeDiagramPath = fastRegimeDiagramPath;
    }
    
    @PropertyName("Slow regime model")
    public DataElementPath getSlowRegimeDiagramPath()
    {
        return slowRegimeDiagramPath;
    }
    public void setSlowRegimeDiagramPath(DataElementPath slowRegimeDiagramPath)
    {
        this.slowRegimeDiagramPath = slowRegimeDiagramPath;
    }
}
