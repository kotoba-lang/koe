(ns koe.session-test
  (:require [clojure.test :refer [deftest is testing]]
            [koe.ports :as p]
            [koe.session :as s]))

;; --- fixture ports (no real actor / socket / model) ---

(defrecord FixtureDialog []
  p/IDialog
  (step [_ state utterance]
    (if (re-find #"予約" utterance)
      {:reply "いつがよろしいですか" :action :book
       :state (assoc state :slot {:at "2026-07-01T10:00"})}
      {:reply "はい" :action nil :state state})))

(defrecord FixtureTTS []
  p/ITTS
  (synth [_ text _] {:audio/text text}))

(defrecord FixtureBooking [confirmed?]
  p/IBooking
  (propose [_ slot] {:proposal slot})
  (confirm [_ proposal sig] (reset! confirmed? true)
    {:booking proposal :signed-by sig}))

(defn ports [confirmed?]
  {:dialog (->FixtureDialog) :tts (->FixtureTTS) :booking (->FixtureBooking confirmed?)})

(deftest non-booking-turn-just-replies
  (testing "an utterance without booking intent synths a reply, no booking"
    (let [turn (s/converse (ports (atom false)) {} "もしもし")]
      (is (= "はい" (:reply turn)))
      (is (nil? (:action turn)))
      (is (nil? (:booking turn)))
      (is (s/booking-delegated? turn)))))

(deftest booking-turn-delegates-with-signature
  (testing "booking intent + member signature → delegated confirm (never server-side)"
    (let [confirmed? (atom false)
          turn (s/converse (ports confirmed?) {} "予約をお願いします" {:signature "did-sig"})]
      (is (= :book (:action turn)))
      (is (= {:at "2026-07-01T10:00"} (get-in turn [:booking :booking :proposal])))
      (is (= "did-sig" (get-in turn [:booking :signed-by])))
      (is (true? @confirmed?))
      (is (s/booking-delegated? turn)))))

(deftest booking-without-signature-holds-only
  (testing "no signature → slot is proposed but NOT confirmed (member-signed required)"
    (let [confirmed? (atom false)
          turn (s/converse (ports confirmed?) {} "予約をお願いします")]
      (is (= :book (:action turn)))
      (is (nil? (:booking turn)))
      (is (false? @confirmed?))
      (is (s/booking-delegated? turn)))))
