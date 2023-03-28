package zipkin;

import brave.Span;
import brave.http.HttpServerHandler;
import brave.http.HttpServerRequest;
import brave.http.HttpServerResponse;
import brave.http.HttpTracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import io.vertx.core.Handler;
import io.vertx.rxjava3.core.net.SocketAddress;
import io.vertx.rxjava3.ext.web.RoutingContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h3>Why not rely on {@code context.request().endHandler()} to finish a span?</h3>
 * <p>There can be only one {@link io.vertx.core.http.HttpServerRequest#endHandler(Handler) end
 * handler}. We can't rely on {@code endHandler()} as a user can override it in their route. If they
 * did, we'd leak an unfinished span. For this reason, we speculatively use both an end handler and
 * an end header handler.
 *
 * <p>The hint that we need to re-attach the headers handler on re-route came from looking at
 * {@code TracingHandler} in https://github.com/opentracing-contrib/java-vertx-web
 */
final class TracingRoutingContextHandler implements Handler<RoutingContext> {
    final HttpServerHandler<HttpServerRequest, HttpServerResponse> handler;
    final CurrentTraceContext currentTraceContext;

    TracingRoutingContextHandler(HttpTracing httpTracing) {
        handler = HttpServerHandler.create(httpTracing);
        currentTraceContext = httpTracing.tracing().currentTraceContext();
    }

    @Override public void handle(RoutingContext context) {
        TracingHandler tracingHandler = context.get(TracingHandler.class.getName());
        if (tracingHandler != null) { // then we already have a span
            if (!context.failed()) { // re-routed, so re-attach the end handler
                context.addHeadersEndHandler(tracingHandler);
            }
            context.next();
            return;
        }

        Span span = handler.handleReceive(new HttpServerRequestWrapper(context.request()));
        TracingHandler handler = new TracingHandler(context, span);
        context.put(TracingHandler.class.getName(), handler);
        context.addHeadersEndHandler(handler);

        try (Scope ws = currentTraceContext.maybeScope(span.context())) {
            context.next();
        }
    }

    class TracingHandler implements Handler<Void> {
        final RoutingContext context;
        final Span span;
        final AtomicBoolean finished = new AtomicBoolean();

        TracingHandler(RoutingContext context, Span span) {
            this.context = context;
            this.span = span;
        }

        @Override public void handle(Void aVoid) {
            if (!finished.compareAndSet(false, true)) return;
            handler.handleSend(new HttpServerResponseWrapper(context), span);
        }
    }

    static final class HttpServerRequestWrapper extends HttpServerRequest {
        final io.vertx.rxjava3.core.http.HttpServerRequest delegate;

        HttpServerRequestWrapper(io.vertx.rxjava3.core.http.HttpServerRequest delegate) {
            this.delegate = delegate;
        }

        @Override public io.vertx.rxjava3.core.http.HttpServerRequest unwrap() {
            return delegate;
        }

        @Override public String method() {
            return delegate.method().toString();
        }

        @Override public String path() {
            return delegate.path();
        }

        @Override public String url() {
            return delegate.absoluteURI();
        }

        @Override public String header(String name) {
            return delegate.headers().get(name);
        }

        @Override public boolean parseClientIpAndPort(Span span) {
            if (parseClientIpFromXForwardedFor(span)) return true;
            SocketAddress addr = delegate.remoteAddress();
            return span.remoteIpAndPort(addr.host(), addr.port());
        }
    }

    static final class HttpServerResponseWrapper extends HttpServerResponse {
        final RoutingContext context;
        HttpServerRequestWrapper request;

        HttpServerResponseWrapper(RoutingContext context) {
            this.context = context;
        }

        @Override public RoutingContext unwrap() {
            return context;
        }

        @Override public HttpServerRequestWrapper request() {
            if (request == null) request = new HttpServerRequestWrapper(context.request());
            return request;
        }

        @Override public Throwable error() {
            return context.failure();
        }

        @Override public int statusCode() {
            return context.response().getStatusCode();
        }

        // This is an example of where the route is not on the request object. Hence, we override.
        @Override public String route() {
            String httpRoute = context.currentRoute().getPath();
            return httpRoute != null ? httpRoute : "";
        }
    }
}
