package org.ek9lang.compiler;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.core.AssertValue;

/**
 * Represents the Intermediate Representation of the EK9 code for a particular EK9 module.
 * <p>
 * The idea is to pull out all the EK9 specifics and move more towards a general target
 * CPU based implementation. By this I mean take out things like 'guards' and
 * </p>
 */
public final class IRModule implements Module {
  @Serial
  private static final long serialVersionUID = 1L;
  private final CompilableSource source;
  private final List<IRConstruct> constructs = new ArrayList<>();

  private String moduleName;
  private boolean extern;

  public IRModule(final CompilableSource source) {

    AssertValue.checkNotNull("CompilableSource cannot be null", source);
    this.source = source;

  }

  @Override
  public CompilableSource getSource() {
    return source;
  }

  @Override
  public String getScopeName() {
    return moduleName;
  }


  @Override
  public boolean isEk9Core() {

    //Any module that start with this is deemed core.
    return moduleName.startsWith("org.ek9");
  }

  public List<IRConstruct> getConstructs() {
    return List.copyOf(constructs);
  }

  public void acceptCompilationUnitContext(final EK9Parser.CompilationUnitContext compilationUnitContext) {

    AssertValue.checkNotNull("CompilationUnitContext cannot be null", compilationUnitContext);
    this.moduleName = compilationUnitContext.moduleDeclaration().dottedName().getText();
    //Need to know if this module is extern, so that compiler can process but not create final output for it.
    this.extern = compilationUnitContext.moduleDeclaration().EXTERN() != null;

    AssertValue.checkNotEmpty("ModuleName must have a value", moduleName);
  }

  public boolean isExtern() {
    return extern;
  }

  /**
   * Adds a new construct operation node (i.e. some form of aggregate/function - i.e. a Construct)
   *
   * @param node The node to be added.
   */
  public void add(final IRConstruct node) {
    AssertValue.checkNotNull("Construct cannot be null", node);

    //Because we may multi-thread the IR generation, just need to ensure the list we add to is thread safe for addition.
    synchronized (constructs) {
      constructs.add(node);
    }
  }
}
