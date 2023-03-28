import io.vertx.core.Launcher
import io.vertx.core.VertxOptions
import io.vertx.rxjava3.core.Vertx
import verticle.MainVerticle
import zipkin.ZipkinTracingOptions

fun main() {
    Launcher.executeCommand("run", MainVerticle::class.java.name)

//    val vertx = Vertx.vertx(
//        VertxOptions()
//            .setTracingOptions(
//                ZipkinTracingOptions().setServiceName("A cute service")
//            )
//    )
//
//    vertx.deployVerticle(MainVerticle::class.java.name)
//    vertx.run {}
}