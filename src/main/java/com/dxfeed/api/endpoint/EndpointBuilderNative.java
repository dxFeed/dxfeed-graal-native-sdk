package com.dxfeed.api.endpoint;

import static com.dxfeed.api.NativeUtils.createHandler;
import static com.dxfeed.api.NativeUtils.destroyHandler;
import static com.dxfeed.api.NativeUtils.extractHandler;
import static com.dxfeed.api.NativeUtils.toJavaString;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

@CContext(Directives.class)
public final class EndpointBuilderNative {

  @CEntryPoint(
      name = "dxfg_endpoint_builder_create",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder
  ) {
    builder.setJavaObjectHandler(createHandler(DXEndpoint.newBuilder()));
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
    destroyHandler(builder.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withRole(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final DxfgEndpointRole role
  ) {
    toJavaBuilder(builder).withRole(role.qdRole);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_name",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withName(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer name
  ) {
    toJavaBuilder(builder).withName(toJavaString(name));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_property",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withProperty(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer key,
      final CCharPointer value
  ) {
    toJavaBuilder(builder).withProperty(toJavaString(key), toJavaString(value));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_builder_with_properties",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int withProperties(
      final IsolateThread ignoreThread,
      final DxfgEndpointBuilder builder,
      final CCharPointer pathToFile
  ) throws IOException {
   final Properties properties = new Properties();
    properties.load(new FileInputStream(toJavaString(pathToFile)));
    toJavaBuilder(builder).withProperties(properties);
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
        toJavaBuilder(builder).supportsProperty(toJavaString(key))
            ? 1
            : 0
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
      final DxfgEndpoint dxfgEndpoint
  ) {
    dxfgEndpoint.setJavaObjectHandler(createHandler(toJavaBuilder(builder).build()));
    return EXECUTE_SUCCESSFULLY;
  }

  private static DXEndpoint.Builder toJavaBuilder(final DxfgEndpointBuilder builder) {
    return extractHandler(builder.getJavaObjectHandler());
  }
}
