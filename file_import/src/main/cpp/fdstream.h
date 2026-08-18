//
// Created by leoni on 25.05.2025.
//

#ifndef RECORDINGSTUDIO_FDSTREAM_H
#define RECORDINGSTUDIO_FDSTREAM_H

#include <streambuf>
#include <ostream>
#include <memory>
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

std::shared_ptr<std::ostream> createOstreamFromFd(int fd) {
    auto buf = std::make_unique<FdStreamBuf>(fd);
    auto stream = std::make_shared<std::ostream>(buf.get());

    // Transfer ownership of buf to the stream, and delete both properly
    return std::shared_ptr<std::ostream>(
            stream.get(),
            [stream, buf = std::move(buf)](std::ostream* ptr) {
                // stream and buf will be destroyed together
            }
    );
}

#endif //RECORDINGSTUDIO_FDSTREAM_H
