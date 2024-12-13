package biouml.plugins.beakerx;

import biouml.plugins.server.access.ClientDataCollection;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.StreamEx;
		
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

import ru.biosoft.access.ImageElement;
import ru.biosoft.access.CollectionFactory;
import ru.biosoft.access.DataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.View.ModelResolver;
import ru.biosoft.graphics.access.DataElementModelResolver;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.plugins.javascript.JScriptShellEnvironment;
import ru.biosoft.plugins.javascript.JScriptContext;

import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.tomcat.TomcatConnection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.NetworkConfigurator;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.UserPermissions;

import com.twosigma.beakerx.BeakerXCommRepository;
import com.twosigma.beakerx.NamespaceClient;
import com.twosigma.beakerx.evaluator.BxInspect;
import com.twosigma.beakerx.evaluator.ClasspathScannerImpl;
import com.twosigma.beakerx.evaluator.Evaluator;
import com.twosigma.beakerx.handler.KernelHandler;
import com.twosigma.beakerx.kernel.BeakerXJsonConfig;
import com.twosigma.beakerx.kernel.BxKernelRunner;
import com.twosigma.beakerx.kernel.Configuration;
import com.twosigma.beakerx.kernel.CustomMagicCommandsEmptyImpl;
import com.twosigma.beakerx.kernel.EvaluatorParameters;
import com.twosigma.beakerx.kernel.Kernel;
import com.twosigma.beakerx.kernel.KernelConfigurationFile;
import com.twosigma.beakerx.kernel.KernelSocketsFactoryImpl;
import com.twosigma.beakerx.kernel.handler.CommOpenHandler;
import com.twosigma.beakerx.kernel.magic.autocomplete.MagicCommandAutocompletePatternsImpl;
import com.twosigma.beakerx.kernel.magic.command.FileServiceImpl;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandConfiguration;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandConfigurationImpl;
import com.twosigma.beakerx.kernel.magic.command.MavenJarResolverServiceImpl;
import com.twosigma.beakerx.kernel.restserver.impl.GetUrlArgHandler;
import com.twosigma.beakerx.message.Message;


public class BeakerXKernel extends Kernel 
{
    public BeakerXKernel( final String sessionId, final Evaluator evaluator, Configuration configuration ) 
    {
        super( sessionId, evaluator, configuration );
    }

    @Override
    public CommOpenHandler getCommOpenHandler( Kernel kernel ) 
    {
        return new BeakerXtoBioUMLCommOpenHandler( kernel );
    }

    @Override
    public KernelHandler<Message> getKernelInfoHandler(Kernel kernel) 
    {
        return new BeakerXtoBioUMLKernelInfoHandler( kernel );
    }
}
