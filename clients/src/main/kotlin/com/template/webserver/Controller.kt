package com.template.webserver

import com.template.flows.*
import com.template.states.GenderEnums
import com.template.states.StatusEnums
import com.template.states.UserState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest




val SERVICE_NAMES = listOf("Notary", "Network Map Service")

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */

    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoAmI() = mapOf("me" to myLegalName)

    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
            .map { it.legalIdentities.first().name }
            //filter out myself, notary and eventual network map started by driver
            .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    @GetMapping(value = ["/states"], produces = [APPLICATION_JSON_VALUE])
    private fun states(): String {
        return proxy.vaultQueryBy<UserState>().states.toString()
    }
    /**
     * CreateUserFlow
     */
    @PostMapping(value = [ "create-user" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun createUser(request: HttpServletRequest): ResponseEntity<String> {
        val name = request.getParameter("name").toString()
        val age = request.getParameter("age").toInt()
        val address = request.getParameter("address").toString()
        val gender = GenderEnums.valueOf(request.getParameter("gender").toString())
        val status = StatusEnums.valueOf(request.getParameter("status").toString())
        val partyName = request.getParameter("partyName") ?: return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")

        val partyX500Name = CordaX500Name.parse(partyName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?:
        return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(CreateUserFlow::Initiator, name, age,address, gender, status, otherParty ).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("User $name with Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * CreateUserOwnNodesFlow
     */
    @PostMapping(value = [ "create-user-own-nodes" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun createUserOwnNodes(request: HttpServletRequest): ResponseEntity<String> {
        val name = request.getParameter("name").toString()
        val age = request.getParameter("age").toInt()
        val address = request.getParameter("address").toString()
        val gender = GenderEnums.valueOf(request.getParameter("gender").toString())
        val status = StatusEnums.valueOf(request.getParameter("status").toString())

        return try {
            val signedTx = proxy.startTrackedFlow(CreateUserOwnNodesFlow::Initiator, name, age,address, gender, status).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("User $name with Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * UpdateUserFlow
     */
    @PutMapping(value = ["update-user"], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun updateUser(request: HttpServletRequest) : ResponseEntity<String> {
        val name = request.getParameter("name").toString()
        val age = request.getParameter("age").toInt()
        val address = request.getParameter("address").toString()
        val gender = GenderEnums.valueOf(request.getParameter("gender").toString())
        val status = StatusEnums.valueOf(request.getParameter("status").toString())
        val linearId = UniqueIdentifier.fromString(request.getParameter("linearId"))
        return try {
            val signedTx = proxy.startTrackedFlow(UpdateUserFlow::Initiator, name, age, address,gender, status, linearId).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("User $name with Transaction id ${signedTx.id} committed to ledger.\n")
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * UpdateNameFlow
     */
    @PutMapping(value = ["update-name"], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun updateName(request: HttpServletRequest) : ResponseEntity<String> {
        val name = request.getParameter("name").toString()
        val linearId = UniqueIdentifier.fromString(request.getParameter("linearId"))

        return try {
            val signedTx = proxy.startTrackedFlow(UpdateNameFlow::Initiator, name, linearId).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("User $name with Transaction id ${signedTx.id} committed to ledger.\n")
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * DeleteUserFlow
     */
    @PutMapping(value = ["delete-user"], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun deleteUser(request: HttpServletRequest) : ResponseEntity<String> {
        val linearId = UniqueIdentifier.fromString(request.getParameter("linearId"))

        return try {
            val signedTx = proxy.startFlow(DeleteUserFlow::Initiator, linearId).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

}


