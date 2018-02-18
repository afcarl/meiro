(ns meiro.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [meiro.core :as meiro]
            [meiro.backtracker]
            [meiro.hunt-and-kill :as hunt]))

;;; Position Functions

(deftest adjacent-test
  (testing "true if cells are adjacent."
    (is (meiro/adjacent? [0 0] [0 1]))
    (is (meiro/adjacent? [0 0] [1 0]))
    (is (not (meiro/adjacent? [0 0] [0 2])))
    (is (not (meiro/adjacent? [0 0] [2 0])))))

(deftest direction-test
  (testing "Cardinal directions."
    (is (= :north (meiro/direction [2 3] [1 3])))
    (is (= :south (meiro/direction [3 1] [4 1])))
    (is (= :east (meiro/direction [5 1] [5 2])))
    (is (= :west (meiro/direction [4 3] [4 2]))))
  (testing "Not adjacent."
    (is (nil? (meiro/direction [0 0] [0 0])))
    (is (nil? (meiro/direction [0 0] [2 0])))
    (is (nil? (meiro/direction [0 0] [0 2])))))

(deftest cell-direction-test
  (testing "Methods for getting the cell in a given direction"
    (is (= [0 1] (meiro/north [1 1])))
    (is (= [2 1] (meiro/south [1 1])))
    (is (= [1 2] (meiro/east [1 1])))
    (is (= [1 0] (meiro/west [1 1])))))

(deftest pos-to-test
  (testing "Methods for getting the cell in a given direction"
    (is (= [0 1] (meiro/pos-to :north [1 1])))
    (is (= [2 1] (meiro/pos-to :south [1 1])))
    (is (= [1 2] (meiro/pos-to :east [1 1])))
    (is (= [1 0] (meiro/pos-to :west [1 1])))))

;;; Grid Functions

(deftest init-test
  (testing "First level is row, each row contains columns."
    (is (= 5 (count (meiro/init 5 3))))
    (is (every? #(= 4 (count %)) (meiro/init 5 4))))
  (testing "Create with value other than []."
    (is (= [[nil nil] [nil nil]] (meiro/init 2 2 nil)))
    (is (= [[0] [0] [0]] (meiro/init 3 1 0)))))

(deftest neighbors-test
  (testing "Get neighbors to a cell in a maze."
    (is (= #{[0 1] [1 0] [1 2] [2 1]}
           (set (meiro/neighbors (meiro/init 3 3) [1 1]))))
    (is (= #{[1 1] [0 0] [2 0]}
           (set (meiro/neighbors (meiro/init 3 3) [1 0]))))
    (is (= #{[0 0] [0 2] [1 1]}
           (set (meiro/neighbors (meiro/init 3 3) [0 1]))))
    (is (= #{[0 1] [1 0]}
           (set (meiro/neighbors (meiro/init 3 3) [0 0]))))
    (is (= #{[0 1] [1 2]}
           (set (meiro/neighbors (meiro/init 3 3) [0 2]))))
    (is (= #{[1 0] [2 1]}
           (set (meiro/neighbors (meiro/init 3 3) [2 0]))))
    (is (= #{[2 1] [1 2]}
           (set (meiro/neighbors (meiro/init 3 3) [2 2]))))))


;;; Maze Functions

(deftest empty-neighbor-test
  (testing "All neighbors are empty."
    (let [maze [[[] [] []] [[] [] []] [[] [] []]]]
      (is (= '([1 0] [2 1] [1 2] [0 1])
             (meiro/empty-neighbors maze [1 1])))
      (is (= '([0 0] [1 1] [2 0])
             (meiro/empty-neighbors maze [1 0])))
      (is (= '([2 1] [1 2])
             (meiro/empty-neighbors maze [2 2])))))
  (testing "No neighbors are empty."
    (let [maze [[[:south] [:south :east] [:west :south]]
                [[:north :south] [:south :north] [:north :south]]
                [[:north :east] [:west :north] [:north]]]]
      (is (empty? (meiro/empty-neighbors maze [1 1])))
      (is (empty? (meiro/empty-neighbors maze [1 0])))
      (is (empty? (meiro/empty-neighbors maze [0 2]))))))


(deftest link-with-test
  (testing "Adjacent cells linked by opposite directions."
    (let [above [2 2]
          below [3 2]
          link-fn (meiro/link-with meiro/direction)
          m (link-fn (meiro/init 6 4) below above)]
      (is (some (comp = :north) (get-in m below)))
      (is (some (comp = :south) (get-in m above))))
    (let [left [1 2]
          right [1 3]
          m (meiro/link (meiro/init 6 4) left right)]
      (is (some (comp = :east) (get-in m left)))
      (is (some (comp = :west) (get-in m right))))))


(deftest link-test
  (testing "Adjacent cells linked by opposite directions."
    (let [above [2 2]
          below [3 2]
          m (meiro/link (meiro/init 6 4) below above)]
      (is (some (comp = :north) (get-in m below)))
      (is (some (comp = :south) (get-in m above))))
    (let [left [1 2]
          right [1 3]
          m (meiro/link (meiro/init 6 4) left right)]
      (is (some (comp = :east) (get-in m left)))
      (is (some (comp = :west) (get-in m right))))))


(deftest dead-end-test
  (testing "No linked cell to the west"
    (let [maze [[[:south] [:south] [:east]
                 [:west :east] [:west :south] [:south] [:east]
                 [:west :south]]
                [[:north :east] [:north :west :south] [:east] [:west :east]
                 [:north :west :east] [:north :west :south] [:south]
                 [:north :south]]
                [[:east] [:north :west :east] [:west :east] [:west :east]
                 [:west :east] [:north :west :east] [:north :west :east]
                 [:north :west]]]]
      (is (= 8 (count (meiro/dead-ends maze)))))))


(deftest braid-test
  (let [maze (meiro.backtracker/create (meiro/init 15 20))]
    (testing "Braid a maze."
      (is (> (count (meiro/dead-ends maze))
             (count (meiro/dead-ends (meiro/braid maze)))))
      (is (zero? (count (meiro/dead-ends (meiro/braid maze 1.0))))))
    (testing "0.0 rate doesn't braid."
      (is (= (count (meiro/dead-ends maze))
             (count (meiro/dead-ends (meiro/braid maze 0.0))))))))


(deftest unlink-test
  (testing "Can unlink two cells from each other."
    (let [maze [[[:east :south] [:west :south]]]
          unlinked (meiro/unlink maze [0 0] [0 1])]
      (is (= [:south] (get-in unlinked [0 0])))
      (is (= [:south] (get-in unlinked [0 1])))))
  (testing "Dead ends are replace with :mask."
    (let [maze [[[:east] [:west]]]
          unlinked (meiro/unlink maze [0 0] [0 1])]
      (is (= [:mask] (get-in unlinked [0 0])))
      (is (= [:mask] (get-in unlinked [0 1]))))))


(deftest cull-test
  (let [maze (hunt/create (meiro/init 15 20))]
    (testing "Cull dead ends."
      (is (> (count (meiro/dead-ends maze))
             (count (meiro/dead-ends (meiro/cull maze))))))
    (testing "0.0 rate doesn't cull."
      (is (= (count (meiro/dead-ends maze))
             (count (meiro/dead-ends (meiro/cull maze 0.0))))))))
