package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.GenderEnums
import com.template.states.StatusEnums
import com.template.states.UserState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object UpdateUserFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator (private val name :String,
                          private val age : Int,
                          private val address : String,
                          private val gender: GenderEnums,
                          private val status : StatusEnums,
                          private val linearId: UniqueIdentifier) : BaseFlow() {

        // Added linearId to use as reference when updating the past state
        private fun userStates(dataState: StateAndRef<UserState>): UserState {

            val data = dataState.state.data

            return UserState(
                    name = name,
                    age = age,
                    address = address,
                    gender = gender,
                    status = status,
                    node = ourIdentity,
                    linearId = linearId,
                    participants = data.participants
            )
        }

        @Suspendable
        override fun call(): SignedTransaction {

            val dataState = getVaultData(linearId)
            val transaction: TransactionBuilder = transaction(userStates(dataState), getVaultData(linearId))
            val signedTransaction: SignedTransaction = verifyAndSign(transaction)
            val sessions: List<FlowSession> = (userStates(dataState).participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
            val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
            return recordTransaction(transactionSignedByAllParties, sessions)
        }

    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(flowSession)
            {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                }
            }
            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
        }
    }
}