package com.dxfeed.api.publisher;

import static com.dxfeed.api.NativeUtils.MAPPER_ENEVET_TYPES;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.api.NativeUtils.newJavaObjectHandler;
import static com.dxfeed.api.NativeUtils.toJava;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgEventClazzList;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(Directives.class)
public class PublisherNative {

  @CEntryPoint(
      name = "dxfg_DXPublisher_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPublisher dxfg_DXPublisher_getInstance(
      final IsolateThread ignoredThread
  ) {
    return newJavaObjectHandler(DXPublisher.getInstance());
  }

  @CEntryPoint(
      name = "dxfg_DXPublisher_publishEvents",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXPublisher_publishEvents(
      final IsolateThread ignoredThread,
      final DxfgPublisher dxfgPublisher,
      final DxfgEventTypeList dxfgEventTypeList
  ) {
    toJava(dxfgPublisher).publishEvents(MAPPER_EVENTS.toJavaList(dxfgEventTypeList));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXPublisher_getSubscription",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableSubscription dxfg_DXPublisher_getSubscription(
      final IsolateThread ignoredThread,
      final DxfgPublisher dxfgPublisher,
      final DxfgEventClazz dxfgClazz
  ) {
    return newJavaObjectHandler(toJava(dxfgPublisher).getSubscription(dxfgClazz.clazz));
  }

  @CEntryPoint(
      name = "dxfg_ObservableSubscription_isClosed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableSubscription_isClosed(
      final IsolateThread ignoredThread,
      final DxfgObservableSubscription dxfgObservableSubscription
  ) {
    return toJava(dxfgObservableSubscription).isClosed() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_ObservableSubscription_getEventTypes",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventClazzList dxfg_ObservableSubscription_getEventTypes(
      final IsolateThread ignoredThread,
      final DxfgObservableSubscription dxfgObservableSubscription
  ) {
    return MAPPER_ENEVET_TYPES.toNativeList(toJava(dxfgObservableSubscription).getEventTypes());
  }

  @CEntryPoint(
      name = "dxfg_ObservableSubscription_containsEventType",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableSubscription_containsEventType(
      final IsolateThread ignoredThread,
      final DxfgObservableSubscription dxfgObservableSubscription,
      final DxfgEventClazz dxfgClazz
  ) {
    return toJava(dxfgObservableSubscription).containsEventType(dxfgClazz.clazz) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_ObservableSubscription_addChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableSubscription_addChangeListener(
      final IsolateThread ignoreThread,
      final DxfgObservableSubscription dxfgObservableSubscription,
      final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
  ) {
    toJava(dxfgObservableSubscription).addChangeListener(toJava(dxfgFeedEventListener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_ObservableSubscription_removeChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableSubscription_removeChangeListener(
      final IsolateThread ignoreThread,
      final DxfgObservableSubscription dxfgObservableSubscription,
      final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
  ) {
    toJava(dxfgObservableSubscription).removeChangeListener(toJava(dxfgFeedEventListener));
    return EXECUTE_SUCCESSFULLY;
  }
}
