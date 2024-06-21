package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.InstrumentProfileReader;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.javac.DxfgAuthToken;
import com.dxfeed.sdk.javac.DxfgInputStream;
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
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).getLastModified();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileReader_wasComplete",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileReader_wasComplete(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileReader dxfgInstrumentProfileReader
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).wasComplete()
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
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).readFromFile(
            NativeUtils.MAPPER_STRING.toJava(address),
            NativeUtils.MAPPER_STRING.toJava(user),
            NativeUtils.MAPPER_STRING.toJava(password)
        )
    );
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
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).readFromFile(
            NativeUtils.MAPPER_STRING.toJava(address),
            NativeUtils.MAPPER_AUTH_TOKEN.toJava(token)
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
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).readCompressed(
            NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream)
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
    return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toNativeList(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_READER.toJava(dxfgInstrumentProfileReader).read(
            NativeUtils.MAPPER_INPUT_STREAM.toJava(dxfgInputStream)
        )
    );
  }
}
