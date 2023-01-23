package com.dxfeed.api.ipf;

import static com.dxfeed.api.NativeUtils.MAPPER_INPUT_STREAM;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILES;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;

import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.javac.DxfgInputStream;
import com.dxfeed.ipf.InstrumentProfileReader;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class InstrumentProfileReaderNative {

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileReader dxfg_InstrumentProfileReader_new(
      final IsolateThread ignoredThread
  ) {
    return MAPPER_INSTRUMENT_PROFILE_READER.toNative(new InstrumentProfileReader());
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_getLastModified",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_InstrumentProfileReader_getLastModified(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader
  ) {
    return MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).getLastModified();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_wasComplete",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_wasComplete(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader
  ) {
    return MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).wasComplete()
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
    return MAPPER_INSTRUMENT_PROFILES.toNativeList(
        MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader)
            .readFromFile(MAPPER_STRING.toJava(address))
    );
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
    return MAPPER_INSTRUMENT_PROFILES.toNativeList(
        MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).readFromFile(
            MAPPER_STRING.toJava(address),
            MAPPER_STRING.toJava(user),
            MAPPER_STRING.toJava(password)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_resolveSourceURL",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfileReader_resolveSourceURL(
      final IsolateThread ignoredThread,
      final CCharPointer address
  ) {
    return MAPPER_STRING.toNative(
        InstrumentProfileReader.resolveSourceURL(
            MAPPER_STRING.toJava(address)
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
    return MAPPER_INSTRUMENT_PROFILES.toNativeList(
        MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).read(
            MAPPER_INPUT_STREAM.toJava(dxfgInputStream),
            MAPPER_STRING.toJava(address)
        )
    );
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
    return MAPPER_INSTRUMENT_PROFILES.toNativeList(
        MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).readCompressed(
            MAPPER_INPUT_STREAM.toJava(dxfgInputStream)
        )
    );
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
    return MAPPER_INSTRUMENT_PROFILES.toNativeList(
        MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).read(
            MAPPER_INPUT_STREAM.toJava(dxfgInputStream)
        )
    );
  }
}
