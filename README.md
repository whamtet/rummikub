###A simple implementation of the tile game Rummikub for Google App Engine (GAE).
Sample available at [www.rummikub-game.appspot.com](www.rummikub-game.appspot.com).

The game creates a two player virtual board with move data exchanged using GAE's Channel API.  There is no server side state, game state is stored in the browser of the current turn's player.

The server code is written in Clojure using the Google App Engine tool Gaeshi.  The browser code is written in Clojurescript which can be compiled to Javascript using lein-cljsbuild.  It's nice to be able to write a project with both server and client code in the same language.

Building

1)  Install [Leiningen](https://github.com/technomancy/leiningen), [Gaeshi](https://github.com/slagyr/gaeshi), [Lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild) and [Google App Engine JDK for Java](https://developers.google.com/appengine/downloads#Google_App_Engine_SDK_for_Java).

2)  Compile the clojurescript to javascript.  Change directory to rummikub/public/my-cljs and type `lein cljsbuild once`.

3)  From the directory rummikub type `lein gaeshi server` to run on localhost:8080 and `lein gaeshi deploy` to deploy to Google App Engine.
