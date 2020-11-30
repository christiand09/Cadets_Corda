package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.Gender
import com.template.states.Status
import com.template.states.UserState
import functions.FunctionFlow
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// *********
// Flows
// *********

@InitiatingFlow
@StartableByRPC
class Initiator (private val name :String,
                 private val age : Int,
                 private val address : String,
                 private val status : Status,
                 private val gender: Gender,
                 private val counterParty: Party): FunctionFlow() {

    private fun userStates(): UserState {
        return UserState(
                name = name,
                age = age,
                address =address,
                status = status,
                gender = gender,
                node = ourIdentity,
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity, counterParty)
        )
    }
    @Suspendable
    override fun call(): SignedTransaction {


        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)

        val sessions: List<FlowSession> = (userStates().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun transaction(): TransactionBuilder {
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand = Command(UserContract.Commands.Issue(), userStates().participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)

        builder.addOutputState(userStates(), UserContract.ID)
        builder.addCommand(issueCommand)
        return builder
    }


}

@InitiatedBy(Initiator::class)
class IOUIssueFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}