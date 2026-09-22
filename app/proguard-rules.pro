# Keep LiteRT/TFLite model artifacts.
-keep class org.tensorflow.lite.** { *; }

# Hilt generates Dagger components; keep them.
-keep class dagger.hilt.** { *; }