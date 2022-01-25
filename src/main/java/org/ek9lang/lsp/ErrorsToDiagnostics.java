package org.ek9lang.lsp;

import org.eclipse.lsp4j.*;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.files.CompilableSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Convert ErrorListener warnings and errors to LSP Diagnostics.
 * 
 */
public class ErrorsToDiagnostics
{

	public PublishDiagnosticsParams getEmptyDiagnostics(CompilableSource compilableSource)
	{
		PublishDiagnosticsParams rtn = new PublishDiagnosticsParams();
		rtn.setUri(compilableSource.getGeneralIdentifier());
		rtn.setDiagnostics(new ArrayList<Diagnostic>(0));
         
         return rtn;
	}
	
	public PublishDiagnosticsParams getErrorDiagnostics(CompilableSource compilableSource)
	{
		PublishDiagnosticsParams rtn = new PublishDiagnosticsParams();
		rtn.setUri(compilableSource.getGeneralIdentifier());
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(0);
		
		ErrorListener errorListener = compilableSource.getErrorListener();
		if(!errorListener.isWarningFree())
			diagnostics.addAll(extractDiagnostics(errorListener.getWarnings(), DiagnosticSeverity.Warning));
		
		if(!errorListener.isErrorFree())
			diagnostics.addAll(extractDiagnostics(errorListener.getErrors(), DiagnosticSeverity.Error));
		
		rtn.setDiagnostics(diagnostics);
		 		
		System.err.println("Our URI [" + compilableSource.getGeneralIdentifier() + "] " + diagnostics.size() + " diagnostics");
		return rtn;
	}
	
	private List<Diagnostic> extractDiagnostics(Iterator<ErrorListener.ErrorDetails> iter, DiagnosticSeverity severity)
	{
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(0);
		
		//We only output the first syntax error per file because the others will be a cascade.
		boolean haveFirstSyntaxError = false;
		
		while(iter.hasNext())
		{
			ErrorListener.ErrorDetails details = iter.next();
			boolean syntaxError = details.getClassification().equals(ErrorListener.ErrorClassification.SYNTAX_ERROR);
			if(!syntaxError || !haveFirstSyntaxError)
				diagnostics.add(extractDiagnotic(severity, details));
			haveFirstSyntaxError |= syntaxError;
		}
		
		return diagnostics;
	}
	
	private Diagnostic extractDiagnotic(DiagnosticSeverity severity, ErrorListener.ErrorDetails details)
	{
		Diagnostic d = new Diagnostic();
		
		d.setSeverity(severity);
		d.setMessage(details.getTypeOfError());
		
		//This should line up with the source in the LSP.
		int lineNo = details.getLineNumber() - 1;
		int charPos = details.getPosition();
		
		Range r = new Range();
		//might see if we can improve this with length of token.
		r.setStart(new Position(lineNo, charPos));
		r.setEnd(new Position(lineNo, charPos+details.getTokenLength()));
		d.setRange(r);
		return d;
	}
}
