package org.ek9lang.compiler.backend.jvm;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V23;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Designed to capture the ASM specifics for byte code generation.
 */
final class AsmStructureCreator {

  private static final String JAVA_STRING_ARRAY_DESCRIPTOR = "[Ljava/lang/String;";
  private static final String FULLY_QUALIFIED_OBJECT = "java/lang/Object";
  private static final String INIT = "<init>";
  private static final String THIS = "this";
  private static final String EK9_MAIN_ENTRY = "_main";

  private final INodeVisitor visitor;
  private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
  private final CheckClassAdapter classVisitor = new CheckClassAdapter(classWriter);
  private final FullyQualifiedJvmName fullyQualifiedJvmName = new FullyQualifiedJvmName();

  private final String fullyQualifiedClassName;
  private final String classNameDescriptor;
  private final ConstructTargetTuple constructTargetTuple;

  AsmStructureCreator(final ConstructTargetTuple constructTargetTuple, final INodeVisitor visitor) {

    this.visitor = visitor;
    this.constructTargetTuple = constructTargetTuple;
    this.fullyQualifiedClassName =
        fullyQualifiedJvmName.apply(constructTargetTuple.construct().getFullyQualifiedName());
    this.classNameDescriptor = "L" + fullyQualifiedClassName + ";";

  }

  void processClass() {

    if (constructTargetTuple.construct().isProgram()) {
      processProgram();
      return;
    }
    throw new CompilerException("Constructs other than program not yet supported");
  }

  private void processProgram() {

    AssertValue.checkTrue("Their should be only one method on a program",
        constructTargetTuple.construct().getOperations().size() == 1);

    //First create a class structure
    classVisitor.visit(V23, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, FULLY_QUALIFIED_OBJECT, null);
    classVisitor.visitSource(constructTargetTuple.relativeFileName(), null);

    processConstructor();
    processMainEntry();

    //Now process the single method.
    constructTargetTuple.construct()
        .getOperations()
        .stream()
        .findFirst()
        .ifPresent(operation -> operation.accept(visitor));

    classVisitor.visitEnd();
  }

  /**
   * There must always be a constructor in Java even if one is not explicit in the Java source.
   */
  private void processConstructor() {
    MethodVisitor methodVisitor = classVisitor.visitMethod(ACC_PUBLIC, INIT, "()V", null, null);
    methodVisitor.visitCode();
    Label label0 = new Label();
    methodVisitor.visitLabel(label0);
    methodVisitor.visitLineNumber(5, label0);
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, FULLY_QUALIFIED_OBJECT, INIT, "()V", false);
    methodVisitor.visitInsn(RETURN);
    Label label1 = new Label();
    methodVisitor.visitLabel(label1);
    methodVisitor.visitLocalVariable(THIS, classNameDescriptor, null, label0, label1, 0);
    //Let ASM work this out for now.
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }

  /**
   * This is the main for jvm entry, in time this will handle argument and convert them to
   * argument required by the EK9 _main entry point.
   */
  private void processMainEntry() {
    final var main = "main";
    MethodVisitor methodVisitor =
        classVisitor.visitMethod(ACC_PUBLIC | ACC_STATIC, main, "(" + JAVA_STRING_ARRAY_DESCRIPTOR + ")V", null, null);
    methodVisitor.visitCode();
    Label label0 = new Label();
    methodVisitor.visitLabel(label0);
    methodVisitor.visitLineNumber(13, label0);
    methodVisitor.visitTypeInsn(NEW, fullyQualifiedClassName);
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, fullyQualifiedClassName, INIT, "()V", false);
    methodVisitor.visitVarInsn(ASTORE, 1);
    Label label1 = new Label();
    methodVisitor.visitLabel(label1);
    methodVisitor.visitLineNumber(14, label1);
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, fullyQualifiedClassName, EK9_MAIN_ENTRY, "()V", false);
    Label label2 = new Label();
    methodVisitor.visitLabel(label2);
    methodVisitor.visitLineNumber(16, label2);
    methodVisitor.visitInsn(RETURN);
    Label label3 = new Label();
    methodVisitor.visitLabel(label3);
    methodVisitor.visitLocalVariable("args", JAVA_STRING_ARRAY_DESCRIPTOR, null, label0, label3, 0);
    methodVisitor.visitLocalVariable(main, classNameDescriptor, null, label1, label3, 1);
    //Let ASM work this out for now.
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }

  /**
   * For now just hard code this in, but need to actually call visit Operation.
   * And actually process the correct Structures!
   */
  void processMethodOnClass(final Operation operation) {

    final var stdout = "org/ek9/lang/Stdout";
    final var methodName = operation.getOperationName();
    MethodVisitor methodVisitor = classVisitor.visitMethod(ACC_PRIVATE, methodName, "()V", null, null);
    methodVisitor.visitCode();
    Label label0 = new Label();
    methodVisitor.visitLabel(label0);
    methodVisitor.visitLineNumber(19, label0);
    methodVisitor.visitTypeInsn(NEW, stdout);
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, stdout, INIT, "()V", false);
    methodVisitor.visitVarInsn(ASTORE, 1);
    Label label1 = new Label();
    methodVisitor.visitLabel(label1);
    methodVisitor.visitLineNumber(20, label1);
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitLdcInsn("Hello, World Steve 15!");
    methodVisitor.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/String", "of",
        "(Ljava/lang/String;)Lorg/ek9/lang/String;", false);
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, stdout, "println", "(Lorg/ek9/lang/String;)V", false);
    Label label2 = new Label();
    methodVisitor.visitLabel(label2);
    methodVisitor.visitLineNumber(21, label2);
    methodVisitor.visitInsn(RETURN);
    Label label3 = new Label();
    methodVisitor.visitLabel(label3);

    methodVisitor.visitLocalVariable(THIS, classNameDescriptor, null, label0, label3, 0);
    methodVisitor.visitLocalVariable("stdout", "Lorg/ek9/lang/Stdout;", null, label1, label3, 1);
    //Let ASM work this out for now.
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }

  byte[] getByteCode() {
    return classWriter.toByteArray();
  }
}
