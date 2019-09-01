# anti-xray
A mod to stop Earthcomputer's brother and others from xraying

## Installation
1. Download and run the [Fabric installer](https://fabricmc.net/use).
   - Click the "vanilla" button, leave the other settings as they are,
     and click "download installer".
   - Note: this step may vary if you aren't using the vanilla launcher
     or an old version of Minecraft.
1. Download anti-xray from the [releases page](https://github.com/Earthcomputer/anti-xray/releases)
   and move it to the mods folder (`.minecraft/mods`).

## Contributing
1. Clone the repository
   ```
   git clone https://github.com/Earthcomputer/anti-xray
   cd anti-xray
   ```
1. Generate the Minecraft source code
   ```
   ./gradlew genSources
   ```
   - Note: on Windows, use `gradlew` rather than `./gradlew`.
1. Generate the IDE project depending on which IDE you prefer
   ```
   ./gradlew idea      # For IntelliJ IDEA
   ./gradlew eclipse   # For Eclipse
   ```
1. Import the project in your IDE and edit the code
1. After testing in the IDE, build a JAR to test whether it works outside the IDE too
   ```
   ./gradlew build
   ```
   The mod JAR may be found in the `build/libs` directory
1. [Create a pull request](https://help.github.com/en/articles/creating-a-pull-request)
   so that your changes can be integrated into anti-xray
   - Note: for large contributions, create an issue before doing all that
     work, to ask whether your pull request is likely to be accepted