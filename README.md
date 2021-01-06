## easy_camera2

	对Camera2进行封装，极简使用方式，一行代码即可轻松使用！

------

### 特性

- 全Kotlin编写，使用Kotlin特性让代码更优雅！
- 模板化Camera2开发代码
- 极简使用方式
- 支持连拍
- 支持录像中拍照

### 如何使用：

```kotlin
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

```groovy
dependencies {
	 implementation 'com.github.XJ-Up:easy_camera2:V1.0.1'
}
```

### 关于使用方法

- 通过EasyCamera2 单例完成使用

### 例子

```kotlin
 button.setOnClickListener {
     //从当前Activity进入 EasyCamera2Activity
	 EasyCamera2.jumpActivity(this)
}
```

------

### 获取拍照图片生成地址（API28）

```kotlin
EasyCamera2.setImageOutMonitor {
    //这里的it即为拍照图片返回地址
     info.text = it
}
```

###  获取录像视频生成地址（API28）

```kotlin
EasyCamera2.setVideoOutMonitor {
     //这里的it即为录像视频返回地址
     info1.text = it
}
```



### 如上，即可完成EasyCamera2简单使用

# 后续内容持续更新。。。




