#include <jni.h>

#include "PluginPlayer.h"

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_loadAudioResourceNative(JNIEnv *env, jobject thiz,
                                                                         jlong player,
                                                                         jbyteArray bytes,
                                                                         jstring filename) {
    jboolean isDataCopy{false};
    auto data = (uint8_t*) env->GetByteArrayElements(bytes, &isDataCopy);
    jboolean isNameCopy{true};
    auto name = env->GetStringUTFChars(filename, &isNameCopy);
    ((aap::PluginPlayer*) player)->setAudioSource(data, env->GetArrayLength(bytes), name);
    if (isNameCopy)
        free((void*) name);
    if (isDataCopy)
        free((void*) data);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_addMidiEventsNative(JNIEnv *env, jobject thiz,
                                                                     jlong player, jbyteArray bytes,
                                                                     jint offset, jint length,
                                                                     jlong timestampInNanoseconds) {
    jboolean isDataCopy{false};
    auto data = (uint8_t*) env->GetByteArrayElements(bytes, &isDataCopy);
    // timeout would not be reliable from non-RT language environment though...
    ((aap::PluginPlayer*) player)->addMidiEvents(data + offset, length, timestampInNanoseconds);
    if (isDataCopy)
        free((void*) data);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_createNewPluginPlayer(JNIEnv *env, jclass clazz,
                                                                       jint sampleRate,
                                                                       jint framesPerCallback,
                                                                       jint channelCount,
                                                                       jint outFileFd) {
    aap::PluginPlayerConfiguration configuration{sampleRate, framesPerCallback, channelCount};
    return (jlong) (void*) new aap::PluginPlayer(configuration, outFileFd);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_deletePluginPlayer(JNIEnv *env, jobject thiz,
                                                                    jlong native) {
    auto player = (aap::PluginPlayer*) native;
    player->pauseProcessing();
    delete player;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_startProcessingNative(JNIEnv *env, jobject thiz,
                                                                       jlong player) {
    ((aap::PluginPlayer*) player)->startProcessing();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_pauseProcessingNative(JNIEnv *env, jobject thiz,
                                                                       jlong player) {
    ((aap::PluginPlayer*) player)->pauseProcessing();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_playPreloadedAudioNative(JNIEnv *env, jobject thiz,
                                                                          jlong player) {
    ((aap::PluginPlayer*) player)->getGraph().playAudioData();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_addPluginNative(JNIEnv *env, jobject thiz,
                                                                 jlong player, jlong nativeClient,
                                                                 jint instanceId) {
    auto client = (aap::PluginClient*) nativeClient;
    auto instance = client->getInstanceById(instanceId);
    ((aap::PluginPlayer*) player)->getGraph().setPlugin((aap::RemotePluginInstance*) instance);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_enableAudioRecorderNative(JNIEnv *,
                                                                           jobject thiz,
                                                                           jlong player) {
    ((aap::PluginPlayer*) player)->enableAudioRecorder();
}
extern "C"
JNIEXPORT void JNICALL
Java_org_androidaudioplugin_manager_PluginPlayer_setPresetIndexNative(JNIEnv *env, jobject thiz,
                                                                      jlong player, jint pluginNumber, jint index) {
    ((aap::PluginPlayer*) player)->setPresetIndex(pluginNumber, index);
}