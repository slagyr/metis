(ns metis.validators-spec
  (#+clj :require #+cljs :require-macros
                  [speclj.core :refer [describe context it should= should-throw should-not= should-be-nil should-not-be-nil with-all]])
  (:require [metis.validators :as validators :refer [contains presence acceptance confirmation numericality length inclusion exclusion formatted
                                                     #+clj email #+clj url #+clj satisfies-protocol]]
            [metis.support.addable :refer [new-dummyaddable Addable]]
            [speclj.core]))

(describe "validations"

  (context "with"
    (it "returns nil if the validation passes"
      (should= nil (validators/with {} nil {:validator (fn [attrs] true)})))

    (it "returns an error message if the validation fails"
      (should-not= nil (validators/with {} nil {:validator (fn [attrs] false)})))

    (it "throws an exception if validator is not given"
      (let [message "some error message"]
        (should-throw #+clj Exception #+cljs js/Error (validators/with nil {} {}))))

    )

  (context "contains"
    (it "passes when the attribute is present"
      (should= nil (contains {:foo "here!"} :foo {})))

    (it "passes when the key is in the map, but nil"
      (should= nil (contains {:foo nil} :foo {})))

    (it "fails when the key is not present"
      (should-not= nil (contains {} :foo {})))

    )

  (context "presence"
    (it "passes when the attribute is present"
      (should= nil (presence {:foo "here!"} :foo {})))

    (it "fails when attribute is nil"
      (should-not= nil (presence {:foo nil} :foo {})))

    (it "fails when attribute is an empty string"
      (should-not= nil (presence {:foo ""} :foo {})))

    (it "fails when attribute is an empty collection"
      (should-not= nil (presence {:foo []} :foo {})))
    )

  #+clj
  (context "satisfies?"
    (it "passes when type implements addable"
      (should-be-nil (satisfies-protocol {:thing (new-dummyaddable)} :thing {:protocol Addable})))

    (it "fails when type does not implement addable"
      (let [expected-error "must satisfy protocol #'metis.support.addable/Addable"]
        (should= expected-error (satisfies-protocol {:thing 22} :thing {:protocol Addable}))))
    )

  (context "acceptance"
    (it "passes when accepted"
      (should= nil (acceptance {:foo "1"} :foo {})))

    (it "fails when not accepted"
      (should-not= nil (acceptance {:foo ""} :foo {})))

    (it "passes with customer accept"
      (should= nil (acceptance {:foo "yes"} :foo {:accept "yes"})))
    )

  (context "confirmation"
    (with-all email-to-validate "snap.into@slim.jim")
    (it "passes when confirmation is equal"
      (should= nil (confirmation {:email @email-to-validate :email-confirmation @email-to-validate} :email {})))

    (it "fails when confirmation is not equal"
      (should-not= nil (confirmation {:email @email-to-validate :email-confirmation "something else"} :email {})))

    (it "passes with custom confirmation attribute"
      (should= nil (confirmation {:email @email-to-validate :some-attr @email-to-validate} :email {:confirm :some-attr})))

    )

  (context "numericality"

    (context "number"
      (context "passes when the attribute is a"

        (it "string that represents an integer"
          (should= nil (numericality {:foo "1"} :foo {})))

        (it "integer"
          (should= nil (numericality {:foo 1} :foo {})))

        (it "string that represents a float"
          (should= nil (numericality {:foo "1.0"} :foo {})))

        (it "float"
          (should= nil (numericality {:foo 1.0} :foo {})))

        )

      (it "fails when the attribute is not a number"
        (should-not= nil (numericality {:foo "asdf"} :foo {}))
        (should= "some message" (numericality {:foo "asdf"} :foo {:is-not-a-number "some message"})))

      )

    (context "only-integer"
      (it "passes when the number is an integer"
        (should= nil (numericality {:foo "1"} :foo {:only-integer true}))
        (should= nil (numericality {:foo 1} :foo {:only-integer true})))

      (it "fails when the number is a float"
        #+clj (should-not= nil (numericality {:foo "1.0"} :foo {:only-integer true}))
        (should-not= nil (numericality {:foo "1.1"} :foo {:only-integer true}))
        #+clj (should-not= nil (numericality {:foo 1.0} :foo {:only-integer true}))
        (should-not= nil (numericality {:foo 1.1} :foo {:only-integer true})))

      (it "returns the is-not-an-int error message upon failure"
        (should= "some message" (numericality {:foo "1.1"} :foo {:only-integer true :is-not-an-int "some message"})))

      )

    (context "greater-than"
      (it "passes when the number is greater than"
        (should= nil (numericality {:foo "1"} :foo {:greater-than 0}))
        (should= nil (numericality {:foo 1} :foo {:greater-than 0}))
        (should= nil (numericality {:foo "1.0"} :foo {:greater-than 0}))
        (should= nil (numericality {:foo 1.0} :foo {:greater-than 0})))

      (it "fails when the number is equal"
        (should-not= nil (numericality {:foo "1"} :foo {:greater-than 1}))
        (should-not= nil (numericality {:foo 1} :foo {:greater-than 1}))
        (should-not= nil (numericality {:foo "1.0"} :foo {:greater-than 1}))
        (should-not= nil (numericality {:foo 1.0} :foo {:greater-than 1})))

      (it "fails when the number is less than"
        (should-not= nil (numericality {:foo "0"} :foo {:greater-than 1}))
        (should-not= nil (numericality {:foo 0} :foo {:greater-than 1}))
        (should-not= nil (numericality {:foo "0.9"} :foo {:greater-than 1}))
        (should-not= nil (numericality {:foo 0.9} :foo {:greater-than 1})))

      (it "returns the is-not-greater-than error message upon failure"
        (should= "some 0" (numericality {:foo "0"} :foo {:greater-than 0 :is-not-greater-than "some %d"}))
        #+clj (should= "some 0.0" (numericality {:foo "0"} :foo {:greater-than 0.0 :is-not-greater-than "some %s"}))
        )

      )

    (context "greater-than-or-equal-to"
      (it "passes when the number is greater than"
        (should= nil (numericality {:foo "2"} :foo {:greater-than-or-equal-to 1}))
        (should= nil (numericality {:foo 2} :foo {:greater-than-or-equal-to 1}))
        (should= nil (numericality {:foo "1.1"} :foo {:greater-than-or-equal-to 1}))
        (should= nil (numericality {:foo 1.1} :foo {:greater-than-or-equal-to 1})))

      (it "passes when the number is equal"
        (should= nil (numericality {:foo "1"} :foo {:greater-than-or-equal-to 1}))
        (should= nil (numericality {:foo 1} :foo {:greater-than-or-equal-to 1}))
        (should= nil (numericality {:foo "1.0"} :foo {:greater-than-or-equal-to 1}))
        (should= nil (numericality {:foo 1.0} :foo {:greater-than-or-equal-to 1})))

      (it "fails when the number is less than"
        (should-not= nil (numericality {:foo "0"} :foo {:greater-than-or-equal-to 1}))
        (should-not= nil (numericality {:foo 0} :foo {:greater-than-or-equal-to 1}))
        (should-not= nil (numericality {:foo "0.9"} :foo {:greater-than-or-equal-to 1}))
        (should-not= nil (numericality {:foo 0.9} :foo {:greater-than-or-equal-to 1})))

      (it "returns the is-not-greater-than-or-equal-to error message upon failure"
        (should= "some 1" (numericality {:foo "0"} :foo {:greater-than-or-equal-to 1 :is-not-greater-than-or-equal-to "some %s"})))

      )

    (context "equal-to"
      (it "fails when the number is greater than"
        (should-not= nil (numericality {:foo "2"} :foo {:equal-to 1}))
        (should-not= nil (numericality {:foo 2} :foo {:equal-to 1}))
        (should-not= nil (numericality {:foo "1.1"} :foo {:equal-to 1}))
        (should-not= nil (numericality {:foo 1.1} :foo {:equal-to 1})))

      (it "passes when the number is equal"
        (should= nil (numericality {:foo "1"} :foo {:equal-to 1}))
        (should= nil (numericality {:foo 1} :foo {:equal-to 1}))
        (should= nil (numericality {:foo "1.0"} :foo {:equal-to 1}))
        (should= nil (numericality {:foo 1.0} :foo {:equal-to 1})))

      (it "fails when the number is less than"
        (should-not= nil (numericality {:foo "0"} :foo {:equal-to 1}))
        (should-not= nil (numericality {:foo 0} :foo {:equal-to 1}))
        (should-not= nil (numericality {:foo "0.9"} :foo {:equal-to 1}))
        (should-not= nil (numericality {:foo 0.9} :foo {:equal-to 1})))

      (it "returns the is-not-equal-to error message upon failure"
        (should= "some 1" (numericality {:foo "0"} :foo {:equal-to 1 :is-not-equal-to "some %d"})))

      )

    (context "not-equal-to"
      (it "passes when the number is greater than"
        (should= nil (numericality {:foo "2"} :foo {:not-equal-to 1}))
        (should= nil (numericality {:foo 2} :foo {:not-equal-to 1}))
        (should= nil (numericality {:foo "1.1"} :foo {:not-equal-to 1}))
        (should= nil (numericality {:foo 1.1} :foo {:not-equal-to 1})))

      (it "fails when the number is equal"
        (should-not= nil (numericality {:foo "1"} :foo {:not-equal-to 1}))
        (should-not= nil (numericality {:foo 1} :foo {:not-equal-to 1}))
        (should-not= nil (numericality {:foo "1.0"} :foo {:not-equal-to 1}))
        (should-not= nil (numericality {:foo 1.0} :foo {:not-equal-to 1})))

      (it "passes when the number is less than"
        (should= nil (numericality {:foo "0"} :foo {:not-equal-to 1}))
        (should= nil (numericality {:foo 0} :foo {:not-equal-to 1}))
        (should= nil (numericality {:foo "0.9"} :foo {:not-equal-to 1}))
        (should= nil (numericality {:foo 0.9} :foo {:not-equal-to 1})))

      (it "returns the is-equal-to error message upon failure"
        (should= "some 1" (numericality {:foo "1"} :foo {:not-equal-to 1 :is-equal-to "some %d"})))

      )

    (context "less-than"
      (it "fails when the number is greater than"
        (should-not= nil (numericality {:foo "2"} :foo {:less-than 1}))
        (should-not= nil (numericality {:foo 2} :foo {:less-than 1}))
        (should-not= nil (numericality {:foo "1.1"} :foo {:less-than 1}))
        (should-not= nil (numericality {:foo 1.1} :foo {:less-than 1})))

      (it "fails when the number is equal"
        (should-not= nil (numericality {:foo "1"} :foo {:less-than 1}))
        (should-not= nil (numericality {:foo 1} :foo {:less-than 1}))
        (should-not= nil (numericality {:foo "1.0"} :foo {:less-than 1}))
        (should-not= nil (numericality {:foo 1.0} :foo {:less-than 1})))

      (it "passes when the number is less than"
        (should= nil (numericality {:foo "0"} :foo {:less-than 1}))
        (should= nil (numericality {:foo 0} :foo {:less-than 1}))
        (should= nil (numericality {:foo "0.9"} :foo {:less-than 1}))
        (should= nil (numericality {:foo 0.9} :foo {:less-than 1})))

      (it "returns the is-equal-to error message upon failure"
        (should= "some 1" (numericality {:foo "1"} :foo {:less-than 1 :is-not-less-than "some %d"})))

      )

    (context "less-than-or-equal-to"
      (it "fails when the number is greater than"
        (should-not= nil (numericality {:foo "2"} :foo {:less-than-or-equal-to 1}))
        (should-not= nil (numericality {:foo 2} :foo {:less-than-or-equal-to 1}))
        (should-not= nil (numericality {:foo "1.1"} :foo {:less-than-or-equal-to 1}))
        (should-not= nil (numericality {:foo 1.1} :foo {:less-than-or-equal-to 1})))

      (it "fails when the number is equal"
        (should= nil (numericality {:foo "1"} :foo {:less-than-or-equal-to 1}))
        (should= nil (numericality {:foo 1} :foo {:less-than-or-equal-to 1}))
        (should= nil (numericality {:foo "1.0"} :foo {:less-than-or-equal-to 1}))
        (should= nil (numericality {:foo 1.0} :foo {:less-than-or-equal-to 1})))

      (it "passes when the number is less than"
        (should= nil (numericality {:foo "0"} :foo {:less-than-or-equal-to 1}))
        (should= nil (numericality {:foo 0} :foo {:less-than-or-equal-to 1}))
        (should= nil (numericality {:foo "0.9"} :foo {:less-than-or-equal-to 1}))
        (should= nil (numericality {:foo 0.9} :foo {:less-than-or-equal-to 1})))

      (it "returns the is-not-less-than-or-equal-to error message upon failure"
        (should= "some 1" (numericality {:foo "2"} :foo {:less-than-or-equal-to 1 :is-not-less-than-or-equal-to "some %d"})))

      )

    (context "odd"
      (it "passes when the number is odd"
        (should= nil (numericality {:foo "1"} :foo {:odd true}))
        (should= nil (numericality {:foo 1} :foo {:odd true}))
        (should= nil (numericality {:foo "1.0"} :foo {:odd true}))
        (should= nil (numericality {:foo 1.0} :foo {:odd true})))

      (it "fails when the number is even"
        (should-not= nil (numericality {:foo "2"} :foo {:odd true}))
        (should-not= nil (numericality {:foo 2} :foo {:odd true}))
        (should-not= nil (numericality {:foo "2.0"} :foo {:odd true}))
        (should-not= nil (numericality {:foo 2.0} :foo {:odd true})))

      (it "returns the is-not-odd error message upon failure"
        (should= "some" (numericality {:foo "2"} :foo {:odd true :is-not-odd "some"})))

      )

    (context "even"
      (it "fails when the number is odd"
        (should-not= nil (numericality {:foo "1"} :foo {:even true}))
        (should-not= nil (numericality {:foo 1} :foo {:even true}))
        (should-not= nil (numericality {:foo "1.0"} :foo {:even true}))
        (should-not= nil (numericality {:foo 1.0} :foo {:even true})))

      (it "passes when the number is even"
        (should= nil (numericality {:foo "2"} :foo {:even true}))
        (should= nil (numericality {:foo 2} :foo {:even true}))
        (should= nil (numericality {:foo "2.0"} :foo {:even true}))
        (should= nil (numericality {:foo 2.0} :foo {:even true})))

      (it "returns the is-not-even error message upon failure"
        (should= "some" (numericality {:foo "1"} :foo {:even true :is-not-even "some"})))

      )

    (context "in"
      (it "passes when the number is in the collection"
        (should= nil (numericality {:foo "5"} :foo {:in (range 5 7)}))
        (should= nil (numericality {:foo "6"} :foo {:in (range 5 7)})))

      (it "fails when the number is not in the collection"
        (should-not= nil (numericality {:foo "4"} :foo {:in (range 5 7)}))
        (should-not= nil (numericality {:foo "7"} :foo {:in (range 5 7)})))

      (it "returns the is-in error message upon failure"
        (should= "some" (numericality {:foo "5"} :foo {:not-in (range 5 7) :is-in "some"})))

      )

    (context "not in"
      (it "fails when the number is in the collection"
        (should-not= nil (numericality {:foo "5"} :foo {:not-in (range 5 7)}))
        (should-not= nil (numericality {:foo 5} :foo {:not-in (range 5 7)}))
        (should-not= nil (numericality {:foo 5.0} :foo {:not-in (range 5 7)}))
        (should-not= nil (numericality {:foo "5.0"} :foo {:not-in (range 5 7)})))

      (it "passes when the number is not in the collection"
        (should= nil (numericality {:foo "7"} :foo {:not-in (range 5 7)}))
        (should= nil (numericality {:foo 7} :foo {:not-in (range 5 7)}))
        (should= nil (numericality {:foo "7.0"} :foo {:not-in (range 5 7)}))
        (should= nil (numericality {:foo 7.0} :foo {:not-in (range 5 7)})))

      (it "returns the is-in error message upon failure"
        (should= "some" (numericality {:foo "4"} :foo {:in (range 5 7) :is-not-in "some"})))

      )

    )

  (context "length"
    (it "passes if the length is equal to"
      (should= nil (length {:foo "1234"} :foo {:equal-to 4})))

    (it "fails if the length is not equal to"
      (should-not= nil (length {:foo "123"} :foo {:equal-to 4})))

    )

  (context "inclusion"
    (it "passes when the item is in the collection"
      (should= nil (inclusion {:foo "1"} :foo {:in ["1" "2" "3" "4"]})))

    (it "fails when the item is not in the collection"
      (should-not= nil (inclusion {:foo "5"} :foo {:in ["1" "2" "3" "4"]})))

    )

  (context "exclusion"
    (it "passes when the item is not in the collection"
      (should= nil (exclusion {:foo "5"} :foo {:from ["1" "2" "3" "4"]})))

    (it "fails when the item is in the collection"
      (should-not= nil (exclusion {:foo "1"} :foo {:from ["1" "2" "3" "4"]})))

    )

  (context "formatted"
    (it "fails if attr is nil"
      (should-not= nil (formatted {:foo nil} :foo {:pattern #""})))

    (it "passes if the pattern matches"
      (should= nil (formatted {:foo "a"} :foo {:pattern #"a"})))

    )

  #+clj
  (context "email"
    (it "passes for valid email"
      (should= nil (email {:foo "snap.into@slim.io"} :foo {}))
      (should= nil (email {:foo "s.n.a.p.i.n.t.o@s.l.i.m.j.i.com"} :foo {}))
      (should= nil (email {:foo "come@me.io"} :foo {}))
      (should= nil (email {:foo "COME@me.io"} :foo {}))
      (should= nil (email {:foo "COME.ME@ME.io"} :foo {}))
      (should= nil (email {:foo "COME.ME@ME.io"} :foo {})))

    (it "fails for invalid email"
      (should-not= nil (email {:foo "snap@into@slim.jim"} :foo {})))

    )

  #+clj
  (context "url"

    (it "passes for a valid url"
      (should-be-nil (url {:foo "http://google.com"} :foo {}))
      (should-be-nil (url {:foo "https://google.com"} :foo {}))
      (should-be-nil (url {:foo "https://google.com/some-path//"} :foo {:allow-two-slashes true}))
      (should-be-nil (url {:foo "unknown://google.com/some-path//"} :foo {:schemes ["unknown"] :allow-two-slashes true}))
      (should-be-nil (url {:foo "unknown://google.com/"} :foo {:allow-all-schemes true}))
      (should-be-nil (url {:foo "http://google.com#view=fitb"} :foo {}))
      (should-be-nil (url {:foo "http://localhost/"} :foo {:allow-local-urls true}))
      )

    (it "fails for an invalid url"
      (should-not-be-nil (url {:foo "unknown://foo.bar.com/"} :foo {}))
      (should-not-be-nil (url {:foo "http://google.com"} :foo {:schemes ["https"]}))
      (should-not-be-nil (url {:foo "http://google.com#view=fitb"} :foo {:no-fragments true}))
      (should-not-be-nil (url {:foo "http://localhost/"} :foo {})))

    )

  )
