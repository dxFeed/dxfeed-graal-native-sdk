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
    dxfgEndpointBuilder.setJavaObjectHandler(WordFactory.nullPointer());
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
    final var oldBuilder = getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler());
    final var newBuilder = oldBuilder.withRole(DxfgEndpointRole.toDxEndpointRole(role));
    compareAndReplaceBuilderIfNeeded(oldBuilder, newBuilder, dxfgEndpointBuilder);
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
    final var oldBuilder = getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler()).withName(toJavaString(name));
    final var newBuilder = oldBuilder.withName(toJavaString(name));
    compareAndReplaceBuilderIfNeeded(oldBuilder, newBuilder, dxfgEndpointBuilder);
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
    final var oldBuilder = getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler());
    final var newBuilder = oldBuilder.withProperty(toJavaString(key), toJavaString(value));
    compareAndReplaceBuilderIfNeeded(oldBuilder, newBuilder, dxfgEndpointBuilder);
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
    final var builder = getDxEndpointBuilder(dxfgEndpointBuilder.getJavaObjectHandler());
    isSupports.write(builder.supportsProperty(toJavaString(key)) ? 1 : 0);
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

  /**
   * Compares the old builder (associated with dxfgEndpointBuilder) with the new builder and
   * destroys the old builder if it differs from the new builder. The new builder will be written to
   * dxfgEndpointBuilder object handle.
   *
   * @param oldBuilder          The old DXEndpoint.Builder.
   * @param newBuilder          The old DXEndpoint.Builder.
   * @param dxfgEndpointBuilder The object handle.
   */
  private static void compareAndReplaceBuilderIfNeeded(
      final DXEndpoint.Builder oldBuilder,
      final DXEndpoint.Builder newBuilder,
      final DxfgEndpointBuilder dxfgEndpointBuilder
  ) {
    if (oldBuilder != newBuilder) {
      destroyJavaObjectHandler(dxfgEndpointBuilder.getJavaObjectHandler());
      dxfgEndpointBuilder.setJavaObjectHandler(createJavaObjectHandler(newBuilder));
    }
  }
}
