package com.template.states

import com.sun.xml.internal.ws.developer.Serialization
import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


// *********
//  State
// *********
@Serialization
@BelongsToContract(UserContract::class)
class UserState(val name: String,
                val age: Int,
                val address: String,
                val gender: Gender,
                val node: Party,
                val delete: Boolean = false,
                val status: Status,
                override val linearId: UniqueIdentifier,
                override val participants : List<Party>
) :  LinearState
