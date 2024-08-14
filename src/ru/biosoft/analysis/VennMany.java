package ru.biosoft.analysis;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.TempFiles;

public class VennMany extends AnalysisMethodSupport<VennManyParameters>
{
    public VennMany(DataCollection<?> origin, String name)
    {
        super(origin, name, new VennManyParameters());
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        File imgFile = TempFiles.file("defaultVenn.png");
        log.info( imgFile.getAbsolutePath() );
        String rcmd = makeRCall(imgFile);
        runR(rcmd);
        ImageDataElement ide = createIDE(imgFile);
        return rcmd;
    }
    
    private ImageDataElement createIDE(File imgFile) throws IOException
    {
        BufferedImage image = ImageIO.read(imgFile);
        DataElementPath imagePath = parameters.getOutVenn();
        ImageDataElement imageDE = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), image );
        imagePath.save( imageDE );
        return imageDE;
    }

    private void runR(final String rcmd) throws Exception 
    {
        final LogScriptEnvironment env = new LogScriptEnvironment(log);
        SecurityManager.runPrivileged(() -> ScriptTypeRegistry.execute("R", rcmd, env, false));
    }
    
    private String makeRCall(File imgFile) throws IOException 
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "library('venn');\n" );
        sb.append( "png(" + "'" + imgFile.getAbsolutePath() + "'" + ",800,800);\n" );
        sb.append( "venn("+
                    String.valueOf(parameters.getCases()) +
                    ", counts=" +
                    "c("+ parameters.getCounts() + ")" +
                    ", zcolor='style');\n" );
        sb.append( "dev.off();\n" );
        log.info( sb.toString() );
        return sb.toString();
    }
}
