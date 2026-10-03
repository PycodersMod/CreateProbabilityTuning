# CreateProbabilityTuning

## Supported Targets

<table>
<thead>
<tr><th>Loader</th><th>Minecraft</th></tr>
</thead>
<tbody>
<tr><td><a href="https://neoforged.net/">NeoForge</a></td><td><a href="https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-1">1.21.1</a></td></tr>
</tbody>
</table>

面向 Create（机械动力）的概率调校内容，维护模组代码、资源和构建配置。

## Project layout

The buildable project is in [$(@{Loader=neoforge; Version=1.21.1; Path=neoforge/1.21.1}.Path)/](neoforge/1.21.1/). Repository metadata remains at the root.

## Build

Run the Gradle wrapper from $(@{Loader=neoforge; Version=1.21.1; Path=neoforge/1.21.1}.Path)/:

``text
cd neoforge/1.21.1
./gradlew clean build
``

The target uses NeoForge for Minecraft 1.21.1. See the project directory for its Java and dependency requirements.
