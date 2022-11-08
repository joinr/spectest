(ns spectest.demo
  (:require [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]
            [clojure.spec.gen.alpha :as gen]))

;;this is pretty unrestrictive, we probably want more
;;realistic bounds.
(s/def ::pos (s/int-in 0 Long/MAX_VALUE))
;;dummy data for game codes, let's assume a finite set of ints.
(s/def ::code #{99 22 85 494})
;;a set of currencies for demonstration.
(s/def ::currency #{"dollars" "euros" "quatloos" "burgers"})

;;a spec-tools data spec registered under ::Amount .
(s/def ::Amount
  (ds/spec ::Amount
           {:amount ::poss
            :currency ::currency}))

;;a spec-tools data spec registered under ::Bet .
(s/def ::Bet
  (ds/spec ::Bet
           {:amounts [::Amount]
            :game-code ::code
            :description string?}))


;;we can leverage these with records as-is, since they all assume
;;unqualified keys.

(defrecord Amount [^int amount ^String currency])
(defrecord Bet [amounts ^int game-code ^String description])

;;spectest.demo> (s/valid? ::Bet (->Bet [(->Amount 10 "quatloos")] 494 "A demo!"))
;;true

;;generate some sample data to get an idea of the specs or have stuff
;;for testing:
(defn sample-data [n spec]
  (let [g (s/gen spec)]
    (repeatedly n (fn [] (gen/generate g)))))

;; spectest.demo> (sample-data 3 ::Amount)
;; ({:amount 3312193, :currency "euros"}
;;  {:amount 40988292, :currency "quatloos"}
;;  {:amount 546, :currency "euros"})

;; spectest.demo> (sample-data 2 ::Bet)
;; ({:amounts
;;   [{:amount 7230045, :currency "euros"}
;;    {:amount 750758, :currency "euros"}
;;    {:amount 11266482, :currency "burgers"}
;;    {:amount 6, :currency "dollars"}
;;    {:amount 4220989, :currency "burgers"}
;;    {:amount 0, :currency "dollars"}
;;    {:amount 1, :currency "dollars"}],
;;   :game-code 85,
;;   :description "UHAzFO4Fx12d7"}
;;  {:amounts
;;   [{:amount 12, :currency "burgers"}
;;    {:amount 397, :currency "quatloos"}
;;    {:amount 6019, :currency "dollars"}
;;    {:amount 1, :currency "euros"}
;;    {:amount 629, :currency "dollars"}
;;    {:amount 36, :currency "euros"}
;;    {:amount 28, :currency "euros"}
;;    {:amount 15, :currency "burgers"}
;;    {:amount 17606779, :currency "burgers"}
;;    {:amount 354832031, :currency "euros"}],
;;   :game-code 494,
;;   :description "Io4m"})
