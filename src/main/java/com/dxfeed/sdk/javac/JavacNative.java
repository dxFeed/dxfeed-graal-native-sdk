package com.dxfeed.sdk.javac;

import com.devexperts.auth.AuthToken;
import com.devexperts.util.TimeFormat;
import com.devexperts.util.TimePeriod;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusMinInteger;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

@CContext(Directives.class)
public class JavacNative {

  private static final int DEEP_RECURSION_TO_THROW_EXCEPTION = 5;

  @CEntryPoint(
      name = "dxfg_TimeZone_getTimeZone",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeZone dxfg_TimeZone_getTimeZone(
      final IsolateThread ignoredThread,
      final CCharPointer ID
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toNative(
        TimeZone.getTimeZone(
            NativeUtils.MAPPER_STRING.toJava(ID)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getDefault",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeZone dxfg_TimeZone_getDefault(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toNative(
        TimeZone.getDefault()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getID",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_TimeZone_getID(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).getID()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getDisplayName",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_TimeZone_getDisplayName(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).getDisplayName()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getDisplayName2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_TimeZone_getDisplayName2(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone,
      final int daylight,
      final int style
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).getDisplayName(daylight == 1, style)
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getDSTSavings",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_getDSTSavings(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).getDSTSavings();
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_useDaylightTime",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_useDaylightTime(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).useDaylightTime() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_observesDaylightTime",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_observesDaylightTime(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).observesDaylightTime() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getOffset",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_getOffset(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone,
      final long date
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).getOffset(date);
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getOffset2",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_getOffset2(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone,
      final int era,
      final int year,
      final int month,
      final int day,
      final int dayOfWeek,
      final int milliseconds
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone)
        .getOffset(era, year, month, day, dayOfWeek, milliseconds);
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_getRawOffset",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_getRawOffset(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).getRawOffset();
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_hasSameRules",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_hasSameRules(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone,
      final DxfgTimeZone other
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone)
        .hasSameRules(NativeUtils.MAPPER_TIME_ZONE.toJava(other)) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_inDaylightTime",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_inDaylightTime(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone,
      final long date
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).inDaylightTime(new Date(date)) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_setID",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_setID(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone,
      final CCharPointer ID
  ) {
    NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).setID(NativeUtils.MAPPER_STRING.toJava(ID));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeZone_setRawOffset",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimeZone_setRawOffset(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone,
      final int offsetMillis
  ) {
    NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone).setRawOffset(offsetMillis);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimePeriod_ZERO",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimePeriod dxfg_TimePeriod_ZERO(final IsolateThread ignoredThread) {
    return NativeUtils.MAPPER_TIME_PERIOD.toNative(TimePeriod.ZERO);
  }

  @CEntryPoint(
      name = "dxfg_TimePeriod_UNLIMITED",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimePeriod dxfg_TimePeriod_UNLIMITED(final IsolateThread ignoredThread) {
    return NativeUtils.MAPPER_TIME_PERIOD.toNative(TimePeriod.UNLIMITED);
  }

  @CEntryPoint(
      name = "dxfg_TimePeriod_valueOf",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimePeriod dxfg_TimePeriod_valueOf(
      final IsolateThread ignoredThread,
      final long value
  ) {
    return NativeUtils.MAPPER_TIME_PERIOD.toNative(TimePeriod.valueOf(value));
  }

  @CEntryPoint(
      name = "dxfg_TimePeriod_valueOf2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimePeriod dxfg_TimePeriod_valueOf2(
      final IsolateThread ignoredThread,
      final CCharPointer value
  ) {
    return NativeUtils.MAPPER_TIME_PERIOD.toNative(
        TimePeriod.valueOf(NativeUtils.MAPPER_STRING.toJava(value))
    );
  }

  @CEntryPoint(
      name = "dxfg_TimePeriod_getTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_TimePeriod_getTime(
      final IsolateThread ignoredThread,
      final DxfgTimePeriod dxfgTimePeriod
  ) {
    return NativeUtils.MAPPER_TIME_PERIOD.toJava(dxfgTimePeriod).getTime();
  }

  @CEntryPoint(
      name = "dxfg_TimePeriod_getSeconds",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_TimePeriod_getSeconds(
      final IsolateThread ignoredThread,
      final DxfgTimePeriod dxfgTimePeriod
  ) {
    return NativeUtils.MAPPER_TIME_PERIOD.toJava(dxfgTimePeriod).getSeconds();
  }

  @CEntryPoint(
      name = "dxfg_TimePeriod_getNanos",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_TimePeriod_getNanos(
      final IsolateThread ignoredThread,
      final DxfgTimePeriod dxfgTimePeriod
  ) {
    return NativeUtils.MAPPER_TIME_PERIOD.toJava(dxfgTimePeriod).getNanos();
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_DEFAULT",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeFormat dxfg_TimeFormat_DEFAULT(final IsolateThread ignoredThread) {
    return NativeUtils.MAPPER_TIME_FORMAT.toNative(TimeFormat.DEFAULT);
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_GMT",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeFormat dxfg_TimeFormat_GMT(final IsolateThread ignoredThread) {
    return NativeUtils.MAPPER_TIME_FORMAT.toNative(TimeFormat.GMT);
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeFormat dxfg_TimeFormat_getInstance(
      final IsolateThread ignoredThread,
      final DxfgTimeZone dxfgTimeZone
  ) {
    return NativeUtils.MAPPER_TIME_FORMAT.toNative(
        TimeFormat.getInstance(NativeUtils.MAPPER_TIME_ZONE.toJava(dxfgTimeZone))
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_withTimeZone",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeFormat dxfg_TimeFormat_withTimeZone(
      final IsolateThread ignoredThread,
      final DxfgTimeFormat dxfgTimeFormat
  ) {
    return NativeUtils.MAPPER_TIME_FORMAT.toNative(
        NativeUtils.MAPPER_TIME_FORMAT.toJava(dxfgTimeFormat).withTimeZone()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_withMillis",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeFormat dxfg_TimeFormat_withMillis(
      final IsolateThread ignoredThread,
      final DxfgTimeFormat dxfgTimeFormat
  ) {
    return NativeUtils.MAPPER_TIME_FORMAT.toNative(
        NativeUtils.MAPPER_TIME_FORMAT.toJava(dxfgTimeFormat).withMillis()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_asFullIso",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeFormat dxfg_TimeFormat_asFullIso(
      final IsolateThread ignoredThread,
      final DxfgTimeFormat dxfgTimeFormat
  ) {
    return NativeUtils.MAPPER_TIME_FORMAT.toNative(
        NativeUtils.MAPPER_TIME_FORMAT.toJava(dxfgTimeFormat).asFullIso()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_parse",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_TimeFormat_parse(
      final IsolateThread ignoredThread,
      final DxfgTimeFormat dxfgTimeFormat,
      final CCharPointer value
  ) {
    return NativeUtils.MAPPER_TIME_FORMAT.toJava(dxfgTimeFormat).parse(
        NativeUtils.MAPPER_STRING.toJava(value)
    ).getTime();
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_format",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_TimeFormat_format(
      final IsolateThread ignoredThread,
      final DxfgTimeFormat dxfgTimeFormat,
      final long time
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_TIME_FORMAT.toJava(dxfgTimeFormat).format(time)
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeFormat_getTimeZone",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeZone dxfg_TimeFormat_getTimeZone(
      final IsolateThread ignoredThread,
      final DxfgTimeFormat dxfgTimeFormat
  ) {
    return NativeUtils.MAPPER_TIME_ZONE.toNative(
        NativeUtils.MAPPER_TIME_FORMAT.toJava(dxfgTimeFormat).getTimeZone()
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_valueOf",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgAuthToken dxfg_AuthToken_valueOf(
      final IsolateThread ignoredThread,
      final CCharPointer value
  ) {
    return NativeUtils.MAPPER_AUTH_TOKEN.toNative(
        AuthToken.valueOf(
            NativeUtils.MAPPER_STRING.toJava(value)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_createBasicToken",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgAuthToken dxfg_AuthToken_createBasicToken(
      final IsolateThread ignoredThread,
      final CCharPointer userPassword
  ) {
    return NativeUtils.MAPPER_AUTH_TOKEN.toNative(
        AuthToken.createBasicToken(
            NativeUtils.MAPPER_STRING.toJava(userPassword)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_createBasicToken2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgAuthToken dxfg_AuthToken_createBasicToken2(
      final IsolateThread ignoredThread,
      final CCharPointer user,
      final CCharPointer password
  ) {
    return NativeUtils.MAPPER_AUTH_TOKEN.toNative(
        AuthToken.createBasicToken(
            NativeUtils.MAPPER_STRING.toJava(user), NativeUtils.MAPPER_STRING.toJava(password)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_createBasicTokenOrNull",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgAuthToken dxfg_AuthToken_createBasicTokenOrNull(
      final IsolateThread ignoredThread,
      final CCharPointer user,
      final CCharPointer password
  ) {
    return NativeUtils.MAPPER_AUTH_TOKEN.toNative(
        AuthToken.createBasicTokenOrNull(
            NativeUtils.MAPPER_STRING.toJava(user), NativeUtils.MAPPER_STRING.toJava(password)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_createBearerToken",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgAuthToken dxfg_AuthToken_createBearerToken(
      final IsolateThread ignoredThread,
      final CCharPointer token
  ) {
    return NativeUtils.MAPPER_AUTH_TOKEN.toNative(
        AuthToken.createBearerToken(NativeUtils.MAPPER_STRING.toJava(token))
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_createBearerTokenOrNull",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgAuthToken dxfg_AuthToken_createBearerTokenOrNull(
      final IsolateThread ignoredThread,
      final CCharPointer token
  ) {
    return NativeUtils.MAPPER_AUTH_TOKEN.toNative(
        AuthToken.createBearerTokenOrNull(NativeUtils.MAPPER_STRING.toJava(token))
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_createCustomToken",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgAuthToken dxfg_AuthToken_createCustomToken(
      final IsolateThread ignoredThread,
      final CCharPointer scheme,
      final CCharPointer value
  ) {
    return NativeUtils.MAPPER_AUTH_TOKEN.toNative(
        AuthToken.createCustomToken(
            NativeUtils.MAPPER_STRING.toJava(scheme), NativeUtils.MAPPER_STRING.toJava(value)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_getHttpAuthorization",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_AuthToken_getHttpAuthorization(
      final IsolateThread ignoredThread,
      final DxfgAuthToken token
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_AUTH_TOKEN.toJava(token).getHttpAuthorization()
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_getUser",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_AuthToken_getUser(
      final IsolateThread ignoredThread,
      final DxfgAuthToken token
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_AUTH_TOKEN.toJava(token).getUser()
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_getPassword",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_AuthToken_getPassword(
      final IsolateThread ignoredThread,
      final DxfgAuthToken token
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_AUTH_TOKEN.toJava(token).getPassword()
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_getScheme",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_AuthToken_getScheme(
      final IsolateThread ignoredThread,
      final DxfgAuthToken token
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_AUTH_TOKEN.toJava(token).getScheme()
    );
  }

  @CEntryPoint(
      name = "dxfg_AuthToken_getValue",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_AuthToken_getValue(
      final IsolateThread ignoredThread,
      final DxfgAuthToken token
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_AUTH_TOKEN.toJava(token).getValue()
    );
  }

  @CEntryPoint(
      name = "dxfg_Object_new",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Object_new(
      final IsolateThread ignoredThread,
      @DxfgOut final DxfgJavaObjectHandlePointer object
  ) {
    if (object.isNull()) {
      throw new IllegalArgumentException("The `object` pointer is null");
    }

    object.write(
        (DxfgJavaObjectHandle) NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toNative(new Object()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Object_toString",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_Object_toString(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> dxfgObjectHandler
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(dxfgObjectHandler).toString()
    );
  }

  @CEntryPoint(
      name = "dxfg_Object_equals",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_Object_equals(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> dxfgObjectHandler,
      final JavaObjectHandler<Object> dxfgOther
  ) {
    var javaObject = NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(dxfgObjectHandler);
    var otherJavaObject = NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(dxfgOther);

    return Objects.equals(javaObject, otherJavaObject) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Object_hashCode",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_Object_hashCode(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> dxfgObjectHandler
  ) {
    return Objects.hashCode(NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(dxfgObjectHandler));
  }

  @CEntryPoint(
      name = "dxfg_Comparable_compareTo",
      exceptionHandler = ExceptionHandlerReturnMinusMinInteger.class
  )
  public static int dxfg_Comparable_compareTo(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> objectHandler,
      final JavaObjectHandler<Object> otherHandler
  ) {
    var javaObject = NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(objectHandler);
    var otherJavaObject = NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(otherHandler);

    if (javaObject == otherJavaObject) {
      return 0;
    }

    if (javaObject == null) {
      return -1;
    }

    if (otherJavaObject == null) {
      return 1;
    }

    //noinspection unchecked,rawtypes
    return ((Comparable)javaObject).compareTo(otherHandler);
  }

  @CEntryPoint(
      name = "dxfg_JavaObjectHandler_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_JavaObjectHandler_release(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> javaObjectHandler
  ) {
    NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.release(javaObjectHandler);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_JavaObjectHandler_clone",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_JavaObjectHandler_clone(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> javaObjectHandler,
      @DxfgOut final DxfgJavaObjectHandlerPointer javaObjectHandlerClone
  ) {
    if (javaObjectHandlerClone.isNull()) {
      throw new IllegalArgumentException("The `javaObjectHandlerClone` pointer is null");
    }

    var object = NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(javaObjectHandler);
    javaObjectHandlerClone.write(NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toNative(object));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_JavaObjectHandler_array_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_JavaObjectHandler_array_release(
      final IsolateThread ignoredThread,
      @CConst final JavaObjectHandler<Object> handlersArray,
      int size
  ) {
    var mapper = (JavaObjectHandlerMapper<Object, JavaObjectHandler<Object>>) NativeUtils
        .MAPPER_JAVA_OBJECT_HANDLER;

    mapper.releaseNativeArray(handlersArray, size);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_JavaObjectHandler_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_JavaObjectHandler_release(
      final IsolateThread ignoredThread,
      final DxfgJavaObjectHandlerList cList
  ) {
    NativeUtils.MAPPER_JAVA_OBJECT_HANDLERS.release(cList);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_String_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_String_release(
      final IsolateThread ignoreThread,
      final CCharPointer string
  ) {
    NativeUtils.MAPPER_STRING.release(string);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_String_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_String_release(
      final IsolateThread ignoreThread,
      final DxfgCStringListPointer strings
  ) {
    NativeUtils.MAPPER_STRINGS.release(strings);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Executors_newFixedThreadPool",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecutor dxfg_Executors_newFixedThreadPool(
      final IsolateThread ignoredThread,
      final int nThreads,
      final CCharPointer name
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        Executors.newFixedThreadPool(
            nThreads,
            new PoolThreadFactory(NativeUtils.MAPPER_STRING.toJava(name))
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Executors_newScheduledThreadPool",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecutor dxfg_Executors_newScheduledThreadPool(
      final IsolateThread ignoredThread,
      final int nThreads,
      final CCharPointer name
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        Executors.newScheduledThreadPool(
            nThreads,
            new PoolThreadFactory(NativeUtils.MAPPER_STRING.toJava(name))
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_ExecutorBaseOnConcurrentLinkedQueue_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecutor dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(new ExecutorBaseOnConcurrentLinkedQueue());
  }

  @CEntryPoint(
      name = "dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(
      final IsolateThread ignoredThread,
      final DxfgExecutor executor
  ) {
    ((ExecutorBaseOnConcurrentLinkedQueue) NativeUtils.MAPPER_EXECUTOR.toJava(
        executor)).processAllPendingTasks();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_gc",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_gc(final IsolateThread ignoredThread) {
    System.gc();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_throw_exception",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static JavaObjectHandler<?> dxfg_throw_exception(final IsolateThread ignoredThread) {
    recursion(DEEP_RECURSION_TO_THROW_EXCEPTION);
    return WordFactory.nullPointer();
  }

  private static void recursion(int i) {
    if (i < 0) {
      throw new RuntimeException("recursion", new RuntimeException("cause"));
    }
    recursion(--i);
  }

  @CEntryPoint(
      name = "dxfg_ByteArrayInputStream_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInputStream dxfg_ByteArrayInputStream_new(
      final IsolateThread ignoredThread,
      final CCharPointer dxfgBytes,
      final int size
  ) {
    final byte[] bytes = new byte[size];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = ((Pointer) dxfgBytes).readByte(i);
    }
    return NativeUtils.MAPPER_INPUT_STREAM.toNative(new ByteArrayInputStream(bytes));
  }

  @CEntryPoint(
      name = "dxfg_Object_finalize",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Object_finalize(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> dxfgObjectHandler,
      final DxfgFinalizeFunction finalizeFunction,
      final VoidPointer userData
  ) {
    NativeUtils.FINALIZER.createFinalizer(
        NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(dxfgObjectHandler),
        new Runnable() {
          @Override
          public void run() {
            finalizeFunction.invoke(CurrentIsolate.getCurrentThread(), userData);
          }
        }
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  private static class ExecutorBaseOnConcurrentLinkedQueue implements Executor {

    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void execute(final Runnable command) {
      taskQueue.add(command);
    }

    public void processAllPendingTasks() {
      Runnable task;
      while ((task = taskQueue.poll()) != null) {
        task.run();
      }
    }
  }

  private static class PoolThreadFactory implements ThreadFactory {

    private final String name;
    private final AtomicInteger index = new AtomicInteger();
    private final ThreadGroup group;

    {
      final SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() :
          Thread.currentThread().getThreadGroup();
    }

    PoolThreadFactory(final String name) {
      this.name = name;
    }

    @Override
    public Thread newThread(final Runnable r) {
      final Thread thread = new Thread(group, r, name + "-" + index.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    }
  }
}
