
##---------------Begin: proguard configuration for Gson  ----------
-keepattributes Signature
# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson

-keep class com.stickers.bank.data.model.BankResponse { <fields>; }
-keep class com.stickers.bank.data.model.BaseDataResponse { <fields>; }
-keep class com.stickers.bank.data.model.DataResponse { <fields>; }
-keep class com.stickers.bank.data.model.FeaturedModel { <fields>; }
-keep class com.stickers.bank.data.model.FeaturedResponse { <fields>; }
-keep class com.stickers.bank.data.model.NewArrivalResponse{ <fields>; }
-keep class com.stickers.bank.data.model.RatioItem { <fields>; }
-keep class com.stickers.bank.data.model.Sticker { <fields>; }
-keep class com.stickers.bank.data.model.StickerApi { <fields>; }
-keep class com.stickers.bank.data.model.StickerBean { <fields>; }
-keep class com.stickers.bank.data.model.StickerModel { <fields>; }
-keep class com.stickers.bank.data.model.StickerPack { <fields>; }
-keep class com.stickers.bank.data.model.StickerPackApi { <fields>; }
-keep class com.stickers.bank.data.model.StickerResponse { <fields>; }
-keep class com.stickers.bank.data.model.User { <fields>; }


-keepclassmembers class com.stickers.bank.data.model.BankResponse { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.BaseDataResponse { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.DataResponse { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.FeaturedModel { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.FeaturedResponse { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.NewArrivalResponse{ <fields>; }
-keepclassmembers class com.stickers.bank.data.model.RatioItem { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.Sticker { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.StickerApi { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.StickerBean { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.StickerModel { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.StickerPack { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.StickerPackApi { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.StickerResponse { <fields>; }
-keepclassmembers class com.stickers.bank.data.model.User { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonDeserializer
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonArray
-keep class * extends com.google.gson.TypeAdapter

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

##---------------End: proguard configuration for Gson  ----------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

#-libraryjars /build/intermediates/pre-dexed/debug/gson-2.3-08958b96da94c86264ec30e35a9d524bac95d2df.jar
#-printmapping outputfile.txt
#-renamesourcefileattribute SourceFile


### Glide, Glide Okttp Module, Glide Transformations
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep,allowobfuscation interface com.google.gson.annotations.SerializedName


-keep class com.arthenica.mobileffmpeg.Config {
void log(long, int, byte[]);
void statistics(long, int, float, float, long , int, double, double);
}

-keep class com.arthenica.mobileffmpeg.AbiDetect {
}

-dontwarn com.android.org.conscrypt.SSLParametersImpl

-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.-dontwarn com.google.android.gms.ads.InterstitialAd
-dontwarn com.google.android.gms.ads.rewarded.RewardedAdCallback
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn com.google.android.gms.ads.InterstitialAd
-dontwarn com.google.android.gms.ads.query.AdInfo


-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
