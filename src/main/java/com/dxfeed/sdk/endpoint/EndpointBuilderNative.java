package com.dxfeed.sdk.endpoint;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public final class EndpointBuilderNative {

  @CEntryPoint(
      name = "dxfg_DXEndpoint_newBuilder",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpointBuilder dxfg_DXEndpoint_newBuilder(
      final IsolateThread ignoreThread
  ) {
    return NativeUtils.MAPPER_ENDPOINT_BUILDER.toNative(DXEndpoint.newBuilder());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_Builder_withRole",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_Builder_withRole(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final DxfgEndpointRole role
  ) {
    NativeUtils.MAPPER_ENDPOINT_BUILDER.toJava(builder).withRole(role.qdRole);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_Builder_withName",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_Builder_withName(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer name
  ) {
    NativeUtils.MAPPER_ENDPOINT_BUILDER.toJava(builder).withName(NativeUtils.MAPPER_STRING.toJava(name));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_Builder_withProperty",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_Builder_withProperty(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer key,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_ENDPOINT_BUILDER.toJava(builder).withProperty(
        NativeUtils.MAPPER_STRING.toJava(key),
        NativeUtils.MAPPER_STRING.toJava(value)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_Builder_withProperties",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_Builder_withProperties(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer pathToFile
  ) throws IOException {
    final Properties properties = new Properties();
    properties.load(new FileInputStream(NativeUtils.MAPPER_STRING.toJava(pathToFile)));
    NativeUtils.MAPPER_ENDPOINT_BUILDER.toJava(builder).withProperties(properties);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_Builder_supportsProperty",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_Builder_supportsProperty(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer key
  ) {
    return NativeUtils.MAPPER_ENDPOINT_BUILDER.toJava(builder).supportsProperty(NativeUtils.MAPPER_STRING.toJava(key))
        ? 1
        : 0;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_Builder_build",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_Builder_build(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder
  ) {
    return NativeUtils.MAPPER_ENDPOINT.toNative(NativeUtils.MAPPER_ENDPOINT_BUILDER.toJava(builder).build());
  }
}
