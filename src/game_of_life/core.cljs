(ns game-of-life.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [clojure.set :as set]
            [cljs.core.async :refer [<! chan close! put!]]
            [goog.dom :as dom]))

(defn set-up-canvas! [canvas w h]
  (let [ctx (.getContext canvas "2d")
        scale-factor (dom/getPixelRatio)]
    (set! (.-width canvas) (* w scale-factor))
    (set! (.-height canvas) (* h scale-factor))
    (set! (.-width (.-style canvas)) (str w "px"))
    (set! (.-height (.-style canvas)) (str h "px"))
    (set! (.-minWidth (.-style canvas)) (str w "px"))
    (set! (.-minHeight (.-style canvas)) (str h "px"))
    (.scale ctx scale-factor scale-factor)
    ctx))

(def fps 10)
(def animation-frame (fn [callback]
                       (js/setTimeout callback
                                      (/ 1000 fps)
                                      (.now js/performance))))

(defn big-bang [initial-state on-init on-update on-render stop?]
  (let [fps-channel (chan)]
    (go-loop [world-state (on-init initial-state)]
             (let [timestamp (<! fps-channel)]
               (on-render world-state)
               (if (stop? world-state)
                 (close! fps-channel)
                 (recur (on-update world-state)))))
    (animation-frame (fn tick [timestamp]
                       (when (put! fps-channel timestamp)
                         (animation-frame tick))))))

;; Drawing

(defn fill-style! [ctx color]
  (set! (.-fillStyle ctx) color))

(defn fill-rect [ctx x y w h & [color]]
  (.save ctx)
  (when color (fill-style! ctx color))
  (.fillRect ctx x y w h)
  (.restore ctx))

;; Conway's Game of Life

(defn moore-neighborhood [wrap-at [x y]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not= [dx dy] [0 0])]
    [(mod (+ x dx) wrap-at)
     (mod (+ y dy) wrap-at)]))

(defn step [wrap-at set-of-cells]
  (set
    (for [[cell count] (frequencies (mapcat (partial moore-neighborhood wrap-at) set-of-cells))
          :when (or (= 3 count)                             ; any cell (dead or alive) with three live neighbors lives
                    (and (contains? set-of-cells cell)      ; a living cell with two neighbors lives
                         (= 2 count)))]
      cell)))

(def add1 (partial + 1))
(def sub1 (partial + -1))

(defn glider [x y]
  #{[x (sub1 y)] [(add1 x) y] [(sub1 x) (add1 y)] [x (add1 y)] [(add1 x) (add1 y)]})

(defn blinker [x y]
  #{[(sub1 x) y] [x y] [(add1 x) y]})

(defn block [x y]
  #{[x y] [(add1 x) (add1 y)] [x (add1 y)] [(add1 x) y]})

(def objects (set/union
               (glider 2 2)
               (glider 6 6)
               (glider 10 10)
               (blinker 25 25)))

(big-bang
  {:number-of-cells 32
   :cell-width      10
   :cell-height     10
   :cell-padding    1
   :set-of-cells    objects
   :generations     0}

  (fn [world-state]
    (let [{:keys [number-of-cells cell-width cell-height]} world-state
          width (* number-of-cells cell-width)
          height (* number-of-cells cell-height)]
      (-> world-state
          (assoc :width width)
          (assoc :height height)
          (assoc :ctx (set-up-canvas! (.querySelector js/document "#c") width height)))))

  (fn [world-state]
    (-> world-state
        (update :set-of-cells (partial step (:number-of-cells world-state)))
        (update :generations inc)))

  (fn [world-state]
    (let [{:keys [ctx cell-width cell-height cell-padding width height]} world-state]
      (fill-rect ctx 0 0 width height "black")
      (doseq [[x y] (:set-of-cells world-state)]
        (fill-rect ctx
                   (+ (* cell-width x) cell-padding)
                   (+ (* cell-height y) cell-padding)
                   (- cell-width (* 2 cell-padding))
                   (- cell-height (* 2 cell-padding))
                   "white"))))

  (fn [world-state]
    (= (:generations world-state) (* 10 1000))))
