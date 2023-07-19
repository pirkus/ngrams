#!/usr/bin/env bb

(require '[babashka.process :refer [shell process exec]])

(shell "clj -X system/-main")
