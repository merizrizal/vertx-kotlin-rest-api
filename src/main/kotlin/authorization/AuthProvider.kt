package authorization

import io.reactivex.rxjava3.functions.Consumer
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.ext.auth.User
import io.vertx.rxjava3.ext.auth.authentication.AuthenticationProvider
import io.vertx.rxjava3.ext.auth.authorization.PermissionBasedAuthorization
import io.vertx.rxjava3.ext.auth.jwt.authorization.JWTAuthorization

class AuthProvider {
    private var authorizationProvider = JWTAuthorization.create("heroes")!!
    private val saveAuthorization = HashMap<String, PermissionBasedAuthorization>()

    var authenticationProvider = AuthenticationProvider.newInstance { credentials, resultHandler ->
        if ((credentials.getString("userId") == "meriz" || credentials.getString("userId") == "rizal")
            && credentials.getString("password") == "123456"
        ) {
            val user = User.create(credentials)
            user.principal().put("userName", "Meriz Rizal")

            if (user.principal().getString("userId").equals("meriz")) {
                user.principal().put("meriz", JsonArray().add("delete"))
                saveAuthorization["delete"] = PermissionBasedAuthorization.create("delete")
            }

            resultHandler.handle(Future.succeededFuture(user.delegate))
        } else {
            resultHandler.handle(Future.failedFuture("Unsuccessfull authorization. Try again !!"))
        }
    }!!

    val publicKey =
        "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApQyxmjhc+UFxH/dVK0ev\n" +
                "------My PUBLIC key here------" +
                "7YeSA8jdEOesIjqtOIgcaB5153k4zBOrlrmnRqx8XwIIM7TWQdqdza5lvXqImzvF\n" +
                "CQIDAQAB\n" +
                "-----END PUBLIC KEY-----"

    val privateKey =
        "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQClDLGaOFz5QXEf\n" +
                "---------------------------------" +
                "------My PRIVATE key here------" +
                "---------------------------------" +
                "QgcLmNCi1RMVuMXbBkauA0hhYTQZjCymI7ng/7tQ66bAVoou1cN0OlgijX978lFu\n" +
                "VgQ6Xq19ZKUbu8uuhMjRm5n4\n" +
                "-----END PRIVATE KEY-----"

    fun verifyPermissionAuthorization(user: User, task: String, callback: Consumer<JsonObject>) {
        authorizationProvider
            .rxGetAuthorizations(user)
            .subscribe {
                if (saveAuthorization.containsKey(task) && saveAuthorization[task]!!.match(user)) {
                    callback.accept(JsonObject().apply {
                        put("success", true)
                    })
                } else {
                    callback.accept(JsonObject().apply {
                        put("success", false)
                        put("error", "You don't have permission to perform $task task")
                    })
                }
            }
    }
}