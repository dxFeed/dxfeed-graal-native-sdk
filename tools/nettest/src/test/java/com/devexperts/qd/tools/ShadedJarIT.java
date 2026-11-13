// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.Test;

/** The all-in-one jar as operators start it: {@code java -jar graal-native-sdk-nettest-<version>-all.jar ...}. */
public class ShadedJarIT {
    @Test
    public void toolListHasNetTestAndNetTestApi() throws Exception {
        Result result = run("Help", "contents");

        assertEquals(result.output, 0, result.exitCode);
        assertTrue(result.output, Pattern.compile("(?m)^\\s+NetTest$").matcher(result.output).find());
        assertTrue(result.output, Pattern.compile("(?m)^\\s+NetTestApi$").matcher(result.output).find());
    }

    @Test
    public void netTestApiWithoutArgumentsPrintsTheUsageAndExitsWithTwo() throws Exception {
        Result result = run("NetTestApi");

        assertEquals(result.output, 2, result.exitCode);
        assertTrue(result.output, result.output.contains("p <address>"));
    }

    @Test
    public void qdVersionIsPrinted() throws Exception {
        Result result = run("NetTestApi", "p", ":0", "-S", "1", "--duration", "1s");

        assertEquals(result.output, 0, result.exitCode);
        assertTrue(result.output, result.output.contains("Using QDS-" + System.getProperty("nettest.qdVersion")));
    }

    private static Result run(String... args) throws IOException, InterruptedException {
        Path jar = Paths.get(System.getProperty("nettest.shadedJar"));
        assertTrue("no " + jar, Files.isRegularFile(jar));
        List<String> command = new ArrayList<>(Arrays.asList(
            Paths.get(System.getProperty("java.home"), "bin", "java").toString(), "-jar", jar.toString()));
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread drain = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                in.transferTo(output);
            } catch (IOException e) {
                // the process is gone
            }
        });
        drain.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("no exit in 60 s: " + command);
        }
        drain.join(5_000);
        return new Result(process.exitValue(), output.toString(StandardCharsets.UTF_8));
    }

    private static final class Result {
        final int exitCode;
        final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
