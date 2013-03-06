(ns rummikub.controller.message-controller
  (:import [com.google.appengine.api.channel ChannelServiceFactory ChannelMessage])
  (:use ring.util.response)
  )

(defn is-local []
  (or (.startsWith (System/getProperty "os.name") "Windows") (= "/h" (.substring (System/getProperty "user.dir") 0 2))))

(defn send-to-channel [channel str]
  (let [channelService (ChannelServiceFactory/getChannelService)]
        (.sendMessage channelService (ChannelMessage. channel str))))

#_(defn message-controller [request]
  (try (let [
    message (-> request :body slurp read-string)
    channel (if (= 1 (:recipient message)) "abc" "def")
    ]
    (if (is-local)
      (spit "public/message.txt" (.toString message)) 
      (send-to-channel channel (.toString message)))
    (response ""))
  (catch Exception e 
    (->> request :body slurp count (spit "public/error.txt"))
    #_(throw e))))
 
(defn message-controller [request]
  (let [
    message (-> request :body slurp)
    ]
    (try
      (let [
        message (read-string message)
        channel (if (= 1 (:recipient message)) "abc" "def")
        ]
        (if (is-local)
          (spit (format "public/messageFor%s.txt" (:recipient message)) (.toString message))
          (send-to-channel channel (.toString message)))
        (response ""))
      (catch Exception e
        (do
          (spit "public/error.txt" (str "error: " message))
          (throw e)))))) 