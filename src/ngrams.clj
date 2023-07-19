(ns ngrams
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:gen-class))

(def atlas-uri "<mongo-connection-string>>")

(defn- gen-key [k]
  (-> (name k)
      (str "_ngram")
      (keyword)))

(defn- str->ngram [n word]
  (map #(apply str %) (partition n 1 word)))

(defn save-ngram [document]
  (let [{:keys [conn db]}    (mg/connect-via-uri atlas-uri)
        document-with-ngrams (-> document
                                 (merge (into {} (for [[k v] document] [(gen-key k) (str->ngram 3 v)]))))]
    (mc/insert db "address" document-with-ngrams)))

(defn fuzzy-match [phrase]
  (let [{:keys [conn db]}    (mg/connect-via-uri atlas-uri)
        grammed-phrase (str->ngram 3 phrase)]
    (mc/aggregate db "address" [{"$match"   {:streetAddress1_ngram {"$in" grammed-phrase}}}
                                {"$project" {:ngram-array-size     {"$size" "$streetAddress1_ngram"}
                                             :phrase               "$streetAddress1"
                                             :matched-ngrams       {"$size" {"$filter" {"input" "$streetAddress1_ngram"
                                                                                        "cond"  {"$in" ["$$this", grammed-phrase]}}}}
                                             ;score <- size(matched-ngrams)/ngram-array-size
                                             :score {"$divide" [{"$size" {"$filter" {"input" "$streetAddress1_ngram"
                                                                                     "cond"  {"$in" ["$$this" grammed-phrase]}}}}
                                                                {"$size" "$streetAddress1_ngram"}]}}}
                                {"$sort"    {:score -1}}])))

(defn blow-db []
  (let [uri               atlas-uri
        {:keys [conn db]} (mg/connect-via-uri uri)]
    (mc/remove db "address")))

(defn -main [& args]
  (print (fuzzy-match "some"))
  ;(save-ngram {:streetAddress1 "123 some address"})
  ;(blow-db)
  )
