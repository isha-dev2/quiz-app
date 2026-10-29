# Quiz App — DevOps CI/CD

## Project Overview
This is an Android Quiz Application developed using Java and Gradle.

## DevOps Tools
- Git and GitHub for source code management
- GitHub Actions for CI/CD automation
- Gradle for building the Android application
- JUnit for unit testing

## CI/CD Pipeline
The GitHub Actions workflow is located at `.github/workflows/android-ci-cd.yml`.

Whenever code is pushed to GitHub, the workflow automatically:
1. Checks out the source code.
2. Sets up Java 17 and Gradle.
3. Builds the Android application and runs unit tests.
4. Generates a debug APK.
5. Publishes the APK as a downloadable workflow artifact after a successful push-triggered run.

## Build Command
```bash
./gradlew clean testDebugUnitTest assembleDebug
```

## Outcome
The pipeline automates the build and test process and publishes the generated APK through GitHub Actions.

