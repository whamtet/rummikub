(ns rummikub.controller.message-controller
  (:import [com.google.appengine.api.channel ChannelServiceFactory ChannelMessage])
  (:use ring.util.response)
  )

(defn is-local?
  "are we running on localhost?"
  [request]
  (= "localhost" (:server-name request)))

(defn send-to-channel [channel str]
  (let [channelService (ChannelServiceFactory/getChannelService)]
        (.sendMessage channelService (ChannelMessage. channel str))))


(defn message-controller
  "messages posted here will be delivered to the opponent"
  [request]
  (let [
    message (-> request :body slurp)
    ]
    (try
      (let [
        message (read-string message)
        channel (if (= 1 (:recipient message)) "abc" "def")
        ]
        (if (is-local? request)
          (spit (format "public/messageFor%s.txt" (:recipient message)) (.toString message))
          (send-to-channel channel (.toString message)))
        (response ""))
      (catch Exception e
        (do
          (spit "public/error.txt" (str "error: " message))
          (throw e))))))
