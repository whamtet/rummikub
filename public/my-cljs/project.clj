(defproject my-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [jayq "1.0.0"]
                 ]

:cljsbuild
{:builds
 [{:source-paths ["src-cljs"],
   :builds nil,
   :compiler
   {:pretty-print false,
    :output-dir "out",
    :output-to "main.js",
    :externs ["externs/externs.js" "externs/jquery-1.8.js"],
    :optimizations :none}}]})
