#include <ostream>
#include "midemo.h"
extern "C"

JNIEXPORT jobject JNICALL Java_com_example_palmcollector_NativeInterface_display(
        JNIEnv* env, jobject thiz, jobject hands_result) {

    jclass cls = env->GetObjectClass(hands_result);
    jmethodID methodId = env->GetMethodID(cls, "timestamp", "()J");
    jlong result = env->CallLongMethod(hands_result, methodId);

    jmethodID mid_MultiHandLandmarksList = env->GetMethodID(cls, "multiHandLandmarks", "()Lcom/google/common/collect/ImmutableList;");
    jobject obj_multiHandLandmarksList = env->CallObjectMethod(hands_result, mid_MultiHandLandmarksList);

    jclass cls_multiHandLandmarksList = env->GetObjectClass(obj_multiHandLandmarksList);

    jmethodID mid_MultiHandLandmardkListSize = env->GetMethodID( cls_multiHandLandmarksList, "size", "()I");
    int multiHandLandmarkListsize = env->CallIntMethod(obj_multiHandLandmarksList, mid_MultiHandLandmardkListSize);

    int landmarksize = 0;

    for (int i = 0; i < multiHandLandmarkListsize; i++) {
        jmethodID mid_get   = env->GetMethodID (cls_multiHandLandmarksList, "get","(I)Ljava/lang/Object;");

        jobject obj_multiHandleLandMarks  = env->CallObjectMethod (obj_multiHandLandmarksList, mid_get, i);
        jclass cls_multiHandleLandMarks  = env->GetObjectClass(obj_multiHandleLandMarks);

        jmethodID mid_LandmarksList = env->GetMethodID(cls_multiHandleLandMarks, "getLandmarkList", "()Ljava/util/List;");
        jobject obj_LandmarksList = env->CallObjectMethod(obj_multiHandleLandMarks, mid_LandmarksList);

        jclass cls_LandmarksList = env->GetObjectClass(obj_LandmarksList);

        jmethodID mid_LandmardkListSize = env->GetMethodID(cls_LandmarksList, "size", "()I");
        landmarksize = env->CallIntMethod(obj_LandmarksList, mid_LandmardkListSize);

        LOGD( "This is a number from JNI: %d", landmarksize );
    }

    jclass cls_result = env->FindClass("com/example/palmcollector/ProcessedData");
    jmethodID mid_result = env->GetMethodID(cls_result, "<init>", "()V");
    auto newResultClass = env->NewObject(cls_result, mid_result);

    jfieldID valueTimestamp = env->GetFieldID(cls_result, "timestamp", "J");
    env->SetLongField(newResultClass, valueTimestamp, result);

    jfieldID valueLandmarksize = env->GetFieldID(cls_result, "landmarksize", "I");
    env->SetIntField(newResultClass, valueLandmarksize, landmarksize);

    return newResultClass;
//    return env->NewStringUTF("Value Returned from JNI ");
}