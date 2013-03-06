##A simple implementation of the tile game Rummikub for Google App Engine (GAE).
Sample available at [www.rummikub-game.appspot.com]()
The game creates a two player virtual board with move data exchanged using GAE's Channel API.  There is no server side state, game state is stored in the browser of the current turn's player.

The server code is written in Clojure using the Google App Engine tool Gaeshi.  The browser code is written in Clojurescript which can be compiled to Javascript using lein-cljsbuild.  It's nice to be able to write a project with both server and client code in the same language.

To run
