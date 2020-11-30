package functions

import co.paralleluniverse.fibers.Suspendable
import com.template.states.UserState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

abstract class FunctionFlow  :FlowLogic<SignedTransaction>()  {
        val Notary
            get() = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                    ?: throw FlowException("No Notary Found.")



}