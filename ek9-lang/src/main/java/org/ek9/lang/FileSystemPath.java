package org.ek9.lang;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;

/**
 * Gives EK9 access to the local file system.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    FileSystemPath""")
public class FileSystemPath extends BuiltinType {

  //This is the actual Path implementation.
  java.nio.file.Path state = null;

  @Ek9Constructor("""
      FileSystemPath() as pure""")
  public FileSystemPath() {
    unSet();
  }

  @Ek9Constructor("""
      FileSystemPath() as pure
        -> pathName as String""")
  public FileSystemPath(String pathName) {
    this();
    if (isValid(pathName)) {
      state = Paths.get(pathName.state);
      set();
    }
  }

  //Not exposed to Ek9.
  private FileSystemPath(java.nio.file.Path path) {
    this();
    if (path != null) {
      this.state = path;
      set();
    }
  }

  public FileSystemPath withCurrentWorkingDirectory() {
    return FileSystemPath._of(FileSystems.getDefault().getPath(".").normalize());
  }

  public FileSystemPath withTemporaryDirectory() {
    return FileSystemPath._of(Paths.get(System.getProperty("java.io.tmpdir")));
  }

  public Boolean startsWith(FileSystemPath path) {
    if (canProcess(path)) {
      return Boolean._of(state.startsWith(path.state));
    }
    return new Boolean();
  }

  public Boolean endsWith(String relativePath) {
    if (canProcess(relativePath)) {
      return Boolean._of(state.endsWith(relativePath.state));
    }
    return new Boolean();
  }

  public Boolean endsWith(FileSystemPath relativePath) {
    if (canProcess(relativePath)) {
      return Boolean._of(state.endsWith(relativePath.state));
    }
    return new Boolean();
  }

  public Boolean exists() {
    if (isSet) {
      return Boolean._of(pathExists());
    }
    return new Boolean();
  }

  public Boolean createFile() {
    return createFile(Boolean._of(false));
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  public Boolean createFile(Boolean createDirectoriesIfRequired) {
    if (canProcess(createDirectoriesIfRequired) && state.isAbsolute()) {
      try {
        if (createDirectoriesIfRequired.state && !state.getParent().toFile().mkdirs()) {
          return Boolean._of(false);
        }
        return Boolean._of(state.toFile().createNewFile());
      } catch (SecurityException | IOException _) {
        return Boolean._of(false);
      }
    }
    return new Boolean();
  }

  public Boolean createDirectory() {
    return createDirectory(Boolean._of(false));
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  public Boolean createDirectory(Boolean createDirectoriesIfRequired) {
    if (canProcess(createDirectoriesIfRequired) && state.isAbsolute()) {
      try {
        if (createDirectoriesIfRequired.state && !state.getParent().toFile().mkdirs()) {
          return Boolean._of(false);
        }
        return Boolean._of(state.toFile().mkdir());
      } catch (SecurityException _) {
        return Boolean._of(false);
      }
    }
    return new Boolean();
  }

  public Boolean isFile() {
    if (pathExists()) {
      return Boolean._of(state.toFile().isFile());
    }
    return new Boolean();
  }

  public Boolean isDirectory() {

    if (pathExists()) {
      return Boolean._of(state.toFile().isDirectory());
    }
    return new Boolean();
  }

  public Boolean isWritable() {
    if (pathExists()) {
      return Boolean._of(state.toFile().canWrite());
    }

    return new Boolean();
  }

  public Boolean isReadable() {
    if (pathExists()) {
      return Boolean._of(state.toFile().canRead());
    }
    return new Boolean();
  }

  public Boolean isExecutable() {
    if (pathExists()) {
      return Boolean._of(state.toFile().canExecute());
    }

    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as FileSystemPath
        <- rtn as Boolean?""")
  public Boolean _eq(FileSystemPath arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as FileSystemPath
        <- rtn as Boolean?""")
  public Boolean _neq(FileSystemPath arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as FileSystemPath
        <- rtn as Integer?""")
  public Integer _cmp(FileSystemPath arg) {
    if (canProcess(arg)) {
      return Integer._of(compare(arg.state));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof FileSystemPath asFileSystemPath) {
      return _cmp(asFileSystemPath);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as FileSystemPath
        <- rtn as Boolean?""")
  public Boolean _lt(FileSystemPath arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg.state) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as FileSystemPath
        <- rtn as Boolean?""")
  public Boolean _lteq(FileSystemPath arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg.state) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as FileSystemPath
        <- rtn as Boolean?""")
  public Boolean _gt(FileSystemPath arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg.state) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as FileSystemPath
        <- rtn as Boolean?""")
  public Boolean _gteq(FileSystemPath arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg.state) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as FileSystemPath
        <- rtn as Integer?""")
  public Integer _fuzzy(FileSystemPath arg) {
    if (canProcess(arg)) {
      return new Integer();
    }

    Levenshtein fuzzy = new Levenshtein();
    return Integer._of(fuzzy.costOfMatch(this.state.toString(), arg.state.toString()));
  }

  public Boolean isAbsolute() {
    if (isSet) {
      return Boolean._of(state.isAbsolute());
    }
    return new Boolean();
  }

  public FileSystemPath absolutePath() {
    if (isSet) {
      return new FileSystemPath(state.toAbsolutePath());
    }
    return this;
  }

  //TODO add in textFile() maybe

  @Ek9Operator("""
      operator +=
        -> arg as FileSystemPath""")
  public void _addAss(FileSystemPath arg) {
    if (canProcess(arg)) {
      this.state = state.resolve(arg.state).normalize();
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as String""")
  public void _addAss(String arg) {
    if (canProcess(arg)) {
      this.state = _add(arg).state;
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as FileSystemPath
        <- rtn as FileSystemPath?""")
  public FileSystemPath _add(FileSystemPath arg) {
    if (canProcess(arg)) {
      return new FileSystemPath(state.resolve(arg.state).normalize());
    }
    return _new();
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as String
        <- rtn as FileSystemPath?""")
  public FileSystemPath _add(String arg) {
    if (canProcess(arg)) {
      java.lang.String[] split = split(arg.state);
      Path p = state;
      for (java.lang.String val : split) {
        if (!val.isBlank()) {
          p = p.resolve(val);
        }
      }
      return new FileSystemPath(p.normalize());
    }
    return _new();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(state.toString());
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (isSet) {
      return Integer._of(state.hashCode());
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  //Factory and Utility methods


  @Override
  protected FileSystemPath _new() {
    return new FileSystemPath();
  }

  private boolean pathExists() {
    return isSet && state.isAbsolute() && state.toFile().exists();
  }

  private java.lang.String[] split(java.lang.String path) {
    //Need to split it either way it is defined. Unix or Windows.
    java.lang.String r1 = path.replace("/", "§");
    java.lang.String r2 = r1.replace("\\\\", "§");
    return r2.split("§");
  }

  private int compare(Path to) {
    return this.state.compareTo(to);
  }

  public static FileSystemPath _of(java.nio.file.Path path) {
    return new FileSystemPath(path);
  }

}
