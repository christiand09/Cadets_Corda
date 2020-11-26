package com.template.states

import net.corda.core.serialization.CordaSerializable



enum class SubStates {}

@CordaSerializable
enum class Gender {
    MALE, FEMALE
}
@CordaSerializable
enum class Status {
    SINGLE, MARRIED
}

