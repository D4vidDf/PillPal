package com.d4viddf.medicationreminder.data // o donde quieras

import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class SimplestDependency @Inject constructor() {
    fun doSomething(): String = "SimplestDependency is working"
}