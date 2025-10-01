<p align="center">
  <a href="https://github.com/D4vidDf/PillPal">
    <img src="https://github.com/D4vidDf/PillPal/blob/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp?raw=true" alt="PillPal Icon" width="128">
  </a>
</p>

<h1 align="center">PillPal</h1>

<p align="center">
  <img alt="Version 0.6.0.16" src="https://img.shields.io/badge/version-0.6.0.16-blue"/>
</p>

## Description

PillPal is a comprehensive medication management app that helps users stay on top of their health regimen. It allows for easy scheduling of medications, setting reminders, and tracking intake. With a user-friendly interface, PillPal simplifies medication management. The app now integrates with Health Connect to provide a holistic view of your health by syncing vital signs, and includes a companion Wear OS app for convenient access to your medication schedule on the go.

## Features

*   **Medication Management:** Easily add, edit, and manage your medication schedules.
*   **Reminders:** Set customizable reminders to ensure you never miss a dose.
*   **Intake Tracking:** Log your medication intake with a single tap.
*   **User-Friendly Interface:** A clean and intuitive design for a seamless user experience.
*   **Health Connect Integration:** Sync and visualize your health data for a complete overview.
    *   **Heart Rate:** Monitor your heart rate trends alongside your medication schedule.
    *   **Water Intake:** Keep track of your hydration levels, which is crucial for overall health and medication efficacy.
    *   **Weight:** Log and track your weight to observe how it correlates with your treatment plan.
    *   **Body Temperature:** Monitor your body temperature to keep an eye on your well-being.
*   **Wear OS Companion App:** Receive reminders and log your medication intake directly from your smartwatch.

## Development Environment Setup

1. Clone the repository:
   ```sh
   git clone https://github.com/D4vidDf/PillPal.git
   ```
2. Open the project in Android Studio.
3. Ensure you have the latest version of Android Studio and the necessary SDKs installed.

## Building and Running the Application

1. Open the project in Android Studio.
2. Connect an Android device or start an emulator.
3. Click on the "Run" button or use the shortcut `Shift + F10` to build and run the application.

## Contributing

### Contributing via cloning and pull requests

1. Clone the repository:
   ```sh
   git clone https://github.com/D4vidDf/PillPal.git
   ```
2. Create a new branch for your feature or bug fix:
   ```sh
   git checkout -b feature/your-feature-name
   ```
3. Make your changes and commit them with a descriptive message:
   ```sh
   git add .
   git commit -m "Add a descriptive commit message"
   ```
4. Push your branch to your forked repository:
   ```sh
   git push origin feature/your-feature-name
   ```
5. Create a pull request from your branch to the main repository.

### Contributing via issues and patches

1. Open an issue in the repository to discuss the changes you want to make.
2. Once the issue is approved, clone the repository:
   ```sh
   git clone https://github.com/D4vidDf/PillPal.git
   ```
3. Create a new branch for your feature or bug fix:
   ```sh
   git checkout -b feature/your-feature-name
   ```
4. Make your changes and commit them with a descriptive message:
   ```sh
   git add .
   git commit -m "Add a descriptive commit message"
   ```
5. Generate a patch file:
   ```sh
   git format-patch origin/main --stdout > your-feature-name.patch
   ```
6. Attach the patch file to the issue for review and merging.

### Contributing via email

1. Open an issue in the repository to discuss the changes you want to make.
2. Once the issue is approved, clone the repository:
   ```sh
   git clone https://github.com/D4vidDf/PillPal.git
   ```
3. Create a new branch for your feature or bug fix:
   ```sh
   git checkout -b feature/your-feature-name
   ```
4. Make your changes and commit them with a descriptive message:
   ```sh
   git add .
   git commit -m "Add a descriptive commit message"
   ```
5. Generate a patch file:
   ```sh
   git format-patch origin/main --stdout > your-feature-name.patch
   ```
6. Send the patch file via email to the repository maintainer for review and merging.

## Code Style and Formatting

* Follow the Kotlin coding conventions as outlined in the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
* Use consistent indentation (4 spaces) and avoid using tabs.
* Ensure that all code is properly formatted before committing. You can use tools like `ktlint` or `detekt` to enforce code style.
* Write clear and concise commit messages that describe the changes made.

## Testing and Quality Assurance

* Write unit tests for all new features and bug fixes using `JUnit`.
* Ensure all tests pass before submitting a pull request.
* Use `Mockito` for mocking dependencies in unit tests.
* Perform manual testing on different devices and Android versions to ensure compatibility.

## Documentation and Comments

* Document all public classes and methods using KDoc comments.
* Provide clear and concise explanations for complex code sections.
* Update the `README.md` file with any new features or changes.
* Include usage examples and code snippets in the documentation to help users understand how to use the app.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.