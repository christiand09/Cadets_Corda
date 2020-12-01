package functions

import co.paralleluniverse.fibers.Suspendable
import com.template.flows.Initiator
import com.template.states.UserState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.Logger;

abstract class FunctionFlow  :FlowLogic<SignedTransaction>()  {
        val myNotary
            get() = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                    ?: throw FlowException("No Notary Found.")
    /**
     *SignedTransaction with counter party
     */
    @Suspendable
    fun signCollectNotarize(
            session: List<FlowSession>,
            utx: TransactionBuilder,
            log: Logger

) : SignedTransaction {

        log.info("Verify Transaction")
        utx.verify(serviceHub)

        log.info("Signing transaction locally")
        val ptx  = serviceHub.signInitialTransaction(utx)

        log.info("collecting signatures")
        val ctx  = subFlow(CollectSignaturesFlow(ptx, session))

        log.info("Initiate flow")
        return subFlow(FinalityFlow(ctx, session))
    }

    /**
     *SignedTransaction without counter party
     */
    @Suspendable
    fun notarize(
            localSigner: Party,
            utx: TransactionBuilder,
            log: Logger

    ) : SignedTransaction {

        log.info("Verify Transaction")
        utx.verify(serviceHub)

        log.info("Signing transaction locally")
        val ptx  = serviceHub.signInitialTransaction(utx, localSigner.owningKey)

        log.info("collecting signatures")
        val ctx  = subFlow(CollectSignaturesFlow(ptx, emptyList()))

        log.info("Initiate flow")
        return subFlow(FinalityFlow(ctx, emptyList()))
    }

}
@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "The output must be a UserState" using (output is UserState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}