package org.ek9lang.compiler.parsing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.DelegatingLexer;
import org.ek9lang.compiler.tokenizer.EK9Lexer;
import org.ek9lang.compiler.tokenizer.LexerPlugin;
import org.ek9lang.compiler.tokenizer.TokenStreamAssessment;
import org.ek9lang.core.utils.Glob;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

public class TestAllExamples
{
    @Test
    public void testValidEK9ExampleSource() throws Exception
    {
        OsSupport os = new OsSupport();
        URL rootDirectoryForTest = this.getClass().getResource("/examples");
        assertNotNull(rootDirectoryForTest);
        File examples = new File(rootDirectoryForTest.getPath());
        Glob ek9 = new Glob("**.ek9");
        for(File file : os.getFilesRecursivelyFrom(examples, ek9))
        {
            test(file, false);
            assessReadability(file);
        }
    }

    @Test
    public void testInvalidEK9ExampleSource() throws Exception
    {
        OsSupport os = new OsSupport();
        URL rootDirectoryForTest = this.getClass().getResource("/badExamples");
        assertNotNull(rootDirectoryForTest);
        File examples = new File(rootDirectoryForTest.getPath());
        Glob ek9 = new Glob("**.ek9");
        for(File file : os.getFilesRecursivelyFrom(examples, ek9))
            test(file, true);
    }

    private void assessReadability(File ek9SourceFile) throws Exception
    {
        ErrorListener errorListener = new ErrorListener();
        LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(new FileInputStream(ek9SourceFile)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        String readability = new TokenStreamAssessment().assess(lexer, false);
        System.out.println("Readability of " + ek9SourceFile.getName() + " is " + readability);
    }

    private  void test(File ek9SourceFile, boolean expectError) throws Exception
    {
        ErrorListener errorListener = new ErrorListener();
        LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(new FileInputStream(ek9SourceFile)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        EK9Parser parser = new EK9Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        long before = System.currentTimeMillis();
        EK9Parser.CompilationUnitContext context = parser.compilationUnit();
        long after = System.currentTimeMillis();

        System.out.println("Parsed " + ek9SourceFile.getName() + " in " + (after-before) + "ms. Expecting Error [" + expectError + "]");

        if(!expectError)
        {
            if(!errorListener.isErrorFree())
            {
                errorListener.getErrors().forEachRemaining(error -> {
                    System.out.println(error);
                });
            }
            assertTrue(errorListener.isErrorFree(), "Parsing of " + ek9SourceFile.getName() + " failed");
            assertNotNull(context);
        }
        else
        {
            assertFalse(errorListener.isErrorFree(), "Parsing of " + ek9SourceFile.getName() + " should have failed");
        }
    }

    private LexerPlugin getEK9Lexer(CharStream charStream)
    {
        return new DelegatingLexer(new EK9Lexer(charStream, EK9Parser.INDENT, EK9Parser.DEDENT));
    }
}
