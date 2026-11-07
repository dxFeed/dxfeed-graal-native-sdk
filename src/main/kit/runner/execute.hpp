// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "scenario.hpp"
#include "session.hpp"

#include <string>

namespace kit {

struct Timing {
    /// The deadline of one step; a poll or a wait gets its own timeout on top of it.
    Clock::duration step;
    /// The end of the whole run.
    Clock::time_point total;
};

struct Outcome {
    enum class Kind { Pass, Fail, Error, Mismatch };

    Kind kind = Kind::Pass;
    std::string message; // "line N: ..." for a failure or an error
};

/// Runs the steps of the scenario on a started session. After Fail, Error or Mismatch the session must not be reused.
Outcome runSteps(Session &session, const Scenario &scenario, const Timing &timing);

/// The deadline of a step that starts now, capped by the end of the run.
Clock::time_point stepDeadline(const Timing &timing, Clock::duration extra = Clock::duration::zero());

} // namespace kit
