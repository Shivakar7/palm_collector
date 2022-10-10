package com.example.palmcollector

import com.google.mediapipe.solutions.hands.HandsResult

public data class ProcessedData(var timestamp: Long = 0, var landmarksize: Int = 0)

class NativeInterface {

    external fun display(handsResult: HandsResult) : ProcessedData
}