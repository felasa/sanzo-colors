# sanzo-colors

A JSON data set with the collection of colors and combinations from the book 
["A Dictionary of Color Combinations"](https://en.seigensha.com/books/978-4-86152-247-5/) originally 
compiled in digital form by [@dblodorn](https://github.com/dblodorn/sanzo-wada) and used for 
(yet another) online exploration of the book @ https://flasa.blog/app/swatches   

This data set incorporates better RGB from CMYK color translations as suggested by 
[@mattdesl](https://github.com/mattdesl/dictionary-of-colour-combinations) 
but uses JAVA's native `ICC_Profile` and `ICC_ColorSpace` built-in classes from `java.awt.color` and 
the script is writen in Clojure.

Color translations differ slightly from those mentioned but seem at a glance mostly equivalent.  

The dataset can be found in `out/sanzo-colors.json` and the script in `src/process_colors.clj`  

Process can be executed with `clojure -M -m process-colors <ouput-filename>".

