package com.example.palmcollector

data class Subject (
    var subjectID : String,
    var leftList : MutableList<SubjectMetaData>,
    var rightList : MutableList<SubjectMetaData>,
    ): java.io.Serializable