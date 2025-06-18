package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import gnu.trove.list.array.TDoubleArrayList;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Entry;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.PValueCutoff;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SiteModelTransformedCollection;
import ru.biosoft.bsa.analysis.CustomWeightsModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.LogOddsWeightMatrixModel;
import ru.biosoft.bsa.analysis.LogWeightMatrixModel;
import ru.biosoft.bsa.analysis.MatchSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TextUtil2;

public class SiteModelTransformer extends AbstractTransformer<Entry, SiteModel>
{
    private static final String DELIM = "//";
    private static final String ID = "ID";
    private static final String TYPE = "TYPE";
    private static final String THRESHOLD = "THRESHOLD";
    private static final String THRESHOLD_TEMPLATE = "THRESHOLD_TEMPLATE";

    private static final String MATRIX = "MATRIX";

    private static final String BACKGROUND = "BG";

    private static final String CORE_START = "CORE_START";
    private static final String CORE_LENGTH = "CORE_LENGTH";
    private static final String CORE_CUTOFF = "CORE_CUTOFF";

    private static final String DIST_MIN = "DIST_MIN";
    private static final String WINDOW_SIZE = "WINDOW_SIZE";
    private static final String WEIGHT_MATRIX = "WEIGHT_MATRIX";
    private static final String FREQUENCY_MATRIX = "FREQUENCY_MATRIX";

    private static final String CUSTOM_WEIGHTS = "CUSTOM_WEIGHTS";

    private static final String PVAL = "PVAL";

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<SiteModel> getOutputType()
    {
        return SiteModel.class;
    }

    @Override
    public boolean isOutputType(Class<?> type)
    {
        return SiteModel.class.isAssignableFrom(type);
    }

    @Override
    public SiteModel transformInput(Entry entry) throws Exception
    {
        BufferedReader reader = new BufferedReader(entry.getReader());

        String name = null;
        String type = null;
        boolean isThresholdSet = false;
        double threshold = 0;

        double[] background = null;

        String matrixPath = null;
        int coreStart = -1;
        int coreLength = -1;
        double coreCutoff = -1;

        int distMin = -1;
        int windowSize = -1;
        List<String> weightMatrixPaths = new ArrayList<>();
        List<String> frequencyMatrixPaths = new ArrayList<>();
        Map<String, String> thresholdTemplates = new HashMap<>();

        double[][] customWeights = null;

        TDoubleArrayList pvalues = new TDoubleArrayList();
        TDoubleArrayList cutoffs = new TDoubleArrayList();

        String line;
        while( ( ( line = reader.readLine() ) != null ) && ! ( line.equals(DELIM) ) )
        {
            int index = line.indexOf(" ");
            String tag;
            String value;
            if( index == -1 )
            {
                tag = line;
                value = "";
            }
            else
            {
                tag = line.substring(0, index);
                value = line.substring(index).trim();
            }

            if( tag.equals(ID) )
                name = value;
            else if( tag.equals(TYPE) )
                type = value;
            else if( tag.equals(THRESHOLD) )
            {
                threshold = Double.parseDouble(value);
                isThresholdSet = true;
            }
            else if( tag.equals(THRESHOLD_TEMPLATE) )
            {
                String[] fields = TextUtil2.split(value, ':');
                if( fields.length == 2 )
                {
                    try
                    {
                        thresholdTemplates.put(fields[0].trim(), fields[1].trim());
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
            else if( tag.equals(MATRIX) )
                matrixPath = value;
            else if( tag.equals(CORE_START) )
                coreStart = Integer.parseInt(value);
            else if( tag.equals(CORE_LENGTH) )
                coreLength = Integer.parseInt(value);
            else if( tag.equals(CORE_CUTOFF) )
                coreCutoff = Double.parseDouble(value);
            else if( tag.equals(DIST_MIN) )
                distMin = Integer.parseInt(value);
            else if( tag.equals(WINDOW_SIZE) )
                windowSize = Integer.parseInt(value);
            else if( tag.equals(WEIGHT_MATRIX) )
                weightMatrixPaths.add(value);
            else if( tag.equals(FREQUENCY_MATRIX) )
                frequencyMatrixPaths.add(value);
            else if( tag.equals(BACKGROUND))
                background = StreamEx.split(value, "\\s").mapToDouble( Double::parseDouble ).toArray();
            else if( tag.equals( CUSTOM_WEIGHTS ) )
            {
                if( matrixPath == null )
                    throw new Exception( "MATRIX should be set before CUSTOM_WEIGHTS" );
                FrequencyMatrix matrix = DataElementPath.create( matrixPath ).getDataElement( FrequencyMatrix.class );
                customWeights = WeightMatrixTransformer.parseMatrix( reader, matrix.getLength(), matrix.getAlphabet() );
            }
            else if( tag.equals( PVAL ))
            {
                String[] parts = value.split( " ", 2 );
                double pval = Double.parseDouble( parts[0] );
                double scoreCutoff = Double.parseDouble( parts[1] );
                pvalues.add( pval );
                cutoffs.add( scoreCutoff );
            }
            else
                throw new Exception("Unknown tag " + tag);
        }

        if( ( name == null ) || ( type == null ) || !isThresholdSet )
            throw new Exception("Incomlete entry for weight matrix model");

        SiteModel model = null;

        if( type.equals( WeightMatrixModel.class.getName() ) || type.equals( LogWeightMatrixModel.class.getName() )
                || type.equals( LogOddsWeightMatrixModel.class.getName() ) || type.equals( MatchSiteModel.class.getName() )
                || type.equals( CustomWeightsModel.class.getName() ) )
        {
            if( matrixPath == null )
                throw new Exception("Incomlete entry for weight matrix site model");
            FrequencyMatrix weightMatrix = DataElementPath.create(matrixPath).getDataElement(FrequencyMatrix.class);
            if(type.equals(WeightMatrixModel.class.getName()))
                model = new WeightMatrixModel(name, getTransformedCollection(), weightMatrix, threshold);
            else if(type.equals(LogWeightMatrixModel.class.getName()))
                model = new LogWeightMatrixModel(name, getTransformedCollection(), weightMatrix, threshold);
            else if(type.equals(LogOddsWeightMatrixModel.class.getName()))
            {
                if(background == null || background.length != weightMatrix.getAlphabet().basicSize())
                    throw new Exception("Invalid background");
                model = new LogOddsWeightMatrixModel(name, getTransformedCollection(), weightMatrix, threshold, background);
            }
            else if( type.equals( MatchSiteModel.class.getName() ) )
            {
                if( ( coreStart < 0 ) || ( coreLength < 0 ) || ( coreCutoff < 0 ) )
                    throw new Exception("invalid core");
                model = new MatchSiteModel(name, getTransformedCollection(), weightMatrix, threshold, coreCutoff, coreStart,
                        coreLength);
            }else if( type.equals( CustomWeightsModel.class.getName() ))
            {
                if( customWeights == null )
                    throw new Exception( "CUSTOM_WEIGHTS not set" );
                model = new CustomWeightsModel( name, getTransformedCollection(), weightMatrix, threshold, customWeights );
            }
        }
        else if( type.equals(IPSSiteModel.class.getName()) )
        {
            if( ( distMin < 0 ) || ( windowSize < 0 ) )
                throw new Exception("Incomlete entry for ips site model");

            if( frequencyMatrixPaths.isEmpty() )
            {
                WeightMatrixModel[] subModels = StreamEx.of( weightMatrixPaths )
                        .map( wmPath -> DataElementPath.create( wmPath ).getDataElement( WeightMatrixModel.class ) )
                        .toArray( WeightMatrixModel[]::new );
                model = new IPSSiteModel( name, getTransformedCollection(), subModels, threshold, distMin, windowSize );
            }
            else
            {
                FrequencyMatrix[] matrices = StreamEx.of( frequencyMatrixPaths )
                        .map( path -> DataElementPath.create( path ).getDataElement( FrequencyMatrix.class ) )
                        .toArray( FrequencyMatrix[]::new );
                model = new IPSSiteModel( name, getTransformedCollection(), matrices, threshold, distMin, windowSize );
            }
        }
        else
            throw new Exception("Unknown site model type " + type);

        if(model != null && !thresholdTemplates.isEmpty())
        {
            model.setThresholdTemplates(thresholdTemplates);
        }

        if(model != null && cutoffs.size() > 0)
        {
            model.setPValueCutoff( new PValueCutoff( cutoffs.toArray(), pvalues.toArray() ) );
        }

        return model;
    }

    @Override
    public Entry transformOutput(SiteModel output) throws Exception
    {
        StringBuffer buffer = new StringBuffer();
        put(buffer, ID, output.getName());

        if( output instanceof MatchSiteModel )
            writeTransfacModel((MatchSiteModel)output, buffer);
        else if( output instanceof IPSSiteModel )
            writeIPSSiteModel((IPSSiteModel)output, buffer);
        else if(output instanceof LogOddsWeightMatrixModel)
            writeLogOddsWeightMatrixModel((LogOddsWeightMatrixModel)output, buffer);
        else if (output instanceof CustomWeightsModel )
            writeCustomWeightsModel((CustomWeightsModel)output, buffer);
        else if( output instanceof WeightMatrixModel )
            writeWeightMatrixModel((WeightMatrixModel)output, buffer);
        else
            throw new Exception("Unknown site model '" + output.getClass() + "'");

        buffer.append(DELIM + lineSep);
        return new Entry(getPrimaryCollection(), output.getName(), "" + buffer, Entry.TEXT_FORMAT);
    }

    private void writeCustomWeightsModel(CustomWeightsModel model, StringBuffer buffer)
    {
        writeWeightMatrixModel( model, buffer );
        buffer.append( CUSTOM_WEIGHTS ).append( lineSep );
        double[][] weights = model.getWeights();
        for( int i = 0; i < weights.length; i++ )
        {
            buffer.append(i + 1);
            Alphabet alphabet = model.getAlphabet();
            for( byte code : alphabet.basicCodes() )
            {
                String letters = alphabet.codeToLetters(code).toUpperCase();
                buffer.append(" " + letters + ":");
                buffer.append(weights[i][code]);
            }
            buffer.append(" " + lineSep);
        }
    }

    private void writeSiteModel(SiteModel model, StringBuffer buffer)
    {
        put(buffer, TYPE, model.getClass().getName());
        put(buffer, THRESHOLD, String.valueOf(model.getThreshold()));
        for(String templateName: model.getThresholdTemplates())
        {
            put(buffer, THRESHOLD_TEMPLATE, templateName+": "+model.getTemplateData(templateName));
        }
        PValueCutoff pValueCutoff = model.getPValueCutoff();
        if(pValueCutoff != null)
        {
            double[] pvalues = pValueCutoff.getPvalues();
            double[] cutoffs = pValueCutoff.getCutoffs();
            for(int i = 0; i < pvalues.length; i++)
            {
                put(buffer, PVAL, pvalues[i] + " " + cutoffs[i]);
            }

        }
    }

    private void writeWeightMatrixModel(WeightMatrixModel model, StringBuffer buffer)
    {
        writeSiteModel(model, buffer);
        put(buffer, MATRIX, DataElementPath.create(model.getFrequencyMatrix()));
    }

    private void writeTransfacModel(MatchSiteModel model, StringBuffer buffer)
    {
        writeWeightMatrixModel(model, buffer);
        put(buffer, CORE_START, model.getCoreStart());
        put(buffer, CORE_LENGTH, model.getCoreLength());
        put(buffer, CORE_CUTOFF, model.getCoreCutoff());
    }

    private void writeLogOddsWeightMatrixModel(LogOddsWeightMatrixModel model, StringBuffer buffer)
    {
        writeWeightMatrixModel(model, buffer);
        put(buffer, BACKGROUND, DoubleStreamEx.of(model.getBackground()).joining( " " ));
    }

    private void writeIPSSiteModel(IPSSiteModel model, StringBuffer buffer)
    {
        writeSiteModel(model, buffer);
        put(buffer, DIST_MIN, model.getDistMin());
        put(buffer, WINDOW_SIZE, model.getWindow());
        for( WeightMatrixModel wm : model.getMatrices() )
            put(buffer, FREQUENCY_MATRIX, DataElementPath.create(wm.getFrequencyMatrix()));
    }

    private void put(StringBuffer buffer, String tag, Object value)
    {
        buffer.append(tag + " " + value + lineSep);
    }

    public static SiteModelCollection createCollection(DataElementPath path) throws Exception
    {
        Properties primary = new ExProperties();
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        primary.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, path.getName() + ".mdl");
        primary.setProperty(FileEntryCollection2.ENTRY_DELIMITERS_PROPERTY, " \t");
        primary.setProperty(FileEntryCollection2.ENTRY_START_PROPERTY, "ID");
        primary.setProperty(FileEntryCollection2.ENTRY_ID_PROPERTY, "ID");
        primary.setProperty(FileEntryCollection2.ENTRY_END_PROPERTY, "//");

        Properties transformed = new ExProperties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, SiteModelTransformedCollection.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.TRANSFORMER_CLASS, SiteModelTransformer.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, SiteModel.class.getName());

        Repository parentRepository = (Repository)DataCollectionUtils.getTypeSpecificCollection(path.optParentCollection(),
                TransformedDataCollection.class);

        SiteModelTransformedCollection result = (SiteModelTransformedCollection) CollectionFactoryUtils.createDerivedCollection(parentRepository, path.getName(), primary, transformed, null);
        result.setNotificationEnabled(false);
        result.setPropagationEnabled(false);
        result.getPrimaryCollection().setNotificationEnabled(false);
        result.getPrimaryCollection().setPropagationEnabled(false);
        return result;
    }
}
