package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import functions.FunctionFlow
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
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
class DeleteUserFlow (private val linearId: UniqueIdentifier) : FunctionFlow() {

    private companion object{
        val log = loggerFor<CreateUserOwnNodesWithoutOtherParty>()
    }

    @Suspendable
    override fun call(): SignedTransaction {

        fun newUserState(settle: StateAndRef<UserState>): UserState {
            return UserState(
                    name = settle.state.data.name,
                    age = settle.state.data.age,
                    address = settle.state.data.address,
                    status = settle.state.data.status,
                    gender = settle.state.data.gender,
                    node = ourIdentity,
                    linearId = linearId,
                    delete = true,
                    participants = settle.state.data.participants
            )
        }
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val settle = serviceHub.vaultService.queryBy<UserState>(queryCriteria).states.single()
        val sessions: List<FlowSession> = (newUserState(settle).participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val command = Command(UserContract.Commands.Update(), newUserState(settle).participants.map { it.owningKey })

        log.info("Building Transaction")
        val utx = TransactionBuilder(myNotary)
                .addInputState(settle)
                .addOutputState(newUserState(settle), UserContract.ID)
                .addCommand(command)

        return signCollectNotarize(
                utx = utx,
                session = sessions,
                log = log
        )

    }
}
@InitiatedBy(DeleteUserFlow::class)
class DeleteUserFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable  override   fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}