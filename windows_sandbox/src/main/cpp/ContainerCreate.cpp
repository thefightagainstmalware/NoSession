#include <windows.h>
#include <strsafe.h>
#include <userenv.h>
#include <accctrl.h>
#include <aclapi.h>
#include <cstdint>
#include <cassert>
#include "ContainerCreate.h"
#include <algorithm>

#pragma comment(lib, "userenv.lib")
#pragma comment(lib, "Advapi32.lib")

WELL_KNOWN_SID_TYPE app_capabilities[] = {
        WinCapabilityInternetClientSid,
        WinCapabilityInternetClientServerSid, // allow both connection and binding to ports
        /**
         * binding to ports is allowed even though it might present a risk because it is useful for ipc, and additionally
         * blocking binding to ports presents no security benefit because reverse shells-type connections still work,
         * additionally it wouldn't help prevent c2s from causing problems on machines because c2s would not be able to
         * do much because it is sandboxed
         */
        WinCapabilityPrivateNetworkClientServerSid, // this is needed when the user is connected to a VPN which
                                                    // routes traffic through a local ip address.
        // TODO: investigate removing this capability conditionally

};
WCHAR container_desc[] = L"Sandboxing Minecraft";


BOOL SetSecurityCapabilities(PSID container_sid, SECURITY_CAPABILITIES *capabilities, PDWORD num_capabilities);

BOOL GrantNamedObjectAccess(PSID appcontainer_sid, LPWSTR object_name, SE_OBJECT_TYPE object_type, DWORD access_mask);

/**
 * Uses FNV-1 hash in order to generate a unique container name
 * use of FNV-1, a non-cryptographic hash is because the mounts which go into the hash are not user supplied, they
 * are supplied by the implementor of the sandbox. FNV-1 was chosen because of its low collision rate, which makes it more
 * likely that an existing container name has the same rwMounts and roMounts reducing the attack surface if the container
 * names collide
 * @param base_container_name name of the container to prepend
 * @param rwMounts read-write mounts allowed
 * @param roMounts read-only mounts allowed
 * @param base_container_name_length length of the base_container_name string
 * @param rwMountsCount count of read-write mounts
 * @param roMountsCount count of read-only mounts
 * @return a container name that is sufficiently random. the caller is expected to free this buffer
 */
WCHAR* createContainerName(WCHAR *base_container_name, LPWSTR *rwMounts, LPWSTR *roMounts, int8_t base_container_name_length, int rwMountsCount,
                           int roMountsCount) {
    assert(base_container_name_length < 47); // 47 + 16 = 63, one less than the max for base_container_name
    uint64_t hash = 0xcbf29ce484222325;
    for (int i = 0; i < rwMountsCount; ++i) {
        size_t strlen = wcslen(rwMounts[i]);
        for (size_t j = 0; j < strlen; j++) {
            hash *= 0x100000001b3;
            hash ^= rwMounts[i][j];
        }
    }
    for (int i = 0; i < roMountsCount; ++i) {
        size_t strlen = wcslen(roMounts[i]);
        for (size_t j = 0; j < strlen; j++) {
            hash *= 0x100000001b3;
            hash ^= roMounts[i][j];
        }
    }
    auto result = (WCHAR*) malloc(sizeof(WCHAR) * (base_container_name_length + 16 + 1));
    static_assert(sizeof(size_t) >= sizeof(int8_t)); // should be true on most systems but doesn't hurt to be safe
    ZeroMemory(&result[0], (size_t) base_container_name_length + 16 + 1);
    std::copy(&base_container_name[0], &base_container_name[base_container_name_length], &result[0]);

    _snwprintf_s(&result[base_container_name_length], 17, 17, L"%jx", hash);

    return result;
}

/*
    Create a container with container_name and run the specified application inside it
*/
BOOL RunExecutableInContainer(LPWSTR command_line, LPWSTR *rwMounts, LPWSTR *roMounts, int rwMountsCount,
                              int roMountsCount) {
    PSID sid = nullptr;
    HRESULT result;
    SECURITY_CAPABILITIES SecurityCapabilities{};
    DWORD num_capabilities = 1;
    SIZE_T attribute_size = 0;
    STARTUPINFOEXW startup_info;
    PROCESS_INFORMATION process_info;
    ZeroMemory(&startup_info, sizeof(startup_info));
    ZeroMemory(&process_info, sizeof(process_info));
    startup_info.StartupInfo.cb = sizeof(STARTUPINFOEXW);

    BOOL success = false;
    WCHAR container_name_base[] = L"MinecraftSandbox";
    WCHAR* container_name = createContainerName(container_name_base, rwMounts, roMounts, 16, rwMountsCount, roMountsCount);
    do //Not a loop
    {
        result = CreateAppContainerProfile(container_name, L"Minecraft Sandbox", container_desc, nullptr, 0, &sid);
        if (!SUCCEEDED(result)) {
            if (HRESULT_CODE(result) == ERROR_ALREADY_EXISTS) {
                result = DeriveAppContainerSidFromAppContainerName(container_name, &sid);
                if (!SUCCEEDED(result)) {
                    printf("Failed to get existing AppContainer name, error code: %ld", HRESULT_CODE(result));
                    break;
                }
            } else {
                printf("Failed to create AppContainer, last error: %ld\n", HRESULT_CODE(result));
                break;
            }
        }

        if (!SetSecurityCapabilities(sid, &SecurityCapabilities, &num_capabilities)) {
            printf("Failed to set security capabilities, last error: %ld\n", GetLastError());
            break;
        }

        for (int i = 0; i < rwMountsCount; i++) {
            GrantNamedObjectAccess(sid, rwMounts[i], SE_FILE_OBJECT, FILE_ALL_ACCESS | FILE_LIST_DIRECTORY);
        }

        for (int i = 0; i < roMountsCount; i++) {
            GrantNamedObjectAccess(sid, roMounts[i], SE_FILE_OBJECT, GENERIC_READ);
        }

        InitializeProcThreadAttributeList(nullptr, 1, 0, &attribute_size);
        startup_info.lpAttributeList = (LPPROC_THREAD_ATTRIBUTE_LIST) malloc(attribute_size);

        if (!InitializeProcThreadAttributeList(startup_info.lpAttributeList, 1, 0, &attribute_size)) {
            printf("InitializeProcThreadAttributeList() failed, last error: %ld", GetLastError());
            break;
        }

        if (!UpdateProcThreadAttribute(startup_info.lpAttributeList, 0, PROC_THREAD_ATTRIBUTE_SECURITY_CAPABILITIES,
                                       &SecurityCapabilities, sizeof(SecurityCapabilities), nullptr, nullptr)) {
            printf("UpdateProcThreadAttribute() failed, last error: %ld", GetLastError());
            break;
        }


        if (!CreateProcessW(nullptr, command_line, nullptr, nullptr, FALSE,
                            EXTENDED_STARTUPINFO_PRESENT, nullptr, nullptr,
                            (LPSTARTUPINFOW) &startup_info, &process_info)) {
            printf("Failed to create process %ls, last error: %ld\n", command_line, GetLastError());
            break;
        }
        success = true;
    } while (FALSE);

    if (startup_info.lpAttributeList)
        DeleteProcThreadAttributeList(startup_info.lpAttributeList);

    if (SecurityCapabilities.Capabilities)
        free(SecurityCapabilities.Capabilities);

    if (sid)
        FreeSid(sid);

    if (container_name)
        free(container_name);

    return success;
}

/*
    Check if the current process is running inside an AppContainer
*/
BOOL IsInAppContainer() {
    HANDLE process_token;
    BOOL is_container = 0;
    DWORD return_length;

    OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &process_token);

    if (!GetTokenInformation(process_token, TokenIsAppContainer, &is_container, sizeof(is_container), &return_length))
        return false;

    return is_container;
}

/*
    Set the security capabilities of the container to those listed in app_capabilities
*/
BOOL SetSecurityCapabilities(PSID container_sid, SECURITY_CAPABILITIES *capabilities, PDWORD num_capabilities) {

    DWORD sid_size = SECURITY_MAX_SID_SIZE;
    DWORD num_capabilities_ = sizeof(app_capabilities) / sizeof(DWORD);
    SID_AND_ATTRIBUTES *attributes;
    BOOL success = TRUE;

    attributes = (SID_AND_ATTRIBUTES *) malloc(sizeof(SID_AND_ATTRIBUTES) * num_capabilities_);

    ZeroMemory(capabilities, sizeof(SECURITY_CAPABILITIES));
    ZeroMemory(attributes, sizeof(SID_AND_ATTRIBUTES) * num_capabilities_);

    for (unsigned int i = 0; i < num_capabilities_; i++) {
        attributes[i].Sid = malloc(SECURITY_MAX_SID_SIZE);
        if (!CreateWellKnownSid(app_capabilities[i], nullptr, attributes[i].Sid, &sid_size)) {
            success = FALSE;
            break;
        }
        attributes[i].Attributes = SE_GROUP_ENABLED;
    }

    if (success == FALSE) {
        for (unsigned int i = 0; i < num_capabilities_; i++) {
            if (attributes[i].Sid)
                LocalFree(attributes[i].Sid);
        }

        free(attributes);
        attributes = nullptr;
        num_capabilities_ = 0;
    }

    capabilities->Capabilities = attributes;
    capabilities->CapabilityCount = num_capabilities_;
    capabilities->AppContainerSid = container_sid;
    *num_capabilities = num_capabilities_;

    return success;
}

BOOL GrantNamedObjectAccess(PSID appcontainer_sid, LPWSTR object_name, SE_OBJECT_TYPE object_type, DWORD access_mask) {
    EXPLICIT_ACCESS_W explicit_access;
    PACL original_acl = nullptr, new_acl = nullptr;
    DWORD status;
    BOOL success = FALSE;

    do {
        explicit_access.grfAccessMode = GRANT_ACCESS;
        explicit_access.grfAccessPermissions = access_mask;
        explicit_access.grfInheritance = OBJECT_INHERIT_ACE | CONTAINER_INHERIT_ACE;

        explicit_access.Trustee.MultipleTrusteeOperation = NO_MULTIPLE_TRUSTEE;
        explicit_access.Trustee.pMultipleTrustee = nullptr;
        explicit_access.Trustee.ptstrName = static_cast<LPWCH>(appcontainer_sid);
        explicit_access.Trustee.TrusteeForm = TRUSTEE_IS_SID;
        explicit_access.Trustee.TrusteeType = TRUSTEE_IS_WELL_KNOWN_GROUP;

        status = GetNamedSecurityInfoW(object_name, object_type, DACL_SECURITY_INFORMATION, nullptr, nullptr,
                                       &original_acl,
                                       nullptr, nullptr);
        if (status != ERROR_SUCCESS) {
            printf("GetNamedSecurityInfoW() failed for %ls, error: %ld\n", object_name, status);
            break;
        }

        status = SetEntriesInAclW(1, &explicit_access, original_acl, &new_acl);
        if (status != ERROR_SUCCESS) {
            printf("SetEntriesInAclW() failed for %ls, error: %ld\n", object_name, status);
            break;
        }

        status = SetNamedSecurityInfoW(object_name, object_type, DACL_SECURITY_INFORMATION, nullptr, nullptr,
                                           new_acl, nullptr);
        if (status != ERROR_SUCCESS) {
            printf("SetNamedSecurityInfoW() failed for %ls, error: %ld\n", object_name, status);
            break;
        }

        success = TRUE;

    } while (FALSE);

    if (original_acl)
        LocalFree(original_acl);

    if (new_acl)
        LocalFree(new_acl);

    return success;
}