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
// Flows
// *********

@InitiatingFlow
@StartableByRPC
class Initiator (private val name :String,
                 private val age : Int,
                 private val address : String,
                 private val status : Status,
                 private val gender: Gender,
                 private val counterParty: Party
): FunctionFlow() {

    private companion object{
        val log = loggerFor<Initiator>()
    }

    @Suspendable
    override fun call(): SignedTransaction {

        val sessions = initiateFlow(counterParty)
        val command = Command(UserContract.Commands.Issue(), listOf(counterParty).map { it.owningKey })

        log.info("Creating output state")
        val newUserState = UserState(
                name = name,
                age = age,
                address =address,
                status = status,
                gender = gender,
                node = ourIdentity,
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity, counterParty)
        )
        log.info("Building Transaction")
        val utx = TransactionBuilder(myNotary)
                .addOutputState(newUserState, UserContract.ID)
                .addCommand(command)

        return signCollectNotarize(
                utx = utx,
                session = listOf(sessions),
                log = log
        )
    }
}

