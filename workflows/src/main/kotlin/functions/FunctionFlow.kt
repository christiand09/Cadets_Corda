package functions

import co.paralleluniverse.fibers.Suspendable
import com.template.states.UserState
import javassist.NotFoundException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*
import javax.annotation.Signed

abstract class FunctionFlow  :FlowLogic<SignedTransaction>()  {
        val myNotary
            get() = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                    ?: throw FlowException("No Notary Found.")

    fun spectator(): List<Party>{
        return listOf(spectate())
    }
    fun spectate(): Party{
        return serviceHub.identityService.partiesFromName("observer water", false).singleOrNull()
                ?: throw NotFoundException("No match found")



    }
    @Suspendable
    fun signCollectNotarize(
        session: List<FlowSession>,
        utx: TransactionBuilder,
        spectator: List<Party>? = null

) : SignedTransaction {

        //Verify Transaction
        utx.verify(serviceHub)

        //Signing transaction locally
        val ptx  = serviceHub.signInitialTransaction(utx)

        //collecting signatures
        val ctx  = subFlow(CollectSignaturesFlow(ptx, session))

        //Initiate flow
        val ntx  = subFlow(FinalityFlow(ctx, session))

        return ntx

    }
}