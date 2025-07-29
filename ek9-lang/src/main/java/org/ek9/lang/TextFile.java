package org.ek9.lang;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Represents a file that can hold some form of text. i.e. not binary data.
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MethodName"})
@Ek9Class("""
    TextFile with trait of File""")
public class TextFile extends BuiltinType implements File {

  java.io.File state = null;

  @Ek9Constructor("""
      TextFile() as pure""")
  public TextFile() {
    unSet();
  }

  @Ek9Constructor("""
      TextFile() as pure
        -> fileName as String""")
  public TextFile(String fileName) {
    this();
    if (isValid(fileName)) {
      //Then lets check it is viable and use it.
      final var fileSystemPath = new FileSystemPath(fileName);
      if (fileSystemPath.exists().state && fileSystemPath.isFile().state) {
        state = fileSystemPath.state.toFile();
        set();
      }
    }
  }

  @Ek9Constructor("""
      TextFile() as pure
        -> textFile as TextFile""")
  public TextFile(TextFile textFile) {
    this();
    if (isValid(textFile)) {
      //Then lets check it is viable and use it.
      final var fileSystemPath = FileSystemPath._of(textFile.state.toPath());
      if (fileSystemPath.exists().state && fileSystemPath.isFile().state) {
        state = fileSystemPath.state.toFile();
        set();
      }
    }
  }

  @Ek9Constructor("""
      TextFile() as pure
        -> fileSystemPath as FileSystemPath""")
  public TextFile(FileSystemPath fileSystemPath) {
    this();
    if (isValid(fileSystemPath) && fileSystemPath.exists().state) {
      state = fileSystemPath.state.toFile();
      set();
    }
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      input() as pure
        <- rtn as StringInput?""")
  public StringInput input() {

    if (isSet && state.canRead()) {
      try {
        return new StringInputImpl(new FileInputStream(state));
      } catch (IOException _) {
        //Just let flow return empty StringInput.
      }
    }

    return new StringInput() {
      @Override
      public Boolean _isSet() {
        return Boolean._of(false);
      }
    };
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      output() as pure
        <- rtn as StringOutput?""")
  public StringOutput output() {

    if (isSet && state.canWrite()) {
      try {
        return new StringOutputImpl(new PrintStream(new FileOutputStream(state)));
      } catch (IOException _) {
        //Just let flow return empty StringOutput.
      }
    }

    return new StringOutput() {
      @Override
      public Boolean _isSet() {
        return Boolean._of(false);
      }
    };
  }

  @Override
  @Ek9Method("""
      override isWritable() as pure
        <- rtn as Boolean?""")
  public Boolean isWritable() {
    if (isSet) {
      return Boolean._of(state.canWrite());
    }
    return new Boolean();
  }

  @Override
  @Ek9Method("""
      override isReadable() as pure
        <- rtn as Boolean?""")
  public Boolean isReadable() {
    if (isSet) {
      return Boolean._of(state.canRead());
    }
    return new Boolean();
  }

  @Override
  @Ek9Method("""
      override isExecutable() as pure
        <- rtn as Boolean?""")
  public Boolean isExecutable() {
    if (isSet) {
      return Boolean._of(state.canExecute());
    }
    return new Boolean();
  }

  @Override
  @Ek9Method("""
      override lastModified() as pure
        <- rtn as DateTime?""")
  public DateTime lastModified() {
    if (isSet) {
      //Note that file last modified always reports seconds from GMT epoch.
      Instant i = Instant.ofEpochSecond(state.lastModified());
      ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(i, ZoneId.of("GMT"));
      return DateTime._of(zonedDateTime);
    }
    return new DateTime();
  }

  @Override
  @Ek9Operator("""
      override operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    if (isSet) {
      return Integer._of(state.length());
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      override operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(state.getAbsolutePath());
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      override operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (isSet) {
      return Integer._of(state.hashCode());
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  public static TextFile _of(java.lang.String path) {
    if (path != null) {
      return new TextFile(String._of(path));
    }
    return new TextFile();
  }

  public static TextFile _of(java.nio.file.Path path) {
    if (path != null) {
      return new TextFile(FileSystemPath._of(path));
    }
    return new TextFile();
  }

}
