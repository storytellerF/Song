# song

嵩。通过adb 分发文件到手机。

[![](https://jitpack.io/v/storytellerF/song.svg)](https://jitpack.io/#storytellerF/song)

```kotlin
song {
    //optional。默认是adb，也就是说需要添加到path。否则需要指定
    adb = ""
    dest = listOf("/sdcard/Download/file/plugins")
    //发送到app 私有目录。自动在前面补全/data/data
    packages = listOf("com.test.test" to "file/plugins")
    //输出的文件名称
    name = "yue.apk"
    //optional。默认返回true。
    device {
        true
    }
}
```
