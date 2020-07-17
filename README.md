Http Trapdoor
===

[ ![Download](https://api.bintray.com/packages/alvince-zy/android/http-trapdoor/images/download.svg?version=0.0.1) ](https://bintray.com/alvince-zy/android/http-trapdoor/0.0.1/link)

Dev tools that inspect http-requests with dynamic host switching which work on [OkHttp](https://github.com/square/okhttp)

Depandency
---

#### Import with gradle

Add into `build.gradle` of app module
```groovy
dependencies {
    …
    // [Required] core lib
    implementation "me.alvince.android.httptrapdoor:trapdoor:0.0.1"
    // [Optional] enable auto-config
    implementation "androidx.startup:startup-runtime:1.0.0-alpha01"
    // [Optional] use default floating-buoy
    implementation "me.alvince.android.httptrapdoor:trapdoor-buoy:0.0.1"
    …
}
```

Usage
---

```Kotlin
val trapdoor = Trapdoor.with(okhttpClient)
    // enable http request log default built-in
    .enableHttpLog()
val client = trapdoor.factory()

retrofit = Retrofit.Builder()
    .baseUrl(…)
    …
    // important, replace request call-factory
    .callFactory(client)
```

### 配置服务器列表

配置服务器列表（支持自动/手动配置）

#### 自动配置

```xml
<application>
    …
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${PACKAGE}.androidx-startup"
        android:exported="false">
        …
        <meta-data
            android:name="me.alvince.android.httptrapdoor.StartupInitializer"
            android:value="androidx.startup" />
        …
    </provider>
    …
</application>
```
app assets 目录下添加文件 `trapdoor_host_config.txt`  
每行为一个服务器信息，格式如下:  
```plain
{描述label},{tag},{host},{http|https}
```
示例：[sample](sample/src/main/assets/trapdoor_host_config.txt)

#### 手动配置

```Kotlin
// 对当前 trapdoor 实例配置服务器
trapdoor.customConfig(…)
```

### 切换服务器

```Kotlin
// 根据配置的服务器标签选择
trapdoor.select(hostTag)
```
选择服务器之后，由对应 `trapdoor` 驱动的网络请求会自动根据配置替换请求域名

### 悬浮指示器

```groovy
dependencies {
    implementation "me.alvince.android.httptrapdoor:trapdoor-buoy:0.0.1"
}
```

启用指示器
```Kotlin
trapdoor.enableFloatingBuoy(application)
```

启用后自动监听 `Activity`，悬浮指示器点击弹出服务器列表选择

LICENSE
---
```
MIT License

Copyright (c) 2020 Alvince

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
