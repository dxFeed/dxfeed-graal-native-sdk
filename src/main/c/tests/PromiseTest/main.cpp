// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// A promise created, completed and failed from C (dxfg_v4_Promise_new(), dxfg_v4_Promise_complete(),
// dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(), dxfg_v4_Promise_failedWithClassNameAndMessage()):
// the result and the exception that the handlers see, the classes of the exceptions in the image, the first result
// that wins, the older functions that keep their behaviour, and the NULL arguments.

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <initializer_list>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace test {

namespace {

const Isolate &isolate() {
    static const Isolate instance{};

    return instance;
}

// The calls of a promise handler: the calling thread and what the promise looked like.
struct HandlerCalls {
    std::vector<std::thread::id> threads{};
    int32_t hasResult = -1;
    int32_t hasException = -1;
    std::string exceptionClassName{};
    std::string exceptionMessage{};

    static void handle(graal_isolatethread_t *thread, dxfg_promise_t *promise, void *userData) {
        auto *calls = static_cast<HandlerCalls *>(userData);

        calls->threads.push_back(std::this_thread::get_id());
        dxfg_v4_Promise_hasResult(thread, promise, &calls->hasResult);
        dxfg_v4_Promise_hasException(thread, promise, &calls->hasException);

        dxfg_v4_exception_t *exception = nullptr;

        if (dxfg_v4_Promise_getException(thread, promise, &exception) == DXFG_EXECUTE_SUCCESSFULLY &&
            exception != nullptr) {
            calls->exceptionClassName = exception->class_name != nullptr ? exception->class_name : "";
            calls->exceptionMessage = exception->message != nullptr ? exception->message : "<null>";
            dxfg_v4_Exception_release(thread, exception);
        }
    }
};

struct Failure {
    std::string className;
    std::string message;
};

// The exception of a failed promise; an empty class name if the promise has none.
Failure exceptionOf(graal_isolatethread_t *thread, dxfg_v4_promise_t *promise) {
    dxfg_v4_exception_t *exception = nullptr;

    REQUIRE(dxfg_v4_Promise_getException(thread, promise, &exception) == DXFG_EXECUTE_SUCCESSFULLY);

    if (exception == nullptr) {
        return {};
    }

    Failure failure{exception->class_name != nullptr ? exception->class_name : "",
                    exception->message != nullptr ? exception->message : "<null>"};

    CHECK(dxfg_v4_Exception_release(thread, exception) == DXFG_EXECUTE_SUCCESSFULLY);

    return failure;
}

dxfg_v4_promise_t *newPromise(graal_isolatethread_t *thread) {
    dxfg_v4_promise_t *promise = nullptr;

    REQUIRE(dxfg_v4_Promise_new(thread, &promise) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(promise != nullptr);

    return promise;
}

int32_t isDone(graal_isolatethread_t *thread, dxfg_v4_promise_t *promise) {
    int32_t done = -1;

    REQUIRE(dxfg_v4_Promise_isDone(thread, promise, &done) == DXFG_EXECUTE_SUCCESSFULLY);

    return done;
}

void release(graal_isolatethread_t *thread, dxfg_v4_promise_t *promise) {
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &promise->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

// Fails a new promise with the class name and the message and returns its exception.
Failure failNew(graal_isolatethread_t *thread, const char *className, const char *message) {
    dxfg_v4_promise_t *promise = newPromise(thread);

    REQUIRE(dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(thread, promise, className, message) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    const Failure failure = exceptionOf(thread, promise);

    release(thread, promise);

    return failure;
}

} // namespace

TEST_CASE("A new promise is not done; a completed one gives its result to the handler once") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *promise = newPromise(thread);
    CHECK(isDone(thread, promise) == 0);

    dxfg_v4_java_object_handler *result = &promise->handler; // a marker: the call must overwrite it
    CHECK(dxfg_v4_Promise_getResult(thread, promise, &result) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(result == nullptr);

    HandlerCalls calls{};
    REQUIRE(dxfg_v4_Promise_whenDone(thread, promise, &HandlerCalls::handle, &calls) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.threads.empty());

    dxfg_v4_java_object_handler *value = nullptr;
    REQUIRE(dxfg_v4_Object_new(thread, &value) == DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dxfg_v4_Promise_complete(thread, promise, value) == DXFG_EXECUTE_SUCCESSFULLY);

    // The handler ran on the completing thread before the function returned.
    REQUIRE(calls.threads.size() == 1);
    CHECK(calls.threads[0] == std::this_thread::get_id());
    CHECK(calls.hasResult == 1);
    CHECK(calls.hasException == 0);
    CHECK(isDone(thread, promise) == 1);

    REQUIRE(dxfg_v4_Promise_getResult(thread, promise, &result) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(result != nullptr);
    CHECK(result != value);

    int32_t equal = -1;
    CHECK(dxfg_v4_Object_equals(thread, value, result, &equal) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);

    // A second value has no effect: the first result wins and the handler is not called again.
    dxfg_v4_java_object_handler *other = nullptr;
    REQUIRE(dxfg_v4_Object_new(thread, &other) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Promise_complete(thread, promise, other) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.threads.size() == 1);

    dxfg_v4_java_object_handler *again = nullptr;
    REQUIRE(dxfg_v4_Promise_getResult(thread, promise, &again) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Object_equals(thread, value, again, &equal) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, again) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, other) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, result) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, value) == DXFG_EXECUTE_SUCCESSFULLY);
    release(thread, promise);
}

TEST_CASE("A promise completed with NULL has a null result") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *promise = newPromise(thread);
    REQUIRE(dxfg_v4_Promise_complete(thread, promise, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);

    int32_t hasResult = -1;
    CHECK(dxfg_v4_Promise_hasResult(thread, promise, &hasResult) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(hasResult == 1);

    dxfg_v4_java_object_handler *result = &promise->handler;
    CHECK(dxfg_v4_Promise_getResult(thread, promise, &result) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(result == nullptr);

    release(thread, promise);
}

TEST_CASE("A failed promise gives the class and the message to the handler once") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *promise = newPromise(thread);
    HandlerCalls calls{};
    REQUIRE(dxfg_v4_Promise_whenDone(thread, promise, &HandlerCalls::handle, &calls) == DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(
                    thread, promise, "java.lang.IllegalArgumentException", "bad argument") ==
            DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(calls.threads.size() == 1);
    CHECK(calls.threads[0] == std::this_thread::get_id());
    CHECK(calls.hasResult == 0);
    CHECK(calls.hasException == 1);
    CHECK(calls.exceptionClassName == "java.lang.IllegalArgumentException");
    CHECK(calls.exceptionMessage == "bad argument");

    int32_t cancelled = -1;
    CHECK(dxfg_v4_Promise_isCancelled(thread, promise, &cancelled) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(cancelled == 0);

    dxfg_v4_java_object_handler *result = &promise->handler;
    CHECK(dxfg_v4_Promise_getResult(thread, promise, &result) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(result == nullptr);

    // The wait fails: the exception of the promise is wrapped.
    CHECK(dxfg_v4_Promise_await(thread, promise) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());

    release(thread, promise);
}

TEST_CASE("Every class of the set is created as it is in the image") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    for (const char *className :
         {"java.lang.RuntimeException", "java.lang.IllegalArgumentException", "java.lang.IllegalStateException",
          "java.lang.UnsupportedOperationException", "java.io.IOException",
          "java.util.concurrent.CancellationException", "java.util.concurrent.TimeoutException"}) {
        CAPTURE(className);

        const Failure failure = failNew(thread, className, "message");

        CHECK(failure.className == className);
        CHECK(failure.message == "message");
    }

    // A class of the set keeps a NULL message.
    const Failure withoutMessage = failNew(thread, "java.lang.IllegalStateException", nullptr);
    CHECK(withoutMessage.className == "java.lang.IllegalStateException");
    CHECK(withoutMessage.message == "<null>");
}

TEST_CASE("A name outside the set gives a RuntimeException with the name in the message") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    SUBCASE("An exception class outside the set") {
        const Failure failure = failNew(thread, "java.lang.ArithmeticException", "boom");

        CHECK(failure.className == "java.lang.RuntimeException");
        CHECK(failure.message == "java.lang.ArithmeticException: boom");
    }

    SUBCASE("A class that is not an exception") {
        const Failure failure = failNew(thread, "java.lang.String", "boom");

        CHECK(failure.className == "java.lang.RuntimeException");
        CHECK(failure.message == "java.lang.String: boom");
    }

    SUBCASE("No such class, no message") {
        const Failure failure = failNew(thread, "com.example.NoSuchException", nullptr);

        CHECK(failure.className == "java.lang.RuntimeException");
        CHECK(failure.message == "com.example.NoSuchException");
    }

    SUBCASE("A NULL or empty name") {
        for (const char *className : {static_cast<const char *>(nullptr), ""}) {
            const Failure failure = failNew(thread, className, "boom");

            CHECK(failure.className == "java.lang.RuntimeException");
            CHECK(failure.message == "boom");
        }
    }

    SUBCASE("A non-ASCII message") {
        const Failure failure = failNew(thread, "java.io.IOException", "\xD0\xBE\xD1\x88\xD0\xB8\xD0\xB1\xD0\xBA\xD0\xB0");

        CHECK(failure.className == "java.io.IOException");
        CHECK(failure.message == "\xD0\xBE\xD1\x88\xD0\xB8\xD0\xB1\xD0\xBA\xD0\xB0");
    }
}

TEST_CASE("The first result wins: a value, then a failure") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *promise = newPromise(thread);
    HandlerCalls calls{};
    REQUIRE(dxfg_v4_Promise_whenDone(thread, promise, &HandlerCalls::handle, &calls) == DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dxfg_v4_Promise_complete(thread, promise, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(
                  thread, promise, "java.lang.IllegalStateException", "too late") == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(calls.threads.size() == 1);
    CHECK(calls.hasResult == 1);

    int32_t hasException = -1;
    CHECK(dxfg_v4_Promise_hasException(thread, promise, &hasException) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(hasException == 0);
    CHECK(exceptionOf(thread, promise).className.empty());

    release(thread, promise);
}

TEST_CASE("The first result wins: a failure, then a value") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *promise = newPromise(thread);
    HandlerCalls calls{};
    REQUIRE(dxfg_v4_Promise_whenDone(thread, promise, &HandlerCalls::handle, &calls) == DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(
                    thread, promise, "java.util.concurrent.TimeoutException", "first") == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_java_object_handler *value = nullptr;
    REQUIRE(dxfg_v4_Object_new(thread, &value) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Promise_complete(thread, promise, value) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(
                  thread, promise, "java.lang.IllegalStateException", "second") == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Promise_cancel(thread, promise) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(calls.threads.size() == 1);
    CHECK(calls.exceptionClassName == "java.util.concurrent.TimeoutException");
    CHECK(calls.exceptionMessage == "first");

    int32_t hasResult = -1;
    CHECK(dxfg_v4_Promise_hasResult(thread, promise, &hasResult) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(hasResult == 0);

    const Failure failure = exceptionOf(thread, promise);
    CHECK(failure.className == "java.util.concurrent.TimeoutException");
    CHECK(failure.message == "first");

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, value) == DXFG_EXECUTE_SUCCESSFULLY);
    release(thread, promise);
}

TEST_CASE("A cancelled promise ignores a later value and failure") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *promise = newPromise(thread);
    REQUIRE(dxfg_v4_Promise_cancel(thread, promise) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_Promise_complete(thread, promise, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(thread, promise, "java.io.IOException",
                                                                       "late") == DXFG_EXECUTE_SUCCESSFULLY);

    int32_t cancelled = -1;
    CHECK(dxfg_v4_Promise_isCancelled(thread, promise, &cancelled) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(cancelled == 1);
    CHECK(exceptionOf(thread, promise).className == "java.util.concurrent.CancellationException");

    release(thread, promise);
}

TEST_CASE("The static failed function gives an already failed promise") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *promise = nullptr;
    REQUIRE(dxfg_v4_Promise_failedWithClassNameAndMessage(thread, "java.lang.UnsupportedOperationException",
                                                          "not here", &promise) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(promise != nullptr);
    CHECK(isDone(thread, promise) == 1);

    // The handler of a done promise runs at once, on the calling thread.
    HandlerCalls calls{};
    REQUIRE(dxfg_v4_Promise_whenDone(thread, promise, &HandlerCalls::handle, &calls) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(calls.threads.size() == 1);
    CHECK(calls.threads[0] == std::this_thread::get_id());
    CHECK(calls.exceptionClassName == "java.lang.UnsupportedOperationException");
    CHECK(calls.exceptionMessage == "not here");

    // A second completion does nothing.
    CHECK(dxfg_v4_Promise_complete(thread, promise, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.threads.size() == 1);
    CHECK(exceptionOf(thread, promise).className == "java.lang.UnsupportedOperationException");

    release(thread, promise);

    dxfg_v4_promise_t *fallback = nullptr;
    REQUIRE(dxfg_v4_Promise_failedWithClassNameAndMessage(thread, "java.lang.Object", "boom", &fallback) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    const Failure failure = exceptionOf(thread, fallback);
    CHECK(failure.className == "java.lang.RuntimeException");
    CHECK(failure.message == "java.lang.Object: boom");
    release(thread, fallback);
}

TEST_CASE("The older failed and completeExceptionally functions keep their behaviour") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_promise_t *source = nullptr;
    REQUIRE(dxfg_v4_Promise_failedWithClassNameAndMessage(thread, "java.lang.IllegalArgumentException", "source",
                                                          &source) == DXFG_EXECUTE_SUCCESSFULLY);
    dxfg_v4_exception_t *exception = nullptr;
    REQUIRE(dxfg_v4_Promise_getException(thread, source, &exception) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(exception != nullptr);

    // A NULL exception gives a promise completed normally with a null result, not a failed one.
    dxfg_v4_promise_t *completed = nullptr;
    REQUIRE(dxfg_v4_Promise_failed(thread, source, nullptr, &completed) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(completed != nullptr);
    int32_t hasResult = -1;
    CHECK(dxfg_v4_Promise_hasResult(thread, completed, &hasResult) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(hasResult == 1);
    release(thread, completed);

    // A non-NULL exception cannot be converted: the call fails with IllegalStateException.
    dxfg_v4_promise_t *notCreated = nullptr;
    CHECK(dxfg_v4_Promise_failed(thread, source, exception, &notCreated) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(notCreated == nullptr);

    dxfg_v4_promise_t *pending = newPromise(thread);
    CHECK(dxfg_v4_Promise_completeExceptionally(thread, pending, exception) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(isDone(thread, pending) == 0);
    release(thread, pending);

    CHECK(dxfg_v4_Exception_release(thread, exception) == DXFG_EXECUTE_SUCCESSFULLY);
    release(thread, source);
}

TEST_CASE("NULL handles and NULL out pointers fail with a thread exception") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_java_object_handler *result = nullptr;

    CHECK(dxfg_v4_Promise_new(thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_Promise_complete(thread, nullptr, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_Promise_getResult(thread, nullptr, &result) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage(thread, nullptr, "java.io.IOException", "x") ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_Promise_failedWithClassNameAndMessage(thread, "java.io.IOException", "x", nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    dxfg_v4_promise_t *promise = newPromise(thread);
    CHECK(dxfg_v4_Promise_getResult(thread, promise, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(isDone(thread, promise) == 0);
    release(thread, promise);
}

} // namespace test
} // namespace dxfg
