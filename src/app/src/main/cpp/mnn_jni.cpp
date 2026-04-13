#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <functional>
#include <atomic>
#include <android/log.h>

#define LOG_TAG "MnnJni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declaration — MNN LLM interface
// The actual MNN header should be included when linking against the MNN library
namespace MNN {
namespace Transformer {
class Llm;
struct LlmContext;
}
}

// PromptItem type matching the Kotlin data class
struct PromptItem {
    std::string role;
    std::string content;
};

// Session state
struct SessionState {
    MNN::Transformer::Llm* llm = nullptr;
    std::vector<PromptItem> history;
    std::atomic<bool> stop_requested{false};
    bool generate_text_end = false;
    std::stringstream response_buffer;
    jobject java_session_ref = nullptr; // Global ref to Java MnnSession
    JavaVM* jvm = nullptr;
};

static SessionState* g_session = nullptr;

// Helper: get JNI env for the current thread
JNIEnv* getJniEnv(JavaVM* jvm) {
    JNIEnv* env = nullptr;
    if (jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        return env;
    }
    if (jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
        return env;
    }
    return nullptr;
}

// Helper: call back to Java onTokenFromNative
void deliverTokenToJava(SessionState* state, const std::string& token) {
    if (!state || !state->java_session_ref || !state->jvm) return;

    JNIEnv* env = getJniEnv(state->jvm);
    if (!env) return;

    jobject session = state->java_session_ref;
    jclass clazz = env->GetObjectClass(session);
    if (!clazz) return;

    jmethodID method = env->GetMethodID(clazz, "onTokenFromNative", "(Ljava/lang/String;)V");
    if (!method) return;

    jstring jToken = env->NewStringUTF(token.c_str());
    env->CallVoidMethod(session, method, jToken);
    env->DeleteLocalRef(jToken);
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_tiggy_gemma4mnn_engine_MnnSession_loadModel(
        JNIEnv* env, jobject thiz, jstring configPath) {
    if (g_session != nullptr) {
        LOGD("Releasing existing session before loading new model");
        // Clean up old session
        if (g_session->llm) {
            delete g_session->llm;
        }
        if (g_session->java_session_ref) {
            env->DeleteGlobalRef(g_session->java_session_ref);
        }
        delete g_session;
        g_session = nullptr;
    }

    const char* path = env->GetStringUTFChars(configPath, nullptr);
    LOGD("loadModel: %s", path);

    g_session = new SessionState();
    env->GetJavaVM(&g_session->jvm);
    g_session->java_session_ref = env->NewGlobalRef(thiz);

    // TODO: Replace with actual MNN LLM creation when MNN library is linked
    // g_session->llm = MNN::Transformer::Llm::createLLM(path);
    // if (g_session->llm) {
    //     g_session->llm->load();
    // }

    env->ReleaseStringUTFChars(configPath, path);

    // Stub: return true for now, will be real once MNN is linked
    return g_session != nullptr ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_tiggy_gemma4mnn_engine_MnnSession_generate(
        JNIEnv* env, jobject thiz, jstring prompt, jobjectArray historyArray) {
    if (!g_session || !g_session->llm) {
        LOGE("No loaded model");
        return 0;
    }

    g_session->stop_requested = false;
    g_session->generate_text_end = false;
    g_session->response_buffer.str("");

    // Read history from Java
    jsize historyLen = env->GetArrayLength(historyArray);
    jclass itemClass = env->FindClass("com/tiggy/gemma4mnn/engine/ChatDataItem");
    jfieldID roleField = env->GetFieldID(itemClass, "role", "Ljava/lang/String;");
    jfieldID contentField = env->GetFieldID(itemClass, "content", "Ljava/lang/String;");

    g_session->history.clear();
    for (jsize i = 0; i < historyLen; i++) {
        jobject item = env->GetObjectArrayElement(historyArray, i);
        jstring jRole = (jstring)env->GetObjectField(item, roleField);
        jstring jContent = (jstring)env->GetObjectField(item, contentField);

        const char* role = env->GetStringUTFChars(jRole, nullptr);
        const char* content = env->GetStringUTFChars(jContent, nullptr);

        g_session->history.push_back({std::string(role), std::string(content)});

        env->ReleaseStringUTFChars(jRole, role);
        env->ReleaseStringUTFChars(jContent, content);
        env->DeleteLocalRef(item);
    }

    // Read prompt
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string promptStr_cpp(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);

    LOGD("generate: prompt length=%zu, history size=%zu", promptStr_cpp.size(), g_session->history.size());

    // TODO: Replace with actual MNN response call
    // g_session->llm->response(g_session->history, &output_stream, "<eop>", 0);
    // Then step through with llm->generate(1) loop

    // Stub: deliver a few test tokens to verify the pipeline
    deliverTokenToJava(g_session, "Hello");
    deliverTokenToJava(g_session, "!");
    deliverTokenToJava(g_session, " This");
    deliverTokenToJava(g_session, " is");
    deliverTokenToJava(g_session, " a");
    deliverTokenToJava(g_session, " test");
    deliverTokenToJava(g_session, " response");
    deliverTokenToJava(g_session, ".");

    return 8; // Number of tokens delivered (stub)
}

JNIEXPORT void JNICALL
Java_com_tiggy_gemma4mnn_engine_MnnSession_stopGeneration(JNIEnv* env, jobject thiz) {
    LOGD("stopGeneration");
    if (g_session) {
        g_session->stop_requested = true;
    }
}

JNIEXPORT void JNICALL
Java_com_tiggy_gemma4mnn_engine_MnnSession_release(JNIEnv* env, jobject thiz) {
    LOGD("release");
    if (g_session) {
        g_session->stop_requested = true;

        if (g_session->llm) {
            delete g_session->llm;
            g_session->llm = nullptr;
        }

        if (g_session->java_session_ref) {
            env->DeleteGlobalRef(g_session->java_session_ref);
            g_session->java_session_ref = nullptr;
        }

        delete g_session;
        g_session = nullptr;
    }
}

} // extern "C"
