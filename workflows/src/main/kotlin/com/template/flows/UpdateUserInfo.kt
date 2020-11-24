package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.StatusEnums
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// *********
// Flows
// *********
@InitiatingFlow
@StartableByRPC
class UpdateUserInfo (private val name :String,
                      private val age : Int,
                      private val address : String,
                      private val status : StatusEnums,
                      private val linearId: UniqueIdentifier

): FlowLogic<SignedTransaction>() {
    override fun call() {

    }

}

