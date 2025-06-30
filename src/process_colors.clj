(ns process-colors
  "A simple script to process swatches into a single data file and include RGB conversions"
  (:require [clojure.data.json :as json] 
            [clojure.java.io :as io]
            [clojure.math :as math]))

(defn make-path 
  [swatch]
  (str "resources/swatches/" swatch ".json"))

(defn read-swatch
  [swatch]
  (get (json/read (io/reader (make-path swatch)))
       swatch))

(defn mutate
  "Adds a constant key vals to a collection of maps xrel" 
  ([xrel key val] (map #(assoc % key val) xrel))
  ([xrel key val & kvs]
   (let [ret (mutate xrel key val)]
     (if kvs 
       (if (next kvs)
         (recur ret (first kvs) (second kvs) (nnext kvs))
         (throw (IllegalArgumentException. "Must be key val pairs")))
       ret))))       

(defn rgb->hex
  "Takes base 256  (r)ed, (g)reen and (b)lue values and returns
   hex tag"
  [r g b]
  (format "#%02x%02x%02x" r g b)) 

(defn cmyk->rgb 
  "Converts c m y k to [r g b] using a default icc colorspace. Unit base. 
  Perceptual match. An alternative for relative colorimetry is provided below"
  [c m y k]
  (let [cspace (new java.awt.color.ICC_ColorSpace 
                  (. java.awt.color.ICC_Profile getInstance 
                     "resources/profiles/US_Web_Coated_SWOP_v2.icc"))
        cmyk-arr (float-array [c m y k])]
    (vec (. cspace toRGB cmyk-arr)))) 

(defn rc-cmyk->rgb 
  "Converts c m y k to [r g b] using two default icc colorspaces. Unit base.
   relative colorimetry using CIEXYZ space between profiles"
  [c m y k]
  (let [o-space (new java.awt.color.ICC_ColorSpace 
                   (. java.awt.color.ICC_Profile getInstance 
                      "resources/profiles/US_Web_Coated_SWOP_v2.icc"))
        d-space  (new java.awt.color.ICC_ColorSpace 
                   (. java.awt.color.ICC_Profile getInstance 
                      "resources/profiles/sRGB_IEC61966-2.1.icc"))
        cmyk-arr (float-array [c m y k])
        o-cieXYZ (. o-space toCIEXYZ cmyk-arr)]
    (vec (. d-space fromCIEXYZ o-cieXYZ)))) 

(defn rgb->hsb 
  "RGB base 255 to HSB"
  [r g b]
  (let [hsb_b1 (. java.awt.Color RGBtoHSB r g b nil)]
    (->> hsb_b1 (mapv * [359 100 100]) (mapv math/round))))

(comment
  (cmyk->rgb    0.2 0.4 0.9 0.15)  ;; [0.70666057 0.5323262 0.2054322]
  (rc-cmyk->rgb 0.2 0.4 0.9 0.15) ;; [0.71459526 0.5489586 0.2640421]
  (rgb->hsb 255 20 60) ;; [239 67 24]
  (java.awt.Color. 1 0 0)
  (let [cs (new java.awt.color.ICC_ColorSpace 
                (. java.awt.color.ICC_Profile getInstance 
                   "resources/profiles/sRGB_IEC61966-2.1.icc"))]
    (apply (java.awt.Color) (. cs toRGB (float-array [1 0 0]))))
  (. java.awt.Color RGBtoHSB 255 0 0 nil))

(defn add-rgb
  "Adds rgb info to a map"
  [kv]
  (let [cmyk (map #(/ % 100) (kv "cmyk"))
        rgb (map #(math/round (* 255 %)) (apply cmyk->rgb cmyk))
        hex (apply rgb->hex rgb)
        hsb (apply rgb->hsb rgb)]
    (-> (assoc kv "rgb" (vec rgb) "hex" hex)
        (assoc "hsb" hsb))))

(def colors_l0
  (->> ["swatches_a" "swatches_b" "swatches_c"
        "swatches_d" "swatches_e" "swatches_f"]
       (map read-swatch)
       (map-indexed #(mutate %2 "id_swatch" (inc %1)))
       (reduce concat)
       (map-indexed #(assoc %2 "id_color" (inc %1))) 
       (map add-rgb)))

(defn get-combo-info
  [combo-id]
  (let [colors (filter (fn [c] (contains? (set (c "combinations")) combo-id)) colors_l0)]
    {"id_combo" combo-id
     "id_colors" (mapv #(% "id_color") colors)
     "hex" (mapv #(% "hex") colors)
     "hsb" (mapv #(% "hsb") colors)}))
     
(def combos (for [combo_id (range 1 349)]
              (get-combo-info combo_id)))

(def data {"colors" colors_l0 "combos" combos})

(defn write! 
  [filename] 
  (spit (str "out/" filename)
        (json/write-str data {:indent true :escape-slash false})))

(comment
  (write! "sanzo-colors_hsb.json"))

(defn -main
  "Invoke me with clojure -M -m process-colors filename"
  [filename]
  (write! filename))

