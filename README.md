# song

嵩。通过adb 分发文件到手机。

[![](https://jitpack.io/v/storytellerF/song.svg)](https://jitpack.io/#storytellerF/song)

```kotlin
song {
    //可选的。默认是adb，也就是说需要添加到path 或者添加android_home。否则需要指定
    adb = ""
    //可选的，需要发送的文件路径
    transfers = listOf(file("/test/hello.txt"))
    //可选的，需要发送的目录
    paths = listOf("/sdcard/Download/file/plugins")
    //可选的，发送到app 私有目录。自动在前面补全/data/data
    packages = listOf("com.test.test" to "file/plugins")
    //可选的，输出的文件名称，如果没有提供使用transfers 中的名称
    name = "yue.apk"
}
```
