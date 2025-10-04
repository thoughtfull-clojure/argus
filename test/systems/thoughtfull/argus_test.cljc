(ns systems.thoughtfull.argus-test
  (:require
    [clojure.test :refer [are deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]]))

(defrecord CustomType [a b])

(def custom-encoder
  (juxt :a :b))

;; the following are explicit and verbose because thrown-with-msg? is doing something funny with a
;; class literal during macro expansion on the JVM
(deftest encoder-tag-invalid-missing-octothorpe
  (when-let [data (let [a (argus :encoders {CustomType (fn [o] {"foo.bar" (custom-encoder o)})})]
                    (-> (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                              #"invalid extension tag"
                              (enargus a (->CustomType 1 2)))
                          "should start with octothorpe")
                      ex-data))]
    (is (= "foo.bar" (:tag data)) "should start with octothorpe")))

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

(deftest encode-keyword-properties
  (is (= {"foo" "bar"} (enargus (argus) {"foo" "bar"})))
  (is (= {":foo" "bar"} (enargus (argus) {:foo "bar"})))
  (is (= {":foo/baz" "bar"} (enargus (argus) {:foo/baz "bar"})))
  (is (= {"::foo" "bar"} (enargus (argus) {":foo" "bar"})))
  (is (= {":::foo" "bar"} (enargus (argus) {"::foo" "bar"}))))

(deftest decode-keyword-properties
  (is (= {"foo" "bar"} (deargus (argus) {"foo" "bar"})))
  (is (= {:foo "bar"} (deargus (argus) {":foo" "bar"})))
  (is (= {:foo/baz "bar"} (deargus (argus) {":foo/baz" "bar"})))
  (is (= {":foo" "bar"} (deargus (argus) {"::foo" "bar"})))
  (is (= {"::foo" "bar"} (deargus (argus) {":::foo" "bar"}))))

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
