package com.dxfeed.api.endpoint;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.WordFactory;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

@CContext(Directives.class)
public final class EndpointBuilderNative extends BaseNative {

  @CEntryPoint(
      name = "dxfg_endpoint_builder_create",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder dxfgEndpointBuilder
  ) {
    dxfgEndpointBuilder.setJavaObjectHandler(createJavaObjectHandler(DXEndpoint.newBuilder()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int close(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder dxfgEndpointBuilder
  ) {
    destroyJavaObjectHandler(dxfgEndpointBuilder.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withRole(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder dxfgEndpointBuilder,
      DxfgEndpointRole role
  ) {
    getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler())
        .withRole(DxfgEndpointRole.toDXEndpointRole(role));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_name",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withName(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder dxfgEndpointBuilder,
      CCharPointer name
  ) {
    getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler())
        .withName(toJavaString(name));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_property",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withProperty(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder dxfgEndpointBuilder,
      CCharPointer key,
      CCharPointer value
  ) {
    getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler())
        .withProperty(toJavaString(key), toJavaString(value));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_supports_property",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int supportsProperty(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder dxfgEndpointBuilder,
      CCharPointer key,
      CIntPointer isSupports
  ) {
    isSupports.write(
        getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler())
            .supportsProperty(toJavaString(key)) ? 1 : 0);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_build",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int build(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder dxfgEndpointBuilder,
      final DxfgEndpoint dxfgEndpoint
  ) {
    dxfgEndpoint.setJavaObjectHandler(createJavaObjectHandler(
        getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler()).build()));
    return EXECUTE_SUCCESSFULLY;
  }
}
