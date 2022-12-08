# biometric

## 如何使用
可以直接作为submodule导入到项目中

settings.gradle中添加
```groovy
includeBuild("biometric") {
    dependencySubstitution {
        substitute module("com.jiaoay.biometric:core") using project(":biometric-core")
        substitute module("com.jiaoay.biometric:ui") using project(":biometric-ui")
    }
}
```
## 吐槽
说实话我不知道androidx biometric这个库是开发者处于一种什么样的精神状态写出来的
感觉一共没多麻烦的代码写的令人发指，我还以为里面有什么神奇的逻辑呢。结果发现全是和展示用的代码混在了一起，我真是草了
抽空自己弄弄好了，但是现在还没法看