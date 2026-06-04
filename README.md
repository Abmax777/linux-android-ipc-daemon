# linux-android-ipc-daemon
# Linux-to-Android IPC Daemon

A native C++ telemetry daemon running on Linux (WSL2) that streams real-time vehicle data to an Android Automotive OS (AAOS) emulator over a TCP socket, with a built-in benchmarking harness.

<img width="962" height="508" alt="image" src="https://github.com/user-attachments/assets/8686545b-b771-42db-82fb-f0413422e5dd" />

## Key Concepts Demonstrated

- **Producer-Consumer threading** — telemetry generation and network sending are decoupled into separate POSIX threads, connected via a mutex-protected queue with condition variable signalling
- **Struct serialization** — `__attribute__((packed))` used to eliminate compiler padding and ensure consistent 24-byte message size across C++ and Kotlin/JVM
- **RTT benchmarking** — round-trip latency measured in microseconds with Min/Avg/Max/P50/P95/P99 percentiles over 100-sample windows
- **ADB reverse tunnelling** — `adb reverse tcp:9000 tcp:9000` routes Android's loopback port to the WSL2 host, avoiding fragile IP-based routing across virtual network namespaces
- **WSL2 mirrored networking** — configured via `.wslconfig` to share the Windows host's loopback identity, making the daemon reachable on `127.0.0.1` from Windows

## Benchmark Results (sample run)

| Metric | Value |
|--------|-------|
| Min RTT | 2003 µs |
| Avg RTT | 14852 µs |
| P50 RTT | 3268 µs |
| P95 RTT | 56492 µs |
| P99 RTT | 317027 µs |
| Est. 1-way | 7426 µs |
| Throughput | 10 msg/sec |

P99 outliers are attributable to Android JVM garbage collection pauses on the receiver thread. In a production AOSP deployment, the receive path would be handled natively via JNI to eliminate GC jitter.

## Project Structure
<img width="1083" height="484" alt="image" src="https://github.com/user-attachments/assets/6507a2fe-a367-48fb-9aa9-c0ec680464f2" />

## Setup & Run

### Prerequisites
- WSL2 with Ubuntu and g++ installed
- Android Studio with AAOS emulator (API 35, Automotive 1408p landscape)
- ADB in system PATH

### Steps

**1. Enable WSL2 mirrored networking** — add to `C:\Users\<you>\.wslconfig`:
[wsl2]
networkingMode=mirrored
Then restart WSL: `wsl --shutdown`

**2. Set up ADB reverse tunnel** (run once per emulator session):
```powershell
adb reverse tcp:9000 tcp:9000
```

**3. Build and run the daemon** (WSL2/Ubuntu):
```bash
g++ -o daemon daemon.cpp -std=c++17 -lpthread
./daemon
```

**4. Launch the Android app** from Android Studio targeting the AAOS emulator.

## Why This Project


Built to demonstrate end-to-end Linux-to-Android native IPC — the same communication pattern used in Android Automotive OS platform development for streaming vehicle sensor data from native HAL layers into Android framework services.
