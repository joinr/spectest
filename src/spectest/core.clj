(ns spectest.core
  (:require [spec-tools.data-spec :as data]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [orchestra.core :refer [defn-spec]]))

(s/def ::ANY any?) ;;kind of redundant
(s/def ::bad-map (data/spec ::bad-map {:foobar   string?}))
(s/def ::Joke    (data/spec ::Joke    {:category string?
                                       :type     string?
                                       :joke     string?
                                       :id       int?}))


(def random-jokes (->> (s/exercise ::Joke)
                       (map first)))
;;(first random-jokes)
;;{:category "", :type "", :joke "", :id 0}

;;one way to catch errors is to use s/valid?.
;;If the input is valid according to the spec, we let it pass like
;;identity, otherwise we throw an exception.
(defn conforms? [spec x]
  (if (s/valid? spec x)
    x
    (throw (ex-info (str "invalid value for spec " spec \newline (s/explain-str spec x))
                    {:in x :spec spec}))))

;;We can provide a kind of low-level but readable way
;;to validate against specs like this...
(defn  jokes  [response]
  (let [; Would like to specify this as joke :- Joke
        joke  (->> response
                   :body
                   (conforms? ::Joke))]
    {:statusCode 200
     :headers    {"Content-Type" "application/json"}
     :body       joke}))

;;we can use libraries to do fancier spec definition and
;instrument functions.

;;this acts like a predicate that validates against the spec,
;;but we can use it as a simple function.
(defn-spec jokify ::Joke [x any?] x)
;;orchestra will add instrumentation to our spec'd functions like jokify.
;;specs are typically not instrumented for functions in production.
;;orchestra makes it simple to turn on instrumentation for everything.
(st/instrument)

;;our second version of jokes uses jokify to get us into
;;using specs for verification.  It's not hugely different
;;from manually using valid?, or a wrapper fn like
;;conforms?, but it opens the door to more expressive stuff.
(defn  jokes2  [response]
  (let [; Would like to specify this as joke :- Joke
        joke  (->> response
                   :body
                   jokify)]
        {:statusCode 200
         :headers    {"Content-Type" "application/json"}
         :body       joke}))

;;If we define  a joke response spec,
;;we can define a function that uses it.
(s/def ::JokeResponse
  (data/spec ::JokeResponse
             {:statusCode int?
              :headers    {string? string?}
              :body ::Joke}))

;;defn-spec (also from orchestra) lets us inline
;;our specs, not unlike type signatures.  You need to
;;spec every arg though.
(defn-spec jokes3 ::JokeResponse [response map?]
  {:statusCode 200
   :headers    {"Content-Type" "application/json"}
   :body       (->> response :body)})
(st/instrument)


(def good-response {:body  (first random-jokes)})
(def bad-response  {:blah "something"})

;; spectest.core> (jokes good-response)
;; {:statusCode 200,
;;  :headers {"Content-Type" "application/json"},
;;  :body {:category "", :type "", :joke "", :id 0}}

;; spectest.core> (jokes bad-response)
;; Execution error (ExceptionInfo) at spectest.core/conforms? (form-init1090336819242511612.clj:349).
;; invalid value for spec :spectest.core/Joke
;; nil - failed: map? spec: :spectest.core/Joke

;; spectest.core> (jokes2 good-response)
;; {:statusCode 200,
;;  :headers {"Content-Type" "application/json"},
;;  :body {:category "", :type "", :joke "", :id 0}}

;; spectest.core> (jokes2 bad-response)
;; Execution error - invalid arguments to orchestra.spec.test/spec-checking-fn$conform! at (test.clj:113).
;; nil - failed: map? at: [:ret] spec: :spectest.core/Joke

;; spectest.core> (jokes3 good-response)
;; {:statusCode 200,
;;  :headers {"Content-Type" "application/json"},
;;  :body {:category "", :type "", :joke "", :id 0}}
;; spectest.core> (jokes3 bad-response)
;; Execution error - invalid arguments to orchestra.spec.test/spec-checking-fn$conform! at (test.clj:113).
;; nil - failed: map? at: [:ret :body] spec: :spectest.core$JokeResponse/body
