package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.Day;
import com.dxfeed.schedule.DayFilter;
import com.dxfeed.schedule.Schedule;
import com.dxfeed.schedule.Session;
import com.dxfeed.schedule.SessionFilter;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CCharPointerPointerPointer;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile2Pointer;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import java.io.IOException;
import com.dxfeed.sdk.javac.DxfgCStringListPointerPointer;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.word.Pointer;

@CContext(Directives.class)
public class ScheduleNative {

  @CEntryPoint(
      name = "dxfg_SessionFilter_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSessionFilter dxfg_SessionFilter_new(
      final IsolateThread ignoredThread,
      final DxfgSessionFilterFunction dxfgSessionFilterFunction
  ) {
    return NativeUtils.MAPPER_SESSION_FILTER.toNative(
        new SessionFilter(null, null) {
          @Override
          public boolean accept(final Session session) {
            return dxfgSessionFilterFunction.invoke(
                CurrentIsolate.getCurrentThread(),
                NativeUtils.MAPPER_SESSION.toNative(session)
            ) == 1;
          }
        }
    );
  }

  @CEntryPoint(
      name = "dxfg_SessionFilter_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSessionFilter dxfg_SessionFilter_getInstance(
      final IsolateThread ignoredThread,
      final DxfgSessionFilterPrepare dxfgSessionFilterPrepare
  ) {
    return NativeUtils.MAPPER_SESSION_FILTER.toNative(dxfgSessionFilterPrepare.sessionFilter);
  }

  @CEntryPoint(
      name = "dxfg_DayFilter_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDayFilter dxfg_DayFilter_new(
      final IsolateThread ignoredThread,
      final DxfgDayFilterFunction dxfgDayFilterFunction
  ) {
    return NativeUtils.MAPPER_DAY_FILTER.toNative(
        new DayFilter(0, null, null, null) {
          @Override
          public boolean accept(final Day day) {
            return dxfgDayFilterFunction.invoke(
                CurrentIsolate.getCurrentThread(),
                NativeUtils.MAPPER_DAY.toNative(day)
            ) == 1;
          }
        }
    );
  }

  @CEntryPoint(
      name = "dxfg_DayFilter_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDayFilter dxfg_DayFilter_getInstance(
      final IsolateThread ignoredThread,
      final DxfgDayFilterPrepare dxfgDayFilterPrepare
  ) {
    return NativeUtils.MAPPER_DAY_FILTER.toNative(dxfgDayFilterPrepare.dayFilter);
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgScheduleHandle dxfg_Schedule_getInstance(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile dxfgInstrumentProfile
  ) {
    //noinspection DataFlowIssue
    return NativeUtils.MAPPER_SCHEDULE.toNative(
        Schedule.getInstance(NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(dxfgInstrumentProfile))
    );
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getInstance_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_getInstance_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile2Pointer instrumentProfile,
      final @DxfgOut DxfgScheduleHandlePointer schedule
  ) {
    if (schedule.isNull()) {
      throw new IllegalArgumentException("The `schedule` pointer is null");
    }

    //noinspection DataFlowIssue
    schedule.write(NativeUtils.MAPPER_SCHEDULE.toNative(
        Schedule.getInstance(NativeUtils.MAPPER_INSTRUMENT_PROFILE_2.toJava(instrumentProfile))
    ));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getInstance_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_getInstance_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile2Pointer instrumentProfile,
      final @DxfgOut DxfgScheduleHandlePointer schedule
  ) {
    if (schedule.isNull()) {
      throw new IllegalArgumentException("The `schedule` pointer is null");
    }

    //noinspection DataFlowIssue
    schedule.write(NativeUtils.MAPPER_SCHEDULE.toNative(
        Schedule.getInstance(
            NativeUtils.MAPPER_INSTRUMENT_PROFILE_2_CACHED.toJava(instrumentProfile))
    ));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getInstance2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgScheduleHandle dxfg_Schedule_getInstance2(
      final IsolateThread ignoredThread,
      final CCharPointer scheduleDefinition
  ) {
    return NativeUtils.MAPPER_SCHEDULE.toNative(
        Schedule.getInstance(NativeUtils.MAPPER_STRING.toJava(scheduleDefinition))
    );
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getInstance3",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgScheduleHandle dxfg_Schedule_getInstance3(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile dxfgInstrumentProfile,
      final CCharPointer venue
  ) {
    return NativeUtils.MAPPER_SCHEDULE.toNative(
        Schedule.getInstance(
            NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(dxfgInstrumentProfile),
            NativeUtils.MAPPER_STRING.toJava(venue)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getInstance3_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_getInstance3_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile2Pointer instrumentProfile,
      @CConst final CCharPointer venue,
      final @DxfgOut DxfgScheduleHandlePointer schedule
  ) {
    if (schedule.isNull()) {
      throw new IllegalArgumentException("The `schedule` pointer is null");
    }

    //noinspection DataFlowIssue
    schedule.write(NativeUtils.MAPPER_SCHEDULE.toNative(
        Schedule.getInstance(NativeUtils.MAPPER_INSTRUMENT_PROFILE_2.toJava(instrumentProfile),
            NativeUtils.MAPPER_STRING.toJava(venue))
    ));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getInstance3_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_getInstance3_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile2Pointer instrumentProfile,
      @CConst final CCharPointer venue,
      final @DxfgOut DxfgScheduleHandlePointer schedule
  ) {
    if (schedule.isNull()) {
      throw new IllegalArgumentException("The `schedule` pointer is null");
    }

    //noinspection DataFlowIssue
    schedule.write(NativeUtils.MAPPER_SCHEDULE.toNative(
        Schedule.getInstance(
            NativeUtils.MAPPER_INSTRUMENT_PROFILE_2_CACHED.toJava(instrumentProfile),
            NativeUtils.MAPPER_STRING.toJava(venue))
    ));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getTradingVenues",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgCStringListPointer dxfg_Schedule_getTradingVenues(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile dxfgInstrumentProfile
  ) {
    return NativeUtils.MAPPER_STRINGS.toNativeList(
        Schedule.getTradingVenues(
            NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(dxfgInstrumentProfile))
    );
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getTradingVenues_v2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_getTradingVenues_v2(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile2Pointer instrumentProfile,
      @DxfgOut DxfgCStringListPointerPointer venues
  ) {
    if (venues.isNull()) {
      throw new IllegalArgumentException("The `venues` pointer is null");
    }

    //noinspection DataFlowIssue
    var javaVenues = Schedule.getTradingVenues(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_2.toJava(instrumentProfile));

    venues.write(NativeUtils.MAPPER_STRINGS.toNativeList(javaVenues));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getTradingVenues_v2_cached",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_getTradingVenues_v2_cached(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile2Pointer instrumentProfile,
      @DxfgOut DxfgCStringListPointerPointer venues
  ) {
    if (venues.isNull()) {
      throw new IllegalArgumentException("The `venues` pointer is null");
    }

    //noinspection DataFlowIssue
    var javaVenues = Schedule.getTradingVenues(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_2_CACHED.toJava(instrumentProfile));

    venues.write(NativeUtils.MAPPER_STRINGS.toNativeList(javaVenues));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_downloadDefaults",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_downloadDefaults(
      final IsolateThread ignoredThread,
      final CCharPointer scheduleDefinition
  ) {
    Schedule.downloadDefaults(NativeUtils.MAPPER_STRING.toJava(scheduleDefinition));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_setDefaults",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Schedule_setDefaults(
      final IsolateThread ignoredThread,
      final CCharPointer data,
      final int size
  ) throws IOException {
    final byte[] bytes = new byte[size];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = ((Pointer) data).readByte(i);
    }
    Schedule.setDefaults(bytes);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getSessionByTime",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Schedule_getSessionByTime(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule,
      final long time
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getSessionByTime(time));
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getDayByTime",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Schedule_getDayByTime(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule,
      final long time
  ) {
    return NativeUtils.MAPPER_DAY.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getDayByTime(time));
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getDayById",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Schedule_getDayById(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule,
      final int dayId
  ) {
    return NativeUtils.MAPPER_DAY.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getDayById(dayId));
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getDayByYearMonthDay",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Schedule_getDayByYearMonthDay(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule,
      final int yearMonthDay
  ) {
    return NativeUtils.MAPPER_DAY.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getDayByYearMonthDay(yearMonthDay)
    );
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getNearestSessionByTime",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Schedule_getNearestSessionByTime(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule,
      final long time,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getNearestSessionByTime(
            time,
            NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Schedule_findNearestSessionByTime",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Schedule_findNearestSessionByTime(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule,
      final long time,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).findNearestSessionByTime(
            time,
            NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getName",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_Schedule_getName(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getName());
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getTimeZone",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_Schedule_getTimeZone(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getTimeZone().getDisplayName());
  }

  @CEntryPoint(
      name = "dxfg_Schedule_getTimeZone_getID",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_Schedule_getTimeZone_getID(
      final IsolateThread ignoredThread,
      final DxfgScheduleHandle dxfgSchedule
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_SCHEDULE.toJava(dxfgSchedule).getTimeZone().getID());
  }

  @CEntryPoint(
      name = "dxfg_Session_getDay",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Session_getDay(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return NativeUtils.MAPPER_DAY.toNative(NativeUtils.MAPPER_SESSION.toJava(dxfgSession).getDay());
  }

  @CEntryPoint(
      name = "dxfg_Session_getType",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Session_getType(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return DxfgSessionType.of(NativeUtils.MAPPER_SESSION.toJava(dxfgSession).getType()).getCValue();
  }

  @CEntryPoint(
      name = "dxfg_Session_isTrading",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Session_isTrading(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return NativeUtils.MAPPER_SESSION.toJava(dxfgSession).isTrading() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Session_isEmpty",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Session_isEmpty(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return NativeUtils.MAPPER_SESSION.toJava(dxfgSession).isEmpty() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Session_getStartTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_Session_getStartTime(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return NativeUtils.MAPPER_SESSION.toJava(dxfgSession).getStartTime();
  }

  @CEntryPoint(
      name = "dxfg_Session_getEndTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_Session_getEndTime(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return NativeUtils.MAPPER_SESSION.toJava(dxfgSession).getEndTime();
  }

  @CEntryPoint(
      name = "dxfg_Session_containsTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Session_containsTime(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession,
      final long time
  ) {
    return NativeUtils.MAPPER_SESSION.toJava(dxfgSession).containsTime(time) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Session_getPrevSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Session_getPrevSession(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_SESSION.toJava(dxfgSession)
            .getPrevSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Session_getNextSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Session_getNextSession(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_SESSION.toJava(dxfgSession)
            .getNextSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Session_findPrevSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Session_findPrevSession(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_SESSION.toJava(dxfgSession)
            .findPrevSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Session_findNextSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Session_findNextSession(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_SESSION.toJava(dxfgSession)
            .findNextSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Session_hashCode",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Session_hashCode(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return NativeUtils.MAPPER_SESSION.toJava(dxfgSession).hashCode();
  }

  @CEntryPoint(
      name = "dxfg_Session_equals",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Session_equals(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession,
      final DxfgSession dxfgOtherSession
  ) {
    return NativeUtils.MAPPER_SESSION.toJava(dxfgSession)
        .equals(NativeUtils.MAPPER_SESSION.toJava(dxfgOtherSession)) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Session_toString",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_Session_toString(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_SESSION.toJava(dxfgSession).toString());
  }

  @CEntryPoint(
      name = "dxfg_Day_getSchedule",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgScheduleHandle dxfg_Day_getSchedule(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_SCHEDULE.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay).getSchedule());
  }

  @CEntryPoint(
      name = "dxfg_Day_getDayId",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_getDayId(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getDayId();
  }

  @CEntryPoint(
      name = "dxfg_Day_getYearMonthDay",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_getYearMonthDay(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getYearMonthDay();
  }

  @CEntryPoint(
      name = "dxfg_Day_getYear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_getYear(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getYear();
  }

  @CEntryPoint(
      name = "dxfg_Day_getMonthOfYear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_getMonthOfYear(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getMonthOfYear();
  }

  @CEntryPoint(
      name = "dxfg_Day_getDayOfMonth",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_getDayOfMonth(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getDayOfMonth();
  }

  @CEntryPoint(
      name = "dxfg_Day_getDayOfWeek",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_getDayOfWeek(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getDayOfWeek();
  }

  @CEntryPoint(
      name = "dxfg_Day_isHoliday",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_isHoliday(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).isHoliday() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Day_isShortDay",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_isShortDay(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).isShortDay() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Day_isTrading",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_isTrading(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).isTrading() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Day_getStartTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_Day_getStartTime(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getStartTime();
  }

  @CEntryPoint(
      name = "dxfg_Day_getEndTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_Day_getEndTime(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getEndTime();
  }

  @CEntryPoint(
      name = "dxfg_Day_containsTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_containsTime(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final long time
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).containsTime(time) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Day_getResetTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_Day_getResetTime(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).getResetTime();
  }

  @CEntryPoint(
      name = "dxfg_Day_getSessions",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSessionList dxfg_Day_getSessions(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_SESSIONS.toNativeList(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay).getSessions());
  }

  @CEntryPoint(
      name = "dxfg_Day_getSessionByTime",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Day_getSessionByTime(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final long time
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay).getSessionByTime(time));
  }

  @CEntryPoint(
      name = "dxfg_Day_getFirstSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Day_getFirstSession(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .getFirstSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_getLastSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Day_getLastSession(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .getLastSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_findFirstSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Day_findFirstSession(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .findFirstSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_findLastSession",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSession dxfg_Day_findLastSession(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgSessionFilter dxfgSessionFilter
  ) {
    return NativeUtils.MAPPER_SESSION.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .findLastSession(NativeUtils.MAPPER_SESSION_FILTER.toJava(dxfgSessionFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_getPrevDay",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Day_getPrevDay(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgDayFilter dxfgDayFilter
  ) {
    return NativeUtils.MAPPER_DAY.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .getPrevDay(NativeUtils.MAPPER_DAY_FILTER.toJava(dxfgDayFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_getNextDay",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Day_getNextDay(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgDayFilter dxfgDayFilter
  ) {
    return NativeUtils.MAPPER_DAY.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .getNextDay(NativeUtils.MAPPER_DAY_FILTER.toJava(dxfgDayFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_findPrevDay",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Day_findPrevDay(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgDayFilter dxfgDayFilter
  ) {
    return NativeUtils.MAPPER_DAY.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .findPrevDay(NativeUtils.MAPPER_DAY_FILTER.toJava(dxfgDayFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_findNextDay",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgDay dxfg_Day_findNextDay(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgDayFilter dxfgDayFilter
  ) {
    return NativeUtils.MAPPER_DAY.toNative(
        NativeUtils.MAPPER_DAY.toJava(dxfgDay)
            .findNextDay(NativeUtils.MAPPER_DAY_FILTER.toJava(dxfgDayFilter))
    );
  }

  @CEntryPoint(
      name = "dxfg_Day_hashCode",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_hashCode(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_DAY.toJava(dxfgDay).hashCode();
  }

  @CEntryPoint(
      name = "dxfg_Day_equals",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_equals(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay,
      final DxfgDay dxfgOtherDay
  ) {
    return
        NativeUtils.MAPPER_DAY.toJava(dxfgDay).equals(NativeUtils.MAPPER_DAY.toJava(dxfgOtherDay))
            ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Day_toString",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_Day_toString(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    return NativeUtils.MAPPER_STRING.toNative(NativeUtils.MAPPER_DAY.toJava(dxfgDay).toString());
  }

  @CEntryPoint(
      name = "dxfg_Day_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Day_release(
      final IsolateThread ignoredThread,
      final DxfgDay dxfgDay
  ) {
    NativeUtils.MAPPER_DAY.release(dxfgDay);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Session_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Session_release(
      final IsolateThread ignoredThread,
      final DxfgSession dxfgSession
  ) {
    NativeUtils.MAPPER_SESSION.release(dxfgSession);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_SessionList_wrapper_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_SessionList_wrapper_release(
      final IsolateThread ignoredThread,
      final DxfgSessionList dxfgSessions
  ) {
    // release only the struct dxfg_session_list and keep the elements
    UnmanagedMemory.free(dxfgSessions.getElements());
    UnmanagedMemory.free(dxfgSessions);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }
}
