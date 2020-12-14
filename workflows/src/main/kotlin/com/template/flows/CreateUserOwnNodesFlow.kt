package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.GenderEnums
import com.template.states.StatusEnums
import com.template.states.UserState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

object CreateUserOwnNodesFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator (private val name :String,
                     private val age : Int,
                     private val address : String,
                     private val gender : GenderEnums,
                     private val status : StatusEnums): BaseFlow() {

        private fun userStates(): UserState {
            return UserState(
                    name = name,
                    age = age,
                    address = address,
                    gender = gender,
                    status = status,
                    node = ourIdentity,
                    linearId = UniqueIdentifier(),
                    participants = listOf(ourIdentity)
            )
        }

        @Suspendable
        override fun call(): SignedTransaction {
            val transaction: TransactionBuilder = transaction(userStates())
            val transactionSignedByAllParties: SignedTransaction = verifyAndSign(transaction)
            return recordTransaction(transactionSignedByAllParties, Collections.emptyList())
        }

    }
}