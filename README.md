# CreateProbabilityTuning

面向 Create（机械动力）的概率调校内容，维护模组代码、资源和构建配置。

## 工程布局

- Gradle 工程：[CreateProbabilityTuning](./CreateProbabilityTuning/)
- 工程详细说明：[CreateProbabilityTuning/README.md](./CreateProbabilityTuning/README.md)
- 项目注册信息：[PycodersMod.projects.json](../../PycodersMod.projects.json)
- 工作区统一测试入口：[launch.ps1](../../launch.ps1)
- 启动参数填写说明：[launch-parameters.md](../../launch-parameters.md)

## 构建

在 CreateProbabilityTuning 目录执行 .\gradlew.bat clean build。

工程使用 Gradle Java Toolchain 声明所需 Java 版本。机器本地的 JDK 路径位于工作区 local-config，不写入 Mod 仓库。

## 标识

| 项目 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.238 |
| Java | 21 |
| Mod ID | createprobabilitytuning |
| Java Package | com.pycoder.createprobabilitytuning |

Mod ID、Registry Namespace 和存档标识保持原值。工程目录、Gradle group、Java package 和 GitHub 仓库名属于工程组织信息。
## License

本项目采用 MIT License，详见 [LICENSE](./LICENSE)。

本仓库的 Gradle Wrapper 保留其随附的 Apache-2.0 许可，详见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。
