package verticle

import authorization.AuthProvider
import io.reactivex.rxjava3.core.Single
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.ext.auth.jwt.JWTAuth
import io.vertx.rxjava3.ext.web.Router
import io.vertx.rxjava3.ext.web.RoutingContext
import io.vertx.rxjava3.ext.web.handler.BodyHandler
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler

class HttpServerVerticle : AbstractVerticle() {
    private var authProvider = AuthProvider()
    private lateinit var jwtAuth: JWTAuth

    private val users = JsonObject().put(
        "users",
        JsonObject().put(
            "tony",
            JsonObject().apply {
                put("user_id", "tony")
                put("user_name", "Tony Stark")
                put("name_alias", "Iron Man")
                put("company", "Stark Industries")
            }))

    override fun start(promise: Promise<Void>) {
        jwtAuth = JWTAuth.create(
            vertx,
            JWTAuthOptions()
                .addPubSecKey(
                    PubSecKeyOptions()
                        .setAlgorithm("RS256")
                        .setBuffer(authProvider.publicKey))
                .addPubSecKey(
                    PubSecKeyOptions()
                        .setAlgorithm("RS256")
                        .setBuffer(authProvider.privateKey)))

        val router = Router.router(vertx).apply {
            get("/api/token").handler(this@HttpServerVerticle::token)
            get("/api/users").handler(this@HttpServerVerticle::getUsers)

            route("/api/protected/*").handler(JWTAuthHandler.create(jwtAuth))
            post("/api/protected/users").handler(BodyHandler.create()).handler(this@HttpServerVerticle::setUser)
            put("/api/protected/users").handler(BodyHandler.create()).handler(this@HttpServerVerticle::updateUser)
            delete("/api/protected/users").handler(this@HttpServerVerticle::deleteUser)
        }

        vertx
            .createHttpServer()
            .requestHandler(router)
            .rxListen(8282)
            .subscribe(
                { promise.complete() },
                { failure -> promise.fail(failure.cause) })
    }

    private fun token(context: RoutingContext) {
        val credentials = JsonObject()
            .put("userId", context.request().getHeader("login"))
            .put("password", context.request().getHeader("password"))

        authProvider.authenticationProvider
            .rxAuthenticate(credentials)
            .flatMap { user ->
                Single.just(
                    jwtAuth.generateToken(
                        JsonObject().apply {
                            put("userId", user.principal().getString("userId"))
                            put("userName", user.principal().getString("userName"))
                            put("meriz", user.principal().getJsonArray("meriz"))
                        },
                        JWTOptions().apply {
                            algorithm = "RS256"
                            expiresInSeconds = 3600
                        }))
            }
            .subscribe(
                { token -> context.response().putHeader("Content-Type", "text/plain").end(token) },
                {
                    context.response()
                        .setStatusCode(401)
                        .putHeader("Content-Type", "text/plain")
                        .end(it.message)
                })
    }

    private fun getUsers(context: RoutingContext) {
        context.response().statusCode = 200

        context.response().putHeader("Content-Type", "application/json")
        context.response().end(users.encode())
    }

    private fun setUser(context: RoutingContext) {
        val userId = context.request().getParam("user_id")
        val userName = context.request().getParam("user_name")
        val nameAlias = context.request().getParam("name_alias")
        val company = context.request().getParam("company")

        users.getJsonObject("users").put(
            userId,
            JsonObject().apply {
                put("user_id", userId)
                put("user_name", userName)
                put("name_alias", nameAlias)
                put("company", company)
            })

        val response = JsonObject().apply {
            put("success", true)
            put("action", "insert")
            put("current_rows", users)
        }

        context.response().statusCode = 200

        context.response().putHeader("Content-Type", "application/json")
        context.response().end(response.encode())
    }

    private fun updateUser(context: RoutingContext) {
        val userId = context.request().getParam("user_id")
        val userName = context.request().getParam("user_name")
        val nameAlias = context.request().getParam("name_alias")
        val company = context.request().getParam("company")

        users.apply {
            getJsonObject("users").getJsonObject(userId).apply {
                put("user_name", userName)
                put("name_alias", nameAlias)
                put("company", company)
            }
        }

        val response = JsonObject().apply {
            put("success", true)
            put("action", "update")
            put("current_rows", users)
        }

        context.response().statusCode = 200

        context.response().putHeader("Content-Type", "application/json")
        context.response().end(response.encode())
    }

    private fun deleteUser(context: RoutingContext) {
        authProvider.verifyPermissionAuthorization(context.user(), "delete") { response ->
            if (response.getBoolean("success")) {
                val userId = context.request().getParam("user_id")

                users.getJsonObject("users").remove(userId)

                response.apply {
                    put("action", "delete")
                    put("current_rows", users)
                }

                context.response().statusCode = 200

                context.response().putHeader("Content-Type", "application/json")
                context.response().end(response.encode())
            } else {
                context.response().statusCode = 403
                context.response().putHeader("Content-Type", "application/json")
                context.response().end(response.encode())
            }
        }
    }
}