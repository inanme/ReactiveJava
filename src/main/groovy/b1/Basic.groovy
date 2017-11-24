package b1

import static ratpack.groovy.Groovy.ratpack

ratpack {
    handlers {
        get("hello/:name") {
            render("Helo ${pathTokens.get("name")}")
        }
    }
}
