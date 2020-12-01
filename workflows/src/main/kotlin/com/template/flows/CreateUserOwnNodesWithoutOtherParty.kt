package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.Gender
import com.template.states.Status
import com.template.states.UserState
import functions.FunctionFlow
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.loggerFor

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
        private val status : Status): FunctionFlow() {

    private companion object{
        val log = loggerFor<InitiatingFlow>()
    }
    @Suspendable
    override fun call(): SignedTransaction {

        val issueCommand = Command(UserContract.Commands.Issue(), ourIdentity.owningKey)

        log.info("Creating output state")
        val newUserState =  UserState(
                    name = name,
                    age = age,
                    address =address,
                    status = status,
                    gender = gender,
                    node = ourIdentity,
                    linearId = UniqueIdentifier(),
                    participants = listOf(ourIdentity)
            )

        log.info("Building Transaction")
        val utx = TransactionBuilder(myNotary)
                .addOutputState(newUserState, UserContract.ID)
                .addCommand(issueCommand)

        return notarize(
                localSigner = ourIdentity,
                utx = utx,
                log = log
        )
    }

}

