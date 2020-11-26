package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.Gender
import com.template.states.Status
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// *********
// ONLY ONE PARTY VIEW INFO
// *********
@InitiatingFlow
@StartableByRPC
class CreateUserOwnNodesWithoutOtherParty (
        private val name :String,
        private val age : Int,
        private val address : String,
        private val gender: Gender,
        private val status : Status): FlowLogic<SignedTransaction>() {

    private fun userStates(): UserState {
        return UserState(
                name = name,
                age = age,
                address =address,
                status = status,
                gender = gender,
                node = ourIdentity,
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity)
        )
    }

    @Suspendable
    override fun call(): SignedTransaction {
        val transaction: TransactionBuilder = transaction()
        val transactionSignedByAllParties: SignedTransaction = verifyAndSign(transaction)
        return recordTransaction(transactionSignedByAllParties)
    }

    private fun transaction(): TransactionBuilder {
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand = Command(UserContract.Commands.Issue(), ourIdentity.owningKey)
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(userStates(), UserContract.ID)
        builder.addCommand(issueCommand)
        return builder
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction): SignedTransaction =
            subFlow(FinalityFlow(transaction, emptyList()))
}

