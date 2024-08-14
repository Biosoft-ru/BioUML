package ru.biosoft.server.servlets.webservices._test;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;

/**
 * @author lan
 *
 */
public class TestImportProvider extends AbstractProviderTest
{
    public void testDoImport() throws Exception
    {
        String sessionId = WebSession.getCurrentSession().getSessionId();
        String fileID = "file";
        String suffix = ".txt";
        String jobID = "job";
        String fileName = "testFile.txt";
        File file = new File(WebServicesServlet.UPLOAD_DIRECTORY, "upload_" + sessionId + "_" + fileID + suffix);
        String content = "Test file content";
        try (PrintWriter pw = new PrintWriter( file ))
        {
            pw.println( content );
        }
        WebSession.getCurrentSession().putValue("uploadedFileSuffix_" + fileID, ".txt");
        WebSession.getCurrentSession().putValue("uploadedFile_" + fileID, fileName);

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test", TextDataElement.class, null);
        CollectionFactory.registerRoot(vdc);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("type", "import");
        arguments.put("fileID", fileID);
        arguments.put("jobID", jobID);
        arguments.put("de", "test");
        arguments.put("format", "Text file (*.txt)");
        JsonObject responseJSON = getResponseJSON("import", arguments);
        assertValueEquals(jobID, responseJSON);
        waitForJob(jobID);

        DataElement de = vdc.get(fileName);
        assertTrue(de instanceof TextDataElement);
        assertEquals(content, ((TextDataElement)de).getContent().trim());
    }
}
