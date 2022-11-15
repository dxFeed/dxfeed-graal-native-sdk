package com.dxfeed.api.endpoint;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

@CContext(Directives.class)
public final class EndpointBuilderNative extends BaseNative {

  @CEntryPoint(
      name = "dxfg_endpoint_builder_create",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder
  ) {
    builder.setJavaObjectHandler(createBuilder());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int release(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder
  ) {
    releaseBuilder(builder);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withRole(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      DxfgEndpointRole role
  ) {
    getBuilder(builder)
        .withRole(DxfgEndpointRole.toDXEndpointRole(role));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_name",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withName(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      CCharPointer name
  ) {
    getBuilder(builder)
        .withName(toJavaString(name));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_property",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withProperty(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      CCharPointer key,
      CCharPointer value
  ) {
    getBuilder(builder)
        .withProperty(toJavaString(key), toJavaString(value));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_supports_property",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int supportsProperty(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer key,
      final CIntPointer isSupports
  ) {
    isSupports.write(
        getBuilder(builder).supportsProperty(toJavaString(key)) ? 1 : 0
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_build",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int build(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final DxfgEndpoint endpoint
  ) {
    endpoint.setJavaObjectHandler(buildEndpoint(builder));
    return EXECUTE_SUCCESSFULLY;
  }

  private static ObjectHandle createBuilder() {
    return createJavaObjectHandler(DXEndpoint.newBuilder());
  }

  private static void releaseBuilder(final DxfgEndpointBuilder builder) {
    destroyJavaObjectHandler(builder.getJavaObjectHandler());
  }

  private static DXEndpoint.Builder getBuilder(final DxfgEndpointBuilder builder) {
    return getJavaObject(builder.getJavaObjectHandler());
  }

  private static ObjectHandle buildEndpoint(final DxfgEndpointBuilder builder) {
    return createJavaObjectHandler(getBuilder(builder).build());
  }
}
