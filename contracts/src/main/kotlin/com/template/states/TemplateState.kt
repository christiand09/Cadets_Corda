package com.template.states

import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.net.Inet4Address

// *********
// * State *
// *********
@CordaSerializable

enum class statusType {
    Single,
    Married
}

@BelongsToContract(TemplateContract::class)
data class TemplateState(val nameA:Party,
                         val nameB:Party,
                         val age: Int,
                         val gender: Int,
                         val address: String,
                         val status: statusType) : ContractState{

    override val participants: List<AbstractParty>
        get() = listOf(nameA,nameB)
}
