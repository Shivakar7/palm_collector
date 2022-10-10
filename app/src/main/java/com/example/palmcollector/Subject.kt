package com.example.palmcollector

data class Subject (
    var subjectID : String,
    var leftList : List<SubjectMetaData>,
    var rightList : List<SubjectMetaData>,
    )