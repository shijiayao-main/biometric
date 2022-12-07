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
