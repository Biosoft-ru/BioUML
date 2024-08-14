package biouml.plugins.gtrd.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;

import ru.biosoft.jobcontrol.StackProgressJobControl;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.bsa.transformer.SiteModelTransformer;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@ClassIcon ( "resources/compute_profile_thresholds.gif" )
public class ComputeProfileThresholds extends AnalysisMethodSupport<ComputeProfileThresholds.Parameters>
{
    
    public ComputeProfileThresholds(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        SiteModelCollection inputModels = parameters.getInputModels().getDataElement( SiteModelCollection.class );
        
        SiteModelCollection result = SiteModelTransformer.createCollection(parameters.getOutputModels());
        
        List<SiteModel> modelList = inputModels.stream().collect( Collectors.toList() );
        double[] thresholds = parameters.getMethod().computeThresholds( modelList, jobControl );
        for(int i = 0; i < modelList.size(); i++)
        {
            SiteModel model = modelList.get( i );
            double t = thresholds[i];
            SiteModel clone = model.clone( result, model.getName() );
            clone.setThreshold( t );
            result.put( clone );
        }
        
        parameters.getOutputModels().save( result );
        return result;
    }


    public static class Parameters extends AbstractAnalysisParameters
    {
        public static final Map<String, Method> METHODS = StreamEx
                .<Method> of( new MinFNMethod(), new MinFPMethod() )
                .mapToEntry( Method::getName, Function.identity() )
                .toCustomMap( LinkedHashMap::new );
        private DataElementPath inputModels;
        private Method method = METHODS.values().iterator().next();
        private DataElementPath outputModels;
        
        @PropertyName("Input models")
        @PropertyDescription("Collection of site models")
        public DataElementPath getInputModels()
        {
            return inputModels;
        }
        public void setInputModels(DataElementPath inputModels)
        {
            Object oldValue = this.inputModels;
            this.inputModels = inputModels;
            firePropertyChange( "inputModels", oldValue, inputModels );
        }
        
        @PropertyName("Method")
        @PropertyDescription("Method of threshold computation")
        public String getMethodName()
        {
            return method.getName();
        }
        public void setMethodName(String methodName)
        {
            setMethod(METHODS.get( methodName ));
        }
        
        @PropertyName("Method parameters")
        @PropertyDescription("Parameters for threshold computation")
        public Method getMethod()
        {
            return method;
        }
        public void setMethod(Method method)
        {
            Object oldValue = this.method;
            this.method = method;
            firePropertyChange( "method", oldValue, method );
        }
        
        @PropertyName("Output models")
        @PropertyDescription("Resulting collection of site models")
        public DataElementPath getOutputModels()
        {
            return outputModels;
        }
        public void setOutputModels(DataElementPath outputModels)
        {
            Object oldValue = this.outputModels;
            this.outputModels = outputModels;
            firePropertyChange( "outputModels", oldValue, outputModels );
        }
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "inputModels" ).inputElement( SiteModelCollection.class ).add();
            property( "methodName" ).tags( bean->StreamEx.ofKeys( Parameters.METHODS) ).add( );
            property( "method" ).add();
            property( "outputModels" ).outputElement( SiteModelCollection.class ).add();
        }
    }
    
    public static interface Method extends JSONBean
    {
        String getName();
        double[] computeThresholds(List<SiteModel> models, StackProgressJobControl jc);
    }
    
    @PropertyName("Min FN")
    @PropertyDescription("Minimal false negative")
    public static class MinFNMethod extends OptionEx implements Method
    {
        private static final String METHOD_MIN_FN = "minFN";
        
        @Override
        public String getName()
        {
            return METHOD_MIN_FN;
        }
        
        private int sampleSize = 1000;
        private double errorRate = 0.1;

        @PropertyName("Sample size")
        public int getSampleSize()
        {
            return sampleSize;
        }
        
        public void setSampleSize(int sampleSize)
        {
            int oldValue = this.sampleSize;
            this.sampleSize = sampleSize;
            firePropertyChange( "sampleSize", oldValue, sampleSize );
        }

        @PropertyName("Error rate")
        public double getErrorRate()
        {
            return errorRate;
        }

        public void setErrorRate(double errorRate)
        {
            double oldValue = this.errorRate;
            this.errorRate = errorRate;
            firePropertyChange( "errorRate", oldValue, errorRate );
        }
        
        private Random rnd = new Random();

        @Override
        public double[] computeThresholds(List<SiteModel> models, StackProgressJobControl jc)
        {
            double[] result = new double[models.size()];
            for(int i = 0; i < models.size(); i++)
            {
                jc.setPreparedness( i * 100 / models.size()  );
                result[i] = computeMinFN(models.get( i ));
            }
            return result;
        }

        private double computeMinFN(SiteModel siteModel)
        {
            FrequencyMatrix fm = getFrequencyMatrix(siteModel);
            List<Sequence> sample = sampleMatrix(fm, sampleSize);
            if(siteModel instanceof IPSSiteModel)
            {
                IPSSiteModel ipsModel = (IPSSiteModel)siteModel;
                sample = StreamEx.of( sample ).map( s->extendSequence(s, ipsModel.getWindow() / 2 - fm.getLength() / 2) ).toList();
            }
            double[] scores = new double[sample.size()];
            for(int i = 0; i < scores.length; i++)
            {
                Sequence seq = sample.get( i );
                scores[i] = siteModel.getScore( seq, seq.getStart() );
            }
            Arrays.sort( scores );
            return scores[(int)(scores.length * errorRate)];
        }
        
        private Sequence extendSequence(Sequence s, int overhangs)
        {
            byte[] result = new byte[s.getLength() + 2*overhangs];
            Alphabet alphabet = s.getAlphabet();
            for(int i = 0; i < overhangs; i++)
                result[i] = randomLetter( alphabet );
            for(int i = 0; i < s.getLength(); i++)
                result[i + overhangs] = s.getLetterAt( s.getStart() + i );
            for(int i = 0; i < overhangs; i++)
                result[i + overhangs + s.getLength()] = randomLetter( alphabet );
            return new LinearSequence( result, s.getAlphabet() );
        }
        
        private byte randomLetter(Alphabet alphabet)
        {
            int idx = rnd.nextInt( alphabet.basicSize() );
            byte letterCode = alphabet.basicCodes()[idx];
            return alphabet.codeToLetterMatrix()[letterCode];
        }

        private List<Sequence> sampleMatrix(FrequencyMatrix fm, int n)
        {
            List<Sequence> result = new ArrayList<>( n );
            Alphabet alphabet = fm.getAlphabet();
            byte[] basicCodes = alphabet.basicCodes();
            
            for(int i = 0; i < n; i++)
            {
                byte[] seq = new byte[fm.getLength()];
                for(int pos = 0; pos < fm.getLength(); pos++)
                {
                    double p = rnd.nextDouble();
                    double sum = 0;
                    for(byte letterCode : basicCodes)
                    {
                        sum += fm.getFrequency( pos, letterCode );
                        if(p <= sum)
                        {
                            seq[pos] = alphabet.codeToLetterMatrix()[letterCode];
                            break;
                        }
                    }
                }
                result.add( new LinearSequence( seq, alphabet ) );
            }
            
            return result;
        }

        private FrequencyMatrix getFrequencyMatrix(SiteModel siteModel)
        {
            if(siteModel instanceof WeightMatrixModel)
                return ((WeightMatrixModel)siteModel).getFrequencyMatrix();
            if(siteModel instanceof IPSSiteModel)
                return ((IPSSiteModel)siteModel).getMatrices()[0].getFrequencyMatrix();
            throw new RuntimeException("Frequency matrix not available for " + siteModel.getName());
        }
    }
    
    public static class MinFNMethodBeanInfo extends BeanInfoEx2<MinFNMethod>
    {
        public MinFNMethodBeanInfo()
        {
            super( MinFNMethod.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "sampleSize" ).add();
            property( "errorRate" ).add();
        }
    }
    
    @PropertyName( "Min FP" )
    @PropertyDescription( "Minimal false positive" )
    public static class MinFPMethod extends OptionEx implements Method
    {
        private static final String METHOD_MIN_FP = "minFP";
        @Override
        public String getName()
        {
            return METHOD_MIN_FP;
        }
        
        private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl();
        
        @PropertyName("Ensembl")
        @PropertyDescription("Ensembl database version")
        public EnsemblDatabase getEnsembl()
        {
            return ensembl;
        }
        public void setEnsembl(EnsemblDatabase ensembl)
        {
            Object oldValue = this.ensembl;
            this.ensembl = ensembl;
            firePropertyChange( "ensembl", oldValue, ensembl );
        }
        
        private double falsePositiveRate = 0.0001;
        @PropertyName("False positive rate")
        @PropertyDescription("False positive rate")
        public double getFalsePositiveRate()
        {
            return falsePositiveRate;
        }
        public void setFalsePositiveRate(double falsePositiveRate)
        {
            double oldValue = this.falsePositiveRate;
            this.falsePositiveRate = falsePositiveRate;
            firePropertyChange( "falsePositiveRate", oldValue, falsePositiveRate );
        }
        
        @Override
        public double[] computeThresholds(List<SiteModel> models, StackProgressJobControl jc)
        {
            jc.pushProgress( 0, 30 );
            List<Sequence> noSet = createNoSet(jc);
            jc.popProgress();
            
            jc.pushProgress( 30, 100 );
            double[] result = new double[models.size()];
            for(int modelIdx = 0 ; modelIdx < models.size(); modelIdx++)
            {
                jc.setPreparedness( modelIdx * 100 / models.size() );
                SiteModel sm = models.get( modelIdx );
                int totalLength = StreamEx.of( noSet ).mapToInt( Sequence::getLength )
                        .remove( l -> l < sm.getLength() ).map( l -> l-sm.getLength()+1 ).sum();
                double[] scores = new double[totalLength];
                int idx = 0;
                for(Sequence seq : noSet)
                {
                    if(seq.getLength() < sm.getLength())
                        continue;
                    for(int i = seq.getStart(); i < seq.getStart() + seq.getLength() - sm.getLength() + 1; i++)
                        scores[idx++] = sm.getScore( seq, i );
                }
                Arrays.sort( scores );
                result[modelIdx] = scores[totalLength - (int)(falsePositiveRate * totalLength)];
            }
            jc.popProgress();
            return result;
        }
        
        private List<Sequence> createNoSet(StackProgressJobControl jc)
        {
            List<Sequence> noSet = new ArrayList<>();
            jc.forCollection( ensembl.getPrimarySequencesPath().getChildren(), chrPath -> {
                fetchSecondExons( chrPath, noSet );
                return true;
            } );
            return noSet;
        }
        
        private void fetchSecondExons(DataElementPath chrPath, List<Sequence> acc)
        {
            Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
            DataCollection<Site> sites = ensembl.getTranscriptsTrack().getSites( chrPath.toString(), 0, chrSeq.getLength() );
            List<Interval> secondExons = new ArrayList<>();
            for(Site transcript : sites)
            {
                if( transcript.getProperties().getProperty( "translation" ) == null)
                    continue;
                Interval[] exons = (Interval[])transcript.getProperties().getValue( "exons" );
                if(exons != null && exons.length > 1)
                {
                    Interval secondExon = exons[1];
                    secondExons.add( secondExon );
                }
            }
            if(secondExons.isEmpty())
                return;
            Collections.sort( secondExons );
            List<Interval> joined = new ArrayList<>();
            int right= secondExons.get( 0 ).getTo();
            int left = secondExons.get( 0 ).getFrom();
            for(int i = 1; i < secondExons.size(); i++)
            {
                Interval cur = secondExons.get( i );
                if(cur.getFrom() > right + 1)
                {
                    joined.add( new Interval( left, right ) );
                    left = cur.getFrom();
                    right = cur.getTo();
                }
                else
                {
                    right = Math.max( right, cur.getTo() );
                }
            }
            joined.add( new Interval( left, right ) );
            
            for(Interval i : joined)
            {
                Sequence seq = new LinearSequence( new SequenceRegion( chrSeq, i, false, false ) );
                acc.add( seq );
            }
            return;
        }
        
    }
    
    public static class MinFPMethodBeanInfo extends BeanInfoEx2<MinFPMethod>
    {
        public MinFPMethodBeanInfo()
        {
            super( MinFPMethod.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            property( "ensembl" ).add();
            property( "falsePositiveRate" ).add();
        }
    }
}
