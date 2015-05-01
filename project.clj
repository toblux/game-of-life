(defproject game-of-life "0.1.0"
  :description "Game of Life"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.753"]
                 [org.clojure/core.async "1.1.587"]]

  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-figwheel "0.5.19"]]

  :clean-targets ^{:protect false} ["target"
                                    "resources/public/js"
                                    ".rebel_readline_history"
                                    ".lein-repl-history"
                                    "figwheel_server.log"]

  :cljsbuild {:builds [{:id           "debug"
                        :figwheel     true
                        :source-paths ["src"]
                        :compiler     {:main          "game-of-life.core"
                                       :asset-path    "js"
                                       :output-to     "resources/public/js/game-of-life.js"
                                       :output-dir    "resources/public/js"
                                       :optimizations :none
                                       :source-map    true}}
                       {:id           "release"
                        :source-paths ["src"]
                        :compiler     {:elide-asserts true
                                       :pretty-print  false
                                       :output-to     "resources/public/js/game-of-life.js"
                                       :optimizations :advanced}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
