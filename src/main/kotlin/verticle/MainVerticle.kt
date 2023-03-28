package verticle

import io.vertx.core.Promise
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.core.RxHelper


class MainVerticle : AbstractVerticle() {
    override fun start(promise: Promise<Void>) {
        RxHelper.deployVerticle(vertx, HttpServerVerticle())
            .subscribe(
                { promise.complete() },
                promise::fail)
    }
}