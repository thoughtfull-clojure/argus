(ns systems.thoughtfull.argus-test
  (:require
    [clojure.test :refer [are deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]]))

(deftest encode-default-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      nil nil
      true true
      1 1
      -9007199254740991 -9007199254740991
      9007199254740991 9007199254740991
      1.2 1.2
      "foo" "foo"
      ["foo"] ["foo"]
      {"#set" [1]} #{1}

      {"#uuid" "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"}
      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa")))

(deftest decode-default-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      nil nil
      true true
      1 1
      1.2 1.2
      -9007199254740991 -9007199254740991
      9007199254740991 9007199254740991
      "foo" "foo"
      ["foo"] ["foo"]
      #{1} {"#set" [1]}

      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"
      {"#uuid" "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"})))

(deftest decode-default-failures
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      {"#set" "foo"}
      {"#set" "foo"}
      {"#set" 42}
      {"#set" 42}
      {"#uuid" "foo"}
      {"#uuid" "foo"}
      {"#uuid" 42}
      {"#uuid" 42})))

(deftest encode-clojure-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      [1 2] (map inc (range 2))
      [1 2] (list* 1 2 nil)
      {"#set" [1 2]} (sorted-set 2 1)
      {"a" 1 "b" 2} (sorted-map "b" 2 "a" 1)
      {"#clojure.map" [[[1 2] "bar"] [{"#clojure.keyword" "foo"} 42]]} {[1 2] "bar" :foo 42}
      {"#clojure.keyword" "foo/bar"} :foo/bar
      {"#clojure.symbol" "foo/bar"} 'foo/bar
      {"#clojure.list" ["foo" "bar"]} (list "foo" "bar"))))

(deftest decode-clojure-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      {[1 2] "bar" :foo 42} {"#clojure.map" [[[1 2] "bar"] [{"#clojure.keyword" "foo"} 42]]}
      :foo/bar {"#clojure.keyword" "foo/bar"}
      'foo/bar {"#clojure.symbol" "foo/bar"}
      (list "foo" "bar") {"#clojure.list" ["foo" "bar"]})
    (is (list? (deargus a {"#clojure.list" ["foo" "bar"]})))))

(deftest decode-clojure-types-failures
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      {"#clojure.map" "foo"} {"#clojure.map" "foo"}
      {"#clojure.keyword" 42} {"#clojure.keyword" 42}
      {"#clojure.symbol" 42} {"#clojure.symbol" 42}
      {"#clojure.list" 42} {"#clojure.list" 42})))

(defrecord CustomType [a b])

(def custom-encoder
  (juxt :a :b))

;; the following are explicit and verbose because thrown-with-msg? is doing something funny with a
;; class literal during macro expansion on the JVM
(deftest encoder-tag-invalid-unqualified
  (when-let [data (let [a (argus :encoders {CustomType (fn [o] {"#bar" (custom-encoder o)})})]
                    (-> (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                              #"invalid extension tag"
                              (enargus a (->CustomType 1 2)))
                          "should be qualified")
                      ex-data))]
    (is (= "#bar" (:tag data)) "should be qualified")))

(deftest encoder-tag-invalid-non-alphanumeric
  (when-let [data (let [a (argus :encoders {CustomType (fn [o] {"#foo bar" (custom-encoder o)})})]
                    (-> (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                              #"invalid extension tag"
                              (enargus a (->CustomType 1 2)))
                          "should be alphanumeric")
                      ex-data))]
    (is (= "#foo bar" (:tag data)) "should be alphanumeric")))

(deftest encoder-tag-invalid-vector-encoder
  (are [t msg]
    (when-let [data (-> (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                              #"invalid extension tag"
                              (argus :encoders {CustomType [t custom-encoder]}))
                          msg)
                      ex-data)]
      (= t (:tag data)))
    "foo.bar" "should start with octothorpe"
    "#bar" "should be qualified"
    "#foo bar" "should be alphanumeric"))

(deftest decoder-tag-invalid
  ;; we're more lenient in what we consume
  (when-let [data (-> (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                            #"invalid extension tag"
                            (argus :decoders {"foo.bar" map->CustomType}))
                        "should start with octothorpe")
                    ex-data)]
    (is (= "foo.bar" (:tag data)) "should start with octothorpe")))

(deftest encode-tagged-value
  (is (= {"#uuid" "62047d3a-112c-4458-900f-606f7fb66a20"}
        (enargus (argus) #uuid "62047d3a-112c-4458-900f-606f7fb66a20"))))

(deftest encode-with-builtin-tag
  (doseq [t ["#set" "#date" "#instant" "#uuid"]]
    (let [a (argus :encoders {CustomType [t custom-encoder]}
              :decoders {t (partial apply ->CustomType)})
          x (map->CustomType {})
          y (enargus a x)]
      (is (= {t [nil nil]} y)))))

(deftest decode-tagged-value
  (is (= #uuid "62047d3a-112c-4458-900f-606f7fb66a20"
        (deargus (argus) {"#uuid" "62047d3a-112c-4458-900f-606f7fb66a20"}))))

(deftest encode-custom-type
  (is (= {"#my.type" [1 2]}
        (enargus (argus :encoders {CustomType (fn [o] {"#my.type" (custom-encoder o)})})
          (->CustomType 1 2)))))

(deftest encode-custom-type-vector-encoder
  (is (= {"#my.type" [1 2]}
        (enargus (argus :encoders {CustomType ["#my.type" custom-encoder]}) (->CustomType 1 2)))))

(deftest decode-custom-type
  (is (= (->CustomType 1 2)
        (deargus (argus :decoders {"#my.type" (partial apply ->CustomType)}) {"#my.type" [1 2]}))))

(deftest encode-keyword-keys
  (is (= {":foo" "bar"} (enargus (argus) {:foo "bar"})))
  (is (= {":foo/baz" "bar"} (enargus (argus) {:foo/baz "bar"}))))

(deftest decode-keyword-keys
  (is (= {:foo "bar"} (deargus (argus) {":foo" "bar"})))
  (is (= {:foo/baz "bar"} (deargus (argus) {":foo/baz" "bar"}))))

(deftest encode-symbol-keys
  (is (= {"'foo" "bar"} (enargus (argus) {'foo "bar"})))
  (is (= {"'foo/baz" "bar"} (enargus (argus) {'foo/baz "bar"}))))

(deftest decode-symbol-keys
  (is (= {'foo "bar"} (deargus (argus) {"'foo" "bar"})))
  (is (= {'foo/baz "bar"} (deargus (argus) {"'foo/baz" "bar"}))))

(deftest encode-string-keys
  (is (= {"foo" "bar"} (enargus (argus) {"foo" "bar"})))
  (is (= {"::" "bar"} (enargus (argus) {":" "bar"})))
  (is (= {"::foo" "bar"} (enargus (argus) {":foo" "bar"})))
  (is (= {":::foo" "bar"} (enargus (argus) {"::foo" "bar"})))
  (is (= {"''" "bar"} (enargus (argus) {"'" "bar"})))
  (is (= {"''foo" "bar"} (enargus (argus) {"'foo" "bar"})))
  (is (= {"'''foo" "bar"} (enargus (argus) {"''foo" "bar"}))))

(deftest decode-string-keys
  (is (= {"foo" "bar"} (deargus (argus) {"foo" "bar"})))
  (is (= {":" "bar"} (deargus (argus) {"::" "bar"})))
  (is (= {":foo" "bar"} (deargus (argus) {"::foo" "bar"})))
  (is (= {"::foo" "bar"} (deargus (argus) {":::foo" "bar"})))
  (is (= {"'" "bar"} (deargus (argus) {"''" "bar"})))
  (is (= {"'foo" "bar"} (deargus (argus) {"''foo" "bar"})))
  (is (= {"''foo" "bar"} (deargus (argus) {"'''foo" "bar"}))))

(deftest encode-empty-string-key
  (is (= {"" "bar"} (enargus (argus) {"" "bar"}))))

(deftest decode-empty-string-key
  (is (= {"" "bar"} (deargus (argus) {"" "bar"}))))

(deftest encode-nested
  (is (= {"#my.type" {":a" {":c" 1 "::d" 2}
                      ":b" {"#uuid" "111c2350-fbb5-4988-824b-f15506e896b1"}}}
        (enargus (argus :encoders {CustomType ["#my.type" #(into {} %)]})
          (->CustomType {:c 1 ":d" 2}
            #uuid "111c2350-fbb5-4988-824b-f15506e896b1")))))

(deftest decode-nested
  (is (= (->CustomType {:c 1 ":d" 2} #uuid "111c2350-fbb5-4988-824b-f15506e896b1")
        (deargus (argus :decoders {"#my.type" map->CustomType})
          {"#my.type" {":a" {":c" 1 "::d" 2}
                       ":b" {"#uuid" "111c2350-fbb5-4988-824b-f15506e896b1"}}}))))

(deftest encode-missing-encoder
  (when-let [data (ex-data (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                                 #"missing encoder"
                                 (enargus (argus) (->CustomType 1 2)))))]
      (is (= CustomType (:type data)))))

(deftest decode-unknown-tag
  (is (= {"#unknown" {:foo #uuid "06ccea6d-0d8a-4f1f-a026-c30a3e12a481"}}
        (deargus (argus) {"#unknown" {":foo" {"#uuid" "06ccea6d-0d8a-4f1f-a026-c30a3e12a481"}}})))
  (is (= {:tag "#unknown"
          :value {:foo #uuid "06ccea6d-0d8a-4f1f-a026-c30a3e12a481"}}
        (deargus (argus :default-decoder (fn [t v] {:tag t :value v}))
          {"#unknown" {":foo" {"#uuid" "06ccea6d-0d8a-4f1f-a026-c30a3e12a481"}}}))))

(deftest decode-invalid-unqualified-tag
  ;; defining a new unqualified tag is not allowed, but sometimes people break then rules and we
  ;; should still be able to read their data even as we hold ourselves to a higher standard :)
  (is (= #{1} (deargus (argus :decoders {"#foo" set}) {"#foo" [1]}))))

(deftest custom-decoder-for-builtin-tag
  ;; overriding the decoder for a builtin tag is allowed
  (is (= [1] (deargus (argus :decoders {"#set" vec}) {"#set" [1]}))))

(deftest quote-non-tagged-value
  (is (= {"##general" {":type" "slack-channel"}}
        (enargus (argus) {"#general" {:type "slack-channel"}}))))

(deftest unquote-non-tagged-value
  (is (= {"#general" {:type "slack-channel"}}
        (deargus (argus) {"##general" {":type" "slack-channel"}}))))
