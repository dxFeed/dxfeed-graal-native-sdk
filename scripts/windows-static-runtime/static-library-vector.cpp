// C++ code of the application: its objects carry /FAILIFMISMATCH of RuntimeLibrary and _ITERATOR_DEBUG_LEVEL, which
// the static library of the SDK must not contradict for any C runtime (/MT, /MTd, /MD, /MDd).
#include <vector>

int applicationVectorSum() {
    std::vector<int> values{1, 2, 3};
    int sum = 0;
    for (int value : values) {
        sum += value;
    }
    return sum;
}
