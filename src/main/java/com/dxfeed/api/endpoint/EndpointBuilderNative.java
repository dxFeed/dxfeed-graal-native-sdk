package com.dxfeed.api.endpoint;

import static com.dxfeed.api.NativeUtils.MAPPER_ENDPOINT;
import static com.dxfeed.api.NativeUtils.MAPPER_ENDPOINT_BUILDER;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
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
    return MAPPER_ENDPOINT_BUILDER.toNative(DXEndpoint.newBuilder());
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
    MAPPER_ENDPOINT_BUILDER.toJava(builder).withRole(role.qdRole);
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_ENDPOINT_BUILDER.toJava(builder).withName(MAPPER_STRING.toJava(name));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_ENDPOINT_BUILDER.toJava(builder).withProperty(
        MAPPER_STRING.toJava(key),
        MAPPER_STRING.toJava(value)
    );
    return EXECUTE_SUCCESSFULLY;
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
    properties.load(new FileInputStream(MAPPER_STRING.toJava(pathToFile)));
    MAPPER_ENDPOINT_BUILDER.toJava(builder).withProperties(properties);
    return EXECUTE_SUCCESSFULLY;
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
    return MAPPER_ENDPOINT_BUILDER.toJava(builder).supportsProperty(MAPPER_STRING.toJava(key))
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
    return MAPPER_ENDPOINT.toNative(MAPPER_ENDPOINT_BUILDER.toJava(builder).build());
  }
}
