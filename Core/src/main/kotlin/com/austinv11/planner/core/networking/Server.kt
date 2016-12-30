package com.austinv11.planner.core.networking

import com.austinv11.planner.core.Config
import com.austinv11.planner.core.db.DatabaseManager
import com.austinv11.planner.core.json.responses.LoginResponse
import com.austinv11.planner.core.networking.http.Routes
import com.austinv11.planner.core.util.Security
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpMethod
import org.apache.commons.validator.routines.EmailValidator
import org.wasabifx.wasabi.app.AppConfiguration
import org.wasabifx.wasabi.app.AppServer
import org.wasabifx.wasabi.protocol.http.ContentType.Companion.Application
import org.wasabifx.wasabi.protocol.http.StatusCodes
import org.wasabifx.wasabi.routing.RouteHandler
import kotlin.system.exitProcess

object Server {
    
    val appServer = AppServer(AppConfiguration(port = Config.port)) //TODO add configurable ssl support
    val routes: MutableMap<String, MutableMap<HttpMethod, RouteHandler.() -> Unit>> = mutableMapOf()
    val gson = GsonBuilder().serializeNulls().setLenient().create()
    
    fun Any.toJson(): String {
        return gson.toJson(this)
    }
    
    fun registerRoute(method: HttpMethod, route: String, handler: RouteHandler.() -> Unit) {
        registerRoute(arrayOf(method), route, handler)
    }
    
    fun registerRoute(route: String, handler: RouteHandler.() -> Unit) {
        registerRoute(arrayOf(HttpMethod.DELETE, HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.PATCH, HttpMethod.POST, 
                HttpMethod.PUT), route, handler)
    }

    fun registerRoute(methods: Array<HttpMethod>, route: String, handler: RouteHandler.() -> Unit) {
        routes.putIfAbsent(route, mutableMapOf())

        val map = routes[route]!!
        methods.forEach { 
            map.put(it, handler)
        }
    }
    
    init {
        registerRoute("/nuke", { exitProcess(1) }) //TODO: Remove on release
        
        registerRoute(HttpMethod.POST, Routes.AUTH, { //Logging in
            val user = request.bodyParams["user"].toString()
            val password = request.bodyParams["password"].toString()
            val isEmail = EmailValidator.getInstance().isValid(user)

            val queryResult = DatabaseManager.ACCOUNT_DAO
                    .queryForFieldValuesArgs(mapOf(
                            Pair(if (isEmail) DatabaseManager.Account.EMAIL else DatabaseManager.Account.USERNAME, user)))
            
            if (queryResult.size == 0) {
                response.setStatus(StatusCodes.NotFound, "User $user does not exist!")
                return@registerRoute
            } else {
                val account = queryResult.first() //Registration process should block a dupe username or email
                val verification = Security.verify(account.hash, password, account.salt)
                if (!verification && !Config.no_auth) {
                    response.setStatus(StatusCodes.Unauthorized, "Login failed.")
                    return@registerRoute
                } else {
                    if (!account.verified && Config.enforce_account_verification) {
                        response.setStatus(StatusCodes.Forbidden, "Please verify your account.")
                        return@registerRoute
                    } else {
                        response.send(LoginResponse("TODO").toJson(), Application.Json.contentType)
                        return@registerRoute
                    }
                }
            }
        })
        
        registerRoute(arrayOf(HttpMethod.POST, HttpMethod.DELETE, HttpMethod.GET), Routes.ACCOUNT, { //Create/delete/get account info
            when(request.method) {
                HttpMethod.POST -> { //Create account
                    val username = request.bodyParams["username"].toString()
                    val email = request.bodyParams["email"].toString()

                    if (!EmailValidator.getInstance().isValid(email)) {
                        response.setStatus(StatusCodes.BadRequest, "Email $email is invalid.")
                        return@registerRoute
                    }

                    val usernameQueryResult = DatabaseManager.ACCOUNT_DAO.queryForFieldValuesArgs(mapOf(
                            Pair(DatabaseManager.Account.USERNAME, username)))
                    val emailQueryResult = DatabaseManager.ACCOUNT_DAO.queryForFieldValuesArgs(mapOf(
                                    Pair(DatabaseManager.Account.EMAIL, email)))
                    
                    if (usernameQueryResult.size != 0) {
                        response.setStatus(StatusCodes.Forbidden, "Username $username is already taken.")
                        return@registerRoute
                    } else if (emailQueryResult.size != 0) {
                        response.setStatus(StatusCodes.Forbidden, "Email $email already has an account.")
                        return@registerRoute
                    }
                    
                    if (Config.password_requirements_regex != null 
                            && !request.bodyParams["password"].toString().matches(Regex(Config.password_requirements_regex!!))) {
                        response.setStatus(StatusCodes.BadRequest, "Password does not match regex ${Config.password_requirements_regex}")
                        return@registerRoute
                    }
                    
                    val (salt, hash) = Security.hash(request.bodyParams["password"].toString()) //We don't store passwords ;)
                    DatabaseManager.Account(username, email, hash, salt)
                    
                    response.setStatus(StatusCodes.OK)
                    return@registerRoute
                }
                HttpMethod.DELETE -> { //Delete account TODO: Replace gson content hack, this is currently due to a combination of https://github.com/wasabifx/wasabi/issues/107 and https://github.com/wasabifx/wasabi/issues/106
                    val string = String((request.httpRequest as HttpContent).content().copy().array())
                    val content = JsonParser().parse(string).asJsonObject
//                    val user = request.bodyParams["user"].toString()
//                    val password = request.bodyParams["password"].toString()
                    val token = request.rawHeaders["token"].toString()
                    val user = content.get("user").asString
                    val password = content.get("password").asString
                    val isEmail = EmailValidator.getInstance().isValid(user)

                    val queryResult = DatabaseManager.ACCOUNT_DAO
                            .queryForFieldValuesArgs(mapOf(
                                    Pair(if (isEmail) DatabaseManager.Account.EMAIL else DatabaseManager.Account.USERNAME, user)))
                    
                    if (queryResult.size == 0) {
                        response.setStatus(StatusCodes.NotFound, "User $user does not exist!")
                        return@registerRoute
                    } else {
                        val account = queryResult.first() //Registration process should block a dupe username or email
                        val verification = Security.verify(account.hash, password, account.salt) //TODO: Also check auth token for validity
                        if (!verification) {
                            response.setStatus(StatusCodes.Unauthorized, "Unauthorized!")
                            return@registerRoute
                        } else {
                            DatabaseManager.ACCOUNT_DAO.delete(account)

                            response.setStatus(StatusCodes.OK)
                            return@registerRoute
                        }
                    }
                }
                HttpMethod.GET -> { //Get account info
                    val token = request.rawHeaders["token"].toString() //TODO: token validation
                    
                    //TODO: get user info from token
                }
            }
        })
    }
    
    fun start() {
        routes.forEach { route, methods -> 
            methods.forEach { method, handler -> 
                when(method) {
                    HttpMethod.GET -> {
                        appServer.get(route, handler)
                    }
                    HttpMethod.POST -> {
                        appServer.post(route, handler)
                    }
                    HttpMethod.PUT -> {
                        appServer.put(route, handler)
                    }
                    HttpMethod.DELETE -> {
                        appServer.delete(route, handler)
                    }
                    HttpMethod.PATCH -> {
                        appServer.patch(route, handler)
                    }
                    HttpMethod.OPTIONS -> {
                        appServer.options(route, handler)
                    }
                }
            }
        }
        
        appServer.exception { 
            println("Exception caught by wasabi!")
            exception.printStackTrace()
            response.setStatus(StatusCodes.BadRequest, exception.localizedMessage)
        }
        
        appServer.start()
        appServer.start()
    }
}
