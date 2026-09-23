//
// Created by leoni on 25.05.2025.
//

#ifndef AAP_CORE_FDSTREAM_H
#define AAP_CORE_FDSTREAM_H

#include <streambuf>
#include <ostream>
#include <unistd.h>
#include <vector>

class FdStreamBuf : public std::streambuf {
public:
    explicit FdStreamBuf(int fd, size_t bufferSize = 4096)
            : fd_(fd), buffer_(bufferSize) {
        setp(buffer_.data(), buffer_.data() + buffer_.size() - 1);
    }

    ~FdStreamBuf() override {
        sync(); // flush on destruction
    }

protected:
    int_type overflow(int_type ch) override {
        if (ch != traits_type::eof()) {
            *pptr() = ch;
            pbump(1);
            if (flushBuffer() == -1)
                return traits_type::eof();
        }
        return ch;
    }

    int sync() override {
        return flushBuffer() == -1 ? -1 : 0;
    }

private:
    int flushBuffer() {
        int bytes = static_cast<int>(pptr() - pbase());
        if (write(fd_, pbase(), bytes) != bytes) {
            return -1;
        }
        pbump(-bytes); // reset buffer
        return 0;
    }

    int fd_;
    std::vector<char> buffer_;
};

std::shared_ptr<std::ostream> createOstreamFromFd(int fd);

#endif //AAP_CORE_FDSTREAM_H
