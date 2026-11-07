// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "process.hpp"

#include <algorithm>
#include <cstring>

#ifdef _WIN32
#    ifndef WIN32_LEAN_AND_MEAN
#        define WIN32_LEAN_AND_MEAN
#    endif
#    ifndef NOMINMAX
#        define NOMINMAX
#    endif
#    include <windows.h>
#else
#    include <cerrno>
#    include <csignal>
#    include <fcntl.h>
#    include <spawn.h>
#    include <sys/types.h>
#    include <sys/wait.h>
#    include <unistd.h>

extern char **environ;
#endif

namespace kit {

namespace {

constexpr std::size_t stderrTailLength = 8 * 1024;

} // namespace

#ifdef _WIN32

namespace {

std::wstring widen(const std::string &text) {
    if (text.empty()) {
        return {};
    }

    int size = MultiByteToWideChar(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), nullptr, 0);
    std::wstring result(static_cast<std::size_t>(size), L'\0');
    MultiByteToWideChar(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), result.data(), size);

    return result;
}

// Quotes one argument by the rules of CommandLineToArgvW and the C runtime.
void appendQuoted(std::wstring &commandLine, const std::wstring &argument) {
    if (!argument.empty() && argument.find_first_of(L" \t\n\v\"") == std::wstring::npos) {
        commandLine += argument;

        return;
    }

    commandLine += L'"';

    for (auto it = argument.begin();; ++it) {
        std::size_t backslashes = 0;

        while (it != argument.end() && *it == L'\\') {
            ++it;
            ++backslashes;
        }

        if (it == argument.end()) {
            commandLine.append(backslashes * 2, L'\\');

            break;
        }

        if (*it == L'"') {
            commandLine.append(backslashes * 2 + 1, L'\\');
        } else {
            commandLine.append(backslashes, L'\\');
        }

        commandLine += *it;
    }

    commandLine += L'"';
}

std::string lastErrorText(const char *what) {
    return std::string(what) + " failed with the error " + std::to_string(GetLastError());
}

void closeHandle(HANDLE &handle) {
    if (handle != nullptr && handle != INVALID_HANDLE_VALUE) {
        CloseHandle(handle);
    }

    handle = nullptr;
}

} // namespace

struct Process::Native {
    HANDLE process = nullptr;
    HANDLE job = nullptr;
    HANDLE stdinWrite = nullptr;
    HANDLE stdoutRead = nullptr;
    HANDLE stderrRead = nullptr;
    DWORD processId = 0;
    std::mutex stdinMutex;
    bool reaped = false;
    int exitCode = 0;

    ~Native() {
        closeHandle(stdinWrite);
        closeHandle(stdoutRead);
        closeHandle(stderrRead);
        closeHandle(process);
        closeHandle(job);
    }
};

std::unique_ptr<Process> Process::spawn(const std::vector<std::string> &argv, std::string &error) {
    if (argv.empty()) {
        error = "no program to start";

        return nullptr;
    }

    std::unique_ptr<Process> result(new Process());
    Native &native = *result->native_;

    HANDLE stdinRead = nullptr;
    HANDLE stdoutWrite = nullptr;
    HANDLE stderrWrite = nullptr;
    auto closeChildEnds = [&] {
        closeHandle(stdinRead);
        closeHandle(stdoutWrite);
        closeHandle(stderrWrite);
    };

    // The pipes are created non-inheritable; only the child's ends are made inheritable, and the handle list below
    // keeps the child from inheriting any other handle of the runner.
    constexpr DWORD pipeBuffer = 64 * 1024;

    if (!CreatePipe(&stdinRead, &native.stdinWrite, nullptr, pipeBuffer) ||
        !CreatePipe(&native.stdoutRead, &stdoutWrite, nullptr, pipeBuffer) ||
        !CreatePipe(&native.stderrRead, &stderrWrite, nullptr, pipeBuffer)) {
        error = lastErrorText("CreatePipe");
        closeChildEnds();

        return nullptr;
    }

    HANDLE inherited[] = {stdinRead, stdoutWrite, stderrWrite};

    for (HANDLE handle : inherited) {
        if (!SetHandleInformation(handle, HANDLE_FLAG_INHERIT, HANDLE_FLAG_INHERIT)) {
            error = lastErrorText("SetHandleInformation");
            closeChildEnds();

            return nullptr;
        }
    }

    SIZE_T attributeListSize = 0;
    InitializeProcThreadAttributeList(nullptr, 1, 0, &attributeListSize);
    std::vector<unsigned char> attributeListBuffer(attributeListSize);
    auto attributeList = reinterpret_cast<LPPROC_THREAD_ATTRIBUTE_LIST>(attributeListBuffer.data());

    if (!InitializeProcThreadAttributeList(attributeList, 1, 0, &attributeListSize)) {
        error = lastErrorText("InitializeProcThreadAttributeList");
        closeChildEnds();

        return nullptr;
    }

    if (!UpdateProcThreadAttribute(attributeList, 0, PROC_THREAD_ATTRIBUTE_HANDLE_LIST, inherited, sizeof(inherited),
                                   nullptr, nullptr)) {
        error = lastErrorText("UpdateProcThreadAttribute");
        DeleteProcThreadAttributeList(attributeList);
        closeChildEnds();

        return nullptr;
    }

    STARTUPINFOEXW startupInfo{};
    startupInfo.StartupInfo.cb = sizeof(startupInfo);
    startupInfo.StartupInfo.dwFlags = STARTF_USESTDHANDLES;
    startupInfo.StartupInfo.hStdInput = stdinRead;
    startupInfo.StartupInfo.hStdOutput = stdoutWrite;
    startupInfo.StartupInfo.hStdError = stderrWrite;
    startupInfo.lpAttributeList = attributeList;

    std::wstring commandLine;

    for (const auto &argument : argv) {
        if (!commandLine.empty()) {
            commandLine += L' ';
        }

        appendQuoted(commandLine, widen(argument));
    }

    PROCESS_INFORMATION processInfo{};

    // Suspended, so that the child is in the job before it can start a child of its own.
    BOOL created = CreateProcessW(nullptr, commandLine.data(), nullptr, nullptr, TRUE,
                                  CREATE_SUSPENDED | CREATE_NO_WINDOW | EXTENDED_STARTUPINFO_PRESENT, nullptr, nullptr,
                                  &startupInfo.StartupInfo, &processInfo);
    DWORD createError = GetLastError();

    DeleteProcThreadAttributeList(attributeList);
    closeChildEnds();

    if (!created) {
        error = "cannot start " + argv[0] + ": CreateProcessW failed with the error " + std::to_string(createError);

        return nullptr;
    }

    native.process = processInfo.hProcess;
    native.processId = processInfo.dwProcessId;

    native.job = CreateJobObjectW(nullptr, nullptr);
    JOBOBJECT_EXTENDED_LIMIT_INFORMATION limits{};
    limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;

    if (native.job == nullptr ||
        !SetInformationJobObject(native.job, JobObjectExtendedLimitInformation, &limits, sizeof(limits)) ||
        !AssignProcessToJobObject(native.job, native.process)) {
        error = lastErrorText("the job object setup");
        TerminateProcess(native.process, 1);
        WaitForSingleObject(native.process, INFINITE);
        native.reaped = true;
        CloseHandle(processInfo.hThread);

        return nullptr;
    }

    ResumeThread(processInfo.hThread);
    CloseHandle(processInfo.hThread);

    result->startReaders();

    return result;
}

bool Process::writeLine(const std::string &line) {
    std::lock_guard<std::mutex> lock(native_->stdinMutex);

    if (native_->stdinWrite == nullptr) {
        return false;
    }

    std::string data = line + '\n';
    const char *position = data.data();
    std::size_t left = data.size();

    while (left > 0) {
        DWORD written = 0;

        if (!WriteFile(native_->stdinWrite, position, static_cast<DWORD>(left), &written, nullptr)) {
            return false;
        }

        position += written;
        left -= written;
    }

    return true;
}

void Process::closeStdin() {
    std::lock_guard<std::mutex> lock(native_->stdinMutex);
    closeHandle(native_->stdinWrite);
}

std::optional<int> Process::waitExit(Clock::time_point deadline) {
    if (native_->reaped) {
        return native_->exitCode;
    }

    auto now = Clock::now();
    auto left = deadline > now ? std::chrono::duration_cast<std::chrono::milliseconds>(deadline - now).count() : 0;

    if (WaitForSingleObject(native_->process, static_cast<DWORD>(std::min<long long>(left, 0x7fffffff))) !=
        WAIT_OBJECT_0) {
        return std::nullopt;
    }

    DWORD code = 0;
    GetExitCodeProcess(native_->process, &code);
    native_->reaped = true;
    native_->exitCode = static_cast<int>(code);

    return native_->exitCode;
}

void Process::kill() {
    if (native_->reaped) {
        return;
    }

    // Ends the child and everything it started in the job.
    TerminateJobObject(native_->job, 1);
    WaitForSingleObject(native_->process, INFINITE);

    DWORD code = 0;
    GetExitCodeProcess(native_->process, &code);
    native_->reaped = true;
    native_->exitCode = static_cast<int>(code);
}

std::int64_t Process::pid() const {
    return native_->processId;
}

void Process::readStdout() {
    std::string pending;
    char buffer[16 * 1024];

    for (;;) {
        DWORD read = 0;

        if (!ReadFile(native_->stdoutRead, buffer, sizeof(buffer), &read, nullptr) || read == 0) {
            break;
        }

        pending.append(buffer, read);

        std::size_t start = 0;
        std::size_t end;
        std::lock_guard<std::mutex> lock(mutex_);

        while ((end = pending.find('\n', start)) != std::string::npos) {
            std::size_t length = end - start;

            if (length > 0 && pending[end - 1] == '\r') {
                --length;
            }

            lines_.emplace_back(pending, start, length);
            start = end + 1;
        }

        pending.erase(0, start);

        if (pending.size() > maxLineLength) {
            lineTooLong_ = true;
            pending.clear();
        }

        linesChanged_.notify_all();
    }

    std::lock_guard<std::mutex> lock(mutex_);
    stdoutEnded_ = true;
    linesChanged_.notify_all();
}

void Process::readStderr() {
    char buffer[16 * 1024];

    for (;;) {
        DWORD read = 0;

        if (!ReadFile(native_->stderrRead, buffer, sizeof(buffer), &read, nullptr) || read == 0) {
            break;
        }

        std::lock_guard<std::mutex> lock(mutex_);
        stderrTail_.append(buffer, read);

        if (stderrTail_.size() > stderrTailLength) {
            stderrTail_.erase(0, stderrTail_.size() - stderrTailLength);
        }
    }
}

#else // POSIX

namespace {

std::string errnoText(const char *what, int error) {
    return std::string(what) + " failed: " + std::strerror(error);
}

void closeFd(int &fd) {
    if (fd >= 0) {
        ::close(fd);
    }

    fd = -1;
}

// A pipe whose both ends are closed on exec: the child gets only the ends that posix_spawn duplicates onto 0, 1, 2.
bool makePipe(int fds[2]) {
    if (::pipe(fds) != 0) {
        return false;
    }

    ::fcntl(fds[0], F_SETFD, FD_CLOEXEC);
    ::fcntl(fds[1], F_SETFD, FD_CLOEXEC);

    return true;
}

} // namespace

struct Process::Native {
    pid_t processId = -1;
    int stdinWrite = -1;
    int stdoutRead = -1;
    int stderrRead = -1;
    std::mutex stdinMutex;
    bool reaped = false;
    int exitCode = 0;

    ~Native() {
        closeFd(stdinWrite);
        closeFd(stdoutRead);
        closeFd(stderrRead);
    }

    void recordStatus(int status) {
        reaped = true;

        if (WIFEXITED(status)) {
            exitCode = WEXITSTATUS(status);
        } else if (WIFSIGNALED(status)) {
            exitCode = 128 + WTERMSIG(status);
        } else {
            exitCode = -1;
        }
    }
};

std::unique_ptr<Process> Process::spawn(const std::vector<std::string> &argv, std::string &error) {
    if (argv.empty()) {
        error = "no program to start";

        return nullptr;
    }

    // A write to the stdin of a child that exited must fail with EPIPE instead of killing the runner.
    std::signal(SIGPIPE, SIG_IGN);

    std::unique_ptr<Process> result(new Process());
    Native &native = *result->native_;

    int stdinPipe[2] = {-1, -1};
    int stdoutPipe[2] = {-1, -1};
    int stderrPipe[2] = {-1, -1};

    if (!makePipe(stdinPipe) || !makePipe(stdoutPipe) || !makePipe(stderrPipe)) {
        error = errnoText("pipe", errno);

        for (int *fd : {&stdinPipe[0], &stdinPipe[1], &stdoutPipe[0], &stdoutPipe[1], &stderrPipe[0], &stderrPipe[1]}) {
            closeFd(*fd);
        }

        return nullptr;
    }

    native.stdinWrite = stdinPipe[1];
    native.stdoutRead = stdoutPipe[0];
    native.stderrRead = stderrPipe[0];

    posix_spawn_file_actions_t actions;
    posix_spawn_file_actions_init(&actions);
    posix_spawn_file_actions_adddup2(&actions, stdinPipe[0], 0);
    posix_spawn_file_actions_adddup2(&actions, stdoutPipe[1], 1);
    posix_spawn_file_actions_adddup2(&actions, stderrPipe[1], 2);

    // A process group of its own, so that a kill also ends the children of the child; the default action of SIGPIPE
    // and an empty signal mask, since an ignored signal stays ignored across exec.
    posix_spawnattr_t attributes;
    posix_spawnattr_init(&attributes);
    posix_spawnattr_setflags(&attributes, POSIX_SPAWN_SETPGROUP | POSIX_SPAWN_SETSIGDEF | POSIX_SPAWN_SETSIGMASK);
    posix_spawnattr_setpgroup(&attributes, 0);

    sigset_t defaults;
    sigemptyset(&defaults);
    sigaddset(&defaults, SIGPIPE);
    posix_spawnattr_setsigdefault(&attributes, &defaults);

    sigset_t mask;
    sigemptyset(&mask);
    posix_spawnattr_setsigmask(&attributes, &mask);

    std::vector<char *> arguments;

    for (const auto &argument : argv) {
        arguments.push_back(const_cast<char *>(argument.c_str()));
    }

    arguments.push_back(nullptr);

    int spawnError = posix_spawnp(&native.processId, arguments[0], &actions, &attributes, arguments.data(), environ);

    posix_spawn_file_actions_destroy(&actions);
    posix_spawnattr_destroy(&attributes);
    closeFd(stdinPipe[0]);
    closeFd(stdoutPipe[1]);
    closeFd(stderrPipe[1]);

    if (spawnError != 0) {
        error = "cannot start " + argv[0] + ": " + std::strerror(spawnError);
        native.reaped = true;

        return nullptr;
    }

    result->startReaders();

    return result;
}

bool Process::writeLine(const std::string &line) {
    std::lock_guard<std::mutex> lock(native_->stdinMutex);

    if (native_->stdinWrite < 0) {
        return false;
    }

    std::string data = line + '\n';
    const char *position = data.data();
    std::size_t left = data.size();

    while (left > 0) {
        ssize_t written = ::write(native_->stdinWrite, position, left);

        if (written < 0) {
            if (errno == EINTR) {
                continue;
            }

            return false;
        }

        position += written;
        left -= static_cast<std::size_t>(written);
    }

    return true;
}

void Process::closeStdin() {
    std::lock_guard<std::mutex> lock(native_->stdinMutex);
    closeFd(native_->stdinWrite);
}

std::optional<int> Process::waitExit(Clock::time_point deadline) {
    for (;;) {
        if (native_->reaped) {
            return native_->exitCode;
        }

        int status = 0;
        pid_t waited = ::waitpid(native_->processId, &status, WNOHANG);

        if (waited == native_->processId) {
            native_->recordStatus(status);

            continue;
        }

        if (waited < 0 && errno != EINTR) {
            native_->reaped = true;
            native_->exitCode = -1;

            continue;
        }

        if (Clock::now() >= deadline) {
            return std::nullopt;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
}

void Process::kill() {
    if (native_->reaped) {
        return;
    }

    ::kill(-native_->processId, SIGKILL);
    ::kill(native_->processId, SIGKILL);

    int status = 0;
    pid_t waited;

    do {
        waited = ::waitpid(native_->processId, &status, 0);
    } while (waited < 0 && errno == EINTR);

    if (waited == native_->processId) {
        native_->recordStatus(status);
    } else {
        native_->reaped = true;
        native_->exitCode = -1;
    }
}

std::int64_t Process::pid() const {
    return native_->processId;
}

void Process::readStdout() {
    std::string pending;
    char buffer[16 * 1024];

    for (;;) {
        ssize_t read = ::read(native_->stdoutRead, buffer, sizeof(buffer));

        if (read < 0 && errno == EINTR) {
            continue;
        }

        if (read <= 0) {
            break;
        }

        pending.append(buffer, static_cast<std::size_t>(read));

        std::size_t start = 0;
        std::size_t end;
        std::lock_guard<std::mutex> lock(mutex_);

        while ((end = pending.find('\n', start)) != std::string::npos) {
            std::size_t length = end - start;

            if (length > 0 && pending[end - 1] == '\r') {
                --length;
            }

            lines_.emplace_back(pending, start, length);
            start = end + 1;
        }

        pending.erase(0, start);

        if (pending.size() > maxLineLength) {
            lineTooLong_ = true;
            pending.clear();
        }

        linesChanged_.notify_all();
    }

    std::lock_guard<std::mutex> lock(mutex_);
    stdoutEnded_ = true;
    linesChanged_.notify_all();
}

void Process::readStderr() {
    char buffer[16 * 1024];

    for (;;) {
        ssize_t read = ::read(native_->stderrRead, buffer, sizeof(buffer));

        if (read < 0 && errno == EINTR) {
            continue;
        }

        if (read <= 0) {
            break;
        }

        std::lock_guard<std::mutex> lock(mutex_);
        stderrTail_.append(buffer, static_cast<std::size_t>(read));

        if (stderrTail_.size() > stderrTailLength) {
            stderrTail_.erase(0, stderrTail_.size() - stderrTailLength);
        }
    }
}

#endif

Process::Process() : native_(std::make_unique<Native>()) {
}

Process::~Process() {
    kill();
    closeStdin();

    if (stdoutReader_.joinable()) {
        stdoutReader_.join();
    }

    if (stderrReader_.joinable()) {
        stderrReader_.join();
    }
}

void Process::startReaders() {
    stdoutReader_ = std::thread([this] { readStdout(); });
    stderrReader_ = std::thread([this] { readStderr(); });
}

Process::ReadStatus Process::readLine(std::string &line, Clock::time_point deadline) {
    std::unique_lock<std::mutex> lock(mutex_);

    if (!linesChanged_.wait_until(lock, deadline, [this] { return !lines_.empty() || stdoutEnded_ || lineTooLong_; })) {
        return ReadStatus::Timeout;
    }

    if (!lines_.empty()) {
        line = std::move(lines_.front());
        lines_.pop_front();

        return ReadStatus::Line;
    }

    if (lineTooLong_) {
        return ReadStatus::TooLong;
    }

    return ReadStatus::Eof;
}

std::string Process::stderrTail() const {
    std::lock_guard<std::mutex> lock(mutex_);

    return stderrTail_;
}

} // namespace kit
