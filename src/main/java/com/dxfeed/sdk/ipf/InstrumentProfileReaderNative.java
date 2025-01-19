package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.ipf.InstrumentProfileReader;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.javac.DxfgAuthToken;
import com.dxfeed.sdk.javac.DxfgInputStream;
import java.io.IOException;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.word.WordFactory;

@CContext(Directives.class)
public class InstrumentProfileReaderNative {

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileReader dxfg_InstrumentProfileReader_new(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toNative(new InstrumentProfileReader());
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_getLastModified",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_InstrumentProfileReader_getLastModified(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .getLastModified();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_wasComplete",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_wasComplete(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .wasComplete()
        ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileList dxfg_InstrumentProfileReader_readFromFile(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final CCharPointer address
  ) throws IOException {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toNativeList(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
            .readFromFile(NativeUtils.MAPPER_STRING.toJava(address))
    );
  }

  @SuppressWarnings("SameReturnValue")
  private static int fillNativeProfiles(List<InstrumentProfile> javaInstrumentProfiles,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer nativeInstrumentProfiles,
      @DxfgOut final CInt32Pointer size, boolean cached) {
    if (javaInstrumentProfiles.isEmpty()) {
      nativeInstrumentProfiles.write(WordFactory.nullPointer());
      size.write(0);

      return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    DxfgInstrumentProfile2Pointer targetInstrumentProfiles = UnmanagedMemory.calloc(
        javaInstrumentProfiles.size() * SizeOf.get(DxfgInstrumentProfile2Pointer.class));

    for (int i = 0; i < javaInstrumentProfiles.size(); i++) {
      if (cached) {
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_2_CACHED.fillNative(javaInstrumentProfiles.get(i),
            targetInstrumentProfiles.addressOf(i), false);
      } else {
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_2.fillNative(javaInstrumentProfiles.get(i),
            targetInstrumentProfiles.addressOf(i), false);
      }
    }

    nativeInstrumentProfiles.write(targetInstrumentProfiles);
    size.write(javaInstrumentProfiles.size());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readFromFile_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      @CConst final CCharPointer address,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readFromFile(NativeUtils.MAPPER_STRING.toJava(address));

    return fillNativeProfiles(profiles, instrumentProfiles, size, false);
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readFromFile_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      @CConst final CCharPointer address,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readFromFile(NativeUtils.MAPPER_STRING.toJava(address));

    return fillNativeProfiles(profiles, instrumentProfiles, size, true);
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileList dxfg_InstrumentProfileReader_readFromFile2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final CCharPointer address,
      final CCharPointer user,
      final CCharPointer password
  ) throws IOException {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toNativeList(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
            .readFromFile(
                NativeUtils.MAPPER_STRING.toJava(address),
                NativeUtils.MAPPER_STRING.toJava(user),
                NativeUtils.MAPPER_STRING.toJava(password)
            )
    );
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile2_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readFromFile2_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      @CConst final CCharPointer address,
      @CConst final CCharPointer user,
      @CConst final CCharPointer password,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readFromFile(NativeUtils.MAPPER_STRING.toJava(address),
            NativeUtils.MAPPER_STRING.toJava(user), NativeUtils.MAPPER_STRING.toJava(password));

    return fillNativeProfiles(profiles, instrumentProfiles, size, false);
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile2_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readFromFile2_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      @CConst final CCharPointer address,
      @CConst final CCharPointer user,
      @CConst final CCharPointer password,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readFromFile(NativeUtils.MAPPER_STRING.toJava(address),
            NativeUtils.MAPPER_STRING.toJava(user), NativeUtils.MAPPER_STRING.toJava(password));

    return fillNativeProfiles(profiles, instrumentProfiles, size, true);
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile3",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileList dxfg_InstrumentProfileReader_readFromFile3(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final CCharPointer address,
      final DxfgAuthToken token
  ) throws IOException {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toNativeList(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
            .readFromFile(
                NativeUtils.MAPPER_STRING.toJava(address),
                NativeUtils.MAPPER_AUTH_TOKEN.toJava(token)
            )
    );
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile3_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readFromFile3_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      @CConst final CCharPointer address,
      final DxfgAuthToken token,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readFromFile(NativeUtils.MAPPER_STRING.toJava(address),
            NativeUtils.MAPPER_AUTH_TOKEN.toJava(token));

    return fillNativeProfiles(profiles, instrumentProfiles, size, false);
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readFromFile3_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readFromFile3_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      @CConst final CCharPointer address,
      final DxfgAuthToken token,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readFromFile(NativeUtils.MAPPER_STRING.toJava(address),
            NativeUtils.MAPPER_AUTH_TOKEN.toJava(token));

    return fillNativeProfiles(profiles, instrumentProfiles, size, true);
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_resolveSourceURL",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfileReader_resolveSourceURL(
      final IsolateThread ignoredThread,
      final CCharPointer address
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        InstrumentProfileReader.resolveSourceURL(
            NativeUtils.MAPPER_STRING.toJava(address)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_read2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileList dxfg_InstrumentProfileReader_read2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream,
      final CCharPointer address
  ) throws IOException {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toNativeList(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).read(
            NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream),
            NativeUtils.MAPPER_STRING.toJava(address)
        )
    );
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_read2_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_read2_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream,
      @CConst final CCharPointer address,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .read(NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream),
            NativeUtils.MAPPER_STRING.toJava(address));

    return fillNativeProfiles(profiles, instrumentProfiles, size, false);
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_read2_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_read2_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream,
      @CConst final CCharPointer address,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .read(NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream),
            NativeUtils.MAPPER_STRING.toJava(address));

    return fillNativeProfiles(profiles, instrumentProfiles, size, true);
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readCompressed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileList dxfg_InstrumentProfileReader_readCompressed(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream
  ) throws IOException {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toNativeList(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
            .readCompressed(
                NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream)
            )
    );
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readCompressed_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readCompressed_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readCompressed(NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream));

    return fillNativeProfiles(profiles, instrumentProfiles, size, false);
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_readCompressed_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_readCompressed_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .readCompressed(NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream));

    return fillNativeProfiles(profiles, instrumentProfiles, size, true);
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_read",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileList dxfg_InstrumentProfileReader_read(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream
  ) throws IOException {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toNativeList(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).read(
            NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream)
        )
    );
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_read_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_read_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .read(NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream));

    return fillNativeProfiles(profiles, instrumentProfiles, size, false);
  }

  @SuppressWarnings("SameReturnValue")
  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_read_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_read_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
      final DxfgInputStream dxfgInputStream,
      @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfiles,
      @DxfgOut final CInt32Pointer size
  ) throws IOException {
    if (instrumentProfiles.isNull()) {
      throw new IllegalArgumentException("The `instrumentProfiles` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var profiles = NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
        .read(NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream));

    return fillNativeProfiles(profiles, instrumentProfiles, size, true);
  }
}
