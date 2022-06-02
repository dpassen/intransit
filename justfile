default:
    @just --list

lint:
    @rg -tclojure -tedn --files | xargs clj-kondo --parallel --lint

install:
    @clojure -T:build install

publish:
    @clojure -T:build deploy
