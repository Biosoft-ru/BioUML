package biouml.plugins.wdl.nextflow.ast;


import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.groovy.ast.ASTNode;

import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;


public class NextflowParser
{
    public static List<ASTNode> parse(String script, boolean statementsOnly, CompilePhase compilePhase)
    {
        final String scriptClassName = makeScriptClassName();
        GroovyCodeSource codeSource = new GroovyCodeSource(script, scriptClassName + ".groovy", "/groovy/script");
        CompilationUnit cu = new CompilationUnit(CompilerConfiguration.DEFAULT, codeSource.getCodeSource(),
                AccessController.doPrivileged((PrivilegedAction<GroovyClassLoader>) GroovyClassLoader::new));
        cu.addPhaseOperation(source -> IncludeTransformer.transformIncludes(source.getAST()), Phases.CONVERSION);
        
        cu.addSource(codeSource.getName(), script);
      
        cu.compile(compilePhase.getPhaseNumber());

        // collect all the ASTNodes into the result, possibly ignoring the script body if desired
        List<ASTNode> result = cu.getAST().getModules().stream().reduce(new LinkedList<>(), (acc, node) -> {
            BlockStatement statementBlock = node.getStatementBlock();
            if (null != statementBlock) {
                acc.add(statementBlock);
            }
            acc.addAll(
                    node.getClasses().stream()
                        .filter(c -> !(statementsOnly && scriptClassName.equals(c.getName())))
                        .collect(Collectors.toList())
            );

            return acc;
        }, (o1, o2) -> o1);

        return result;
    }
    
    private static String makeScriptClassName() {
        return "Script" + System.nanoTime();
    }
}
