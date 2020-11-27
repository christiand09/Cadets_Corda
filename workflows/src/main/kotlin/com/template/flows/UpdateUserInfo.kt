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
import javax.management.Query

@InitiatingFlow
@StartableByRPC
class UpdateUserFlow (private val name :String,
                      private val age : Int,
                      private val address : String,
                      private val status : StatusEnums,
                      private val counterParty: Party,
                      private val linearId: UniqueIdentifier) : BaseFlow() {

    private fun userStates(): UserState {

        return UserState(
                name = name,
                age = age,
                address = address,
                status = status,
                node = ourIdentity,
                linearId = linearId,
                participants = listOf(ourIdentity, counterParty)
        )
    }

    @Suspendable
    override fun call(): SignedTransaction {

        val transaction: TransactionBuilder = transaction( userStates(), vault(linearId))
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions: List<FlowSession> = (userStates().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }



//    @Suspendable
//    private fun collectSignature(
//            transaction: SignedTransaction,
//            sessions: List<FlowSession>
//    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))
//
//    @Suspendable
//    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
//            subFlow(FinalityFlow(transaction, sessions))
}

@InitiatedBy(UpdateUserFlow::class)
class UpdateFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

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