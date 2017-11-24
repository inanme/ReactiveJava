package org.inanme;

import org.junit.jupiter.api.Test;
import ratpack.server.RatpackServer;

import java.util.concurrent.TimeUnit;

public class RatpackServerTest {

    @Test
    public void test1() throws Exception {
        RatpackServer.start(server -> server
                .registryOf(registry -> registry.add("World!"))
                .handlers(chain -> chain
                        .get(ctx -> ctx.render("Hello World!"))
                        .get(":name", ctx -> ctx.render("Hello " + ctx.getPathTokens().get("name") + "!"))
                )
        );

        TimeUnit.SECONDS.sleep(100L);
    }
}
