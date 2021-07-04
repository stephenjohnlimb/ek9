package org.ek9lang.compiler.errors;

import junit.framework.TestCase;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.ek9lang.compiler.files.Module;
import org.ek9lang.compiler.files.Source;
import org.ek9lang.compiler.symbol.support.MatchResults;
import org.junit.Test;

/**
 * Most of the use of the ErrorListener will be driven by examples of
 * EK9 source code that is designed to 'fail' to compile.
 *
 * These examples of EK9 that fail to compile should be stored in 'badExamples' in the
 * resources directory. Ideally these bad example will be grouped in to subdirectories.
 *
 * So this unit test is just designed to to test the basic mechanisms.
 */
public class ErrorListenerTest
{
    @Test
    public void testReturningRequired()
    {
        ErrorListener underTest = new ErrorListener();
        underTest.raiseReturningRequired(createSyntheticToken(), "_EK9 Test");
        assertInError(underTest);
    }

    @Test
    public void testReturningRedundant()
    {
        ErrorListener underTest = new ErrorListener();
        underTest.raiseReturningRedundant(createSyntheticToken(), "_EK9 Test");
        assertInError(underTest);
    }

    @Test
    public void testSemanticErrorCreationWithModule()
    {
        ErrorListener underTest = new ErrorListener();
        Module module = new Module() {
            @Override
            public Source getSource()
            {
                return new Source()
                {
                    @Override
                    public String getFileName()
                    {
                        return "NoneSuch.ek9";
                    }
                };
            }
        };
        underTest.setModule(module);
        underTest.semanticError(createSyntheticToken(), "_EK9 Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
        assertInError(underTest);
    }
    @Test
    public void testSemanticErrorCreation()
    {
        ErrorListener underTest = new ErrorListener();
        underTest.semanticError(createSyntheticToken(), "_EK9 Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
        assertInError(underTest);
    }

    @Test
    public void testSemanticErrorCreationNoToken()
    {
        ErrorListener underTest = new ErrorListener();
        underTest.semanticError(null, "Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
        assertInError(underTest);
    }

    @Test
    public void testSemanticErrorFuzzyResults()
    {
        ErrorListener underTest = new ErrorListener();
        MatchResults results = new MatchResults() {
            @Override
            public String toString() { return "Simulated fuzzy match details"; }
        };
        underTest.semanticError(createSyntheticToken(), "Fuzzy", ErrorListener.SemanticClassification.CONSTRUCTOR_NOT_RESOLVED, results);
        assertInError(underTest);
    }

    private void assertInError(ErrorListener underTest)
    {
        TestCase.assertFalse(underTest.isErrorFree());
        ErrorListener.ErrorDetails details = underTest.getErrors().next();
        TestCase.assertNotNull(details);
        TestCase.assertNotNull(details.toString());
    }
    @Test
    public void testConstructionAndSetup()
    {
        ErrorListener underTest = new ErrorListener();

        //Setup check defaults and ensure switches work.
        TestCase.assertTrue(underTest.isErrorFree());
        TestCase.assertTrue(underTest.isWarningFree());

        //These are the three main flags that indicate there might be an issue with the grammar.
        TestCase.assertFalse(underTest.isExceptionOnAmbiguity());
        underTest.setExceptionOnAmbiguity(true);
        TestCase.assertTrue(underTest.isExceptionOnAmbiguity());

        TestCase.assertFalse(underTest.isExceptionOnContextSensitive());
        underTest.setExceptionOnContextSensitive(true);
        TestCase.assertTrue(underTest.isExceptionOnContextSensitive());

        TestCase.assertFalse(underTest.isExceptionOnFullContext());
        underTest.setExceptionOnFullContext(true);
        TestCase.assertTrue(underTest.isExceptionOnFullContext());

        Token token= createSyntheticToken();
        underTest.raiseReturningRedundant(token, "Test Message");

        //Should now be in error
        TestCase.assertFalse(underTest.isErrorFree());
        ErrorListener.ErrorDetails details = underTest.getErrors().next();
        TestCase.assertNotNull(details);
        TestCase.assertNotNull(details.getClassification());

        TestCase.assertNotNull(details.toString());
        TestCase.assertEquals(ErrorListener.SemanticClassification.RETURNING_REDUNDANT, details.getSemanticClassification());

        TestCase.assertTrue(underTest.isWarningFree());

        //Now create a warning
        underTest.semanticWarning(createSyntheticToken(), "Test Warning Message", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
        TestCase.assertFalse(underTest.isWarningFree());

        details = underTest.getWarnings().next();
        TestCase.assertNotNull(details.toString());
        TestCase.assertEquals(ErrorListener.SemanticClassification.METHOD_AMBIGUOUS, details.getSemanticClassification());

    }

    private Token createSyntheticToken()
    {
        return new Token() {
            @Override
            public String getText()
            {
                return "Synthetic Token";
            }

            @Override
            public int getType()
            {
                return 0;
            }

            @Override
            public int getLine()
            {
                return 0;
            }

            @Override
            public int getCharPositionInLine()
            {
                return 0;
            }

            @Override
            public int getChannel()
            {
                return 0;
            }

            @Override
            public int getTokenIndex()
            {
                return 0;
            }

            @Override
            public int getStartIndex()
            {
                return 0;
            }

            @Override
            public int getStopIndex()
            {
                return 0;
            }

            @Override
            public TokenSource getTokenSource()
            {
                return new TokenSource() {

                    @Override
                    public Token nextToken()
                    {
                        return null;
                    }

                    @Override
                    public int getLine()
                    {
                        return 0;
                    }

                    @Override
                    public int getCharPositionInLine()
                    {
                        return 0;
                    }

                    @Override
                    public CharStream getInputStream()
                    {
                        return null;
                    }

                    @Override
                    public String getSourceName()
                    {
                        return "Synthetic Token Source";
                    }

                    @Override
                    public void setTokenFactory(TokenFactory<?> factory)
                    {

                    }

                    @Override
                    public TokenFactory<?> getTokenFactory()
                    {
                        return null;
                    }
                };
            }

            @Override
            public CharStream getInputStream()
            {
                return null;
            }
        };
    }
}
