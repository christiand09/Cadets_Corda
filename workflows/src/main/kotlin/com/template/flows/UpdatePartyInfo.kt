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
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.loggerFor


@InitiatingFlow
@StartableByRPC
class UpdatePartyInfo (private val name :String,
                  private val age : Int,
                  private val address : String,
                  private val gender: Gender,
                  private val status : Status,
                  private val counterParty: Party,
                  private val linearId: UniqueIdentifier) : FunctionFlow() {

    private companion object {
        val log = loggerFor<UpdatePartyInfo>()
    }

    @Suspendable
    override fun call(): SignedTransaction {

        val sessions = initiateFlow(counterParty)
        val command = Command(UserContract.Commands.Update(), listOf(counterParty).map { it.owningKey })
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val settle = serviceHub.vaultService.queryBy<UserState>(queryCriteria).states.single()

        log.info("Creating output state")
        val newUserState = UserState(
                name = name,
                age = age,
                address = address,
                gender = gender,
                status = status,
                node = ourIdentity,
                linearId = linearId,
                participants = listOf(ourIdentity, counterParty)
        )
        log.info("Building Transaction")
        val utx = TransactionBuilder(myNotary)
                .addInputState(settle)
                .addOutputState(newUserState, UserContract.ID)
                .addCommand(command)

        return signCollectNotarize(
                utx = utx,
                session = listOf(sessions),
                log = log
        )
    }

    @InitiatedBy(UpdatePartyInfo::class)
    class Responder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "The output must be a UserState" using (output is UserState)
                }
            }
            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
        }
    }
}
