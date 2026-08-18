#include <unistd.h>
#include <jni.h>
#include "fdstream.h"

bool copyFile(int inputFd, int outputFd) {
    auto out = createOstreamFromFd(outputFd);

    const size_t bufferSize = 8192;
    char buffer[bufferSize];
    ssize_t bytesRead;



    while ((bytesRead = read(inputFd, buffer, bufferSize)) > 0) {
        out->write(buffer, bytesRead);
        //ssize_t bytesWritten = 0;
        //while (bytesWritten < bytesRead) {
        //    ssize_t result = write(outputFd, buffer + bytesWritten, bytesRead - bytesWritten);
        //    if (result < 0) return false;
        //    bytesWritten += result;
        //}
    }
    return true; //bytesRead == 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_leonidius20_recorder_data_import_1file_NativeBridge_copyFile(JNIEnv* env, jobject thiz, jint inputFd, jint outputFd) {
    return copyFile(inputFd, outputFd) ? JNI_TRUE : JNI_FALSE;
}

extern "C" void dummy() {}


