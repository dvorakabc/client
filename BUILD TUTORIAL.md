Setup
Currently only JDK 8 is supported. You can download it for free here.

All following steps will require sh or bash on Linux / OSX, and Git Bash for Windows users.

You will have to install Git Bash on Windows, and Git on Linux / OSX first, refer to Google or Stackoverflow if you’re unsure how.

Once you have that setup, run the following:

git clone https://github.com/kami-blue/client kamiblue
cd kamiblue
./scripts/setupWorkspace.sh
You will want to replace https://github.com/kami-blue/client with the URL of your own fork, which you can make by clicking here.

Building
After setting up a workspace, you can run the gradle build task from within Intellij IDEA, or you can run ./gradlew build inside the KAMI Blue folder.

Running Minecraft
Only Intellij IDEA is supported, due to lack of features and proper Kotlin support in Eclipse. You’re free to use another IDE, but will not get support setting up the environment.

Once you have setup a workspace as per above, import the build.gradle file

File -> New -> Project from Existing Sources.

Select kamiblue, then the build.gradle file.

In the Gradle tab on the right, expand Run Configurations

Run genIntellijRuns, then press the reimport 🔄 button above.

You should see a RUNCLIENT at the top now, you can press the green ▶️ start button to start Minecraft.

If you do not see it, you can manually find it inside Gradle -> Tasks -> fg_runs -> runClient.