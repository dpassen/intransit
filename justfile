default:
    @just --list

lint:
    @clj-kondo --parallel --lint src deps.edn build.edn

install:
    @clojure -T:build install

publish:
    @clojure -T:build deploy
