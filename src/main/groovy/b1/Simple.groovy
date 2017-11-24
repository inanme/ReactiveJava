package b1

import static ratpack.groovy.Groovy.ratpack

class Greet {
    String hi(String name) {
        "hello $name"
    }
}

ratpack {
    bindings {
        bindInstance(new Greet())
    }
    handlers {
        get("hello/:name") { Greet greet ->
            render(greet.hi(pathTokens.get("name")))
        }
    }
}
