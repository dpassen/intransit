default:
    @just --list

lint:
    @clj-kondo --parallel --lint src

install:
    @clojure -T:build install

publish:
    @clojure -T:build deploy
