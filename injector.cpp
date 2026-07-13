#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <dirent.h>

// دالة حقيقية للكتابة المباشرة في ذاكرة العملية باستخدام syscall الخاص بنظام لينكس
bool write_process_memory(pid_t pid, uintptr_t address, void* buffer, size_t size) {
    struct iovec local[1];
    struct iovec remote[1];

    local[0].iov_base = buffer;
    local[0].iov_len = size;
    remote[0].iov_base = (void*)address;
    remote[0].iov_len = size;

    // syscall الحقيقي للكتابة في ذاكرة عملية أخرى بشرط وجود صلاحيات الروت (PTRACE_ATTACH)
    ssize_t written = process_vm_writev(pid, local, 1, remote, 1, 0);
    return written == size;
}

int main(int argc, char* argv[]) {
    if (argc < 7) {
        std::cerr << "Usage: ./injector -p <PID> -a <Address> -v <Value>" << std::endl;
        return 1;
    }

    pid_t pid = 0;
    uintptr_t address = 0;
    int value = 0;

    for (int i = 1; i < argc; i++) {
        if (std::string(argv[i]) == "-p") pid = std::stoi(argv[++i]);
        else if (std::string(argv[i]) == "-a") address = std::stoull(argv[++i], nullptr, 16);
        else if (std::string(argv[i]) == "-v") value = std::stoi(argv[++i]);
    }

    if (pid == 0 || address == 0) {
        std::cerr << "Invalid arguments." << std::endl;
        return 1;
    }

    // حقن القيمة الحقيقية داخل العنوان المحدد في الذاكرة RAM
    if (write_process_memory(pid, address, &value, sizeof(value))) {
        std::cout << "SUCCESS: Injected value " << value << " into address 0x" << std::hex << address << std::endl;
        return 0;
    } else {
        std::cerr << "FAILED: Unable to write to process memory." << std::endl;
        return 1;
    }
}
