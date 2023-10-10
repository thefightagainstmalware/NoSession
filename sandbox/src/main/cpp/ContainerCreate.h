BOOL IsInAppContainer();
BOOL RunExecutableInContainer(LPWSTR command_line, LPWSTR* rwMounts, LPWSTR* roMounts, int rwMountsCount, int roMountsCount);