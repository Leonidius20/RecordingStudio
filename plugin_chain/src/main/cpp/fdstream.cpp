#include "fdstream.h"
#include <memory>

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
