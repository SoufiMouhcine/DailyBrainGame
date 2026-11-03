
# Keep source line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable

# Keep Firebase model classes and their members
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Keep constructors used by Firebase serialization
-keepclassmembers class * {
    public <init>();
}
