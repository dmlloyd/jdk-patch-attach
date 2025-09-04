## What it does

This utility creates a JAR called `jdk-patch-attach.jar` which can be used with the `--patch-module` command to perform a certain type of virtual thread experiment.

## How to use it

Run the main class in the JAR using the JDK you want to patch. The patch JAR will appear in the current working directory. Note that it will only work on the exact JDK used to produce it.
