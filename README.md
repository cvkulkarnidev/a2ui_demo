# A2UI Android Renderer

A native Android proof-of-concept renderer for the **A2UI v0.9** protocol, implemented with Kotlin and Jetpack Compose.

## Current capabilities

- Parses A2UI JSONL, JSON arrays, and `{ "messages": [...] }` wrappers.
- Handles `createSurface`, `updateComponents`, `updateDataModel`, and `deleteSurface`.
- Maintains independent component and data-model state for each surface.
- Resolves literal values and JSON-pointer-style `{ "path": "/..." }` bindings.
- Renders `Text`, `Column`, `Row`, `Card`, `Button`, `TextField`, `CheckBox`, `Slider`, `Divider`, `Image`, and a basic `Icon` fallback.
- Produces A2UI v0.9 action payloads and includes the data model when `sendDataModel` is enabled.
- Includes an editable built-in sample for testing directly on a phone.

## Install on an Android device

### Download the CI-built APK

1. Open the repository's **Actions** tab.
2. Open the latest **Android APK** workflow run.
3. Download the `a2ui-android-debug` artifact.
4. Extract and install `app-debug.apk` on the Android device.

Android may ask you to allow installation from your browser or file manager.

### Run from Android Studio

1. Clone this repository.
2. Open it in a recent Android Studio release.
3. Let Gradle sync using JDK 17.
4. Connect a device with USB debugging enabled.
5. Run the `app` configuration.

## Using the playground

The app opens with a valid A2UI v0.9 JSONL example. Edit the messages and press **Render**. Pressing the rendered button displays the generated action payload at the bottom of the screen.

The renderer expects a component with ID `root` before displaying a surface.

## Scope

This is a working vertical slice, not yet a complete production renderer. The next milestones are dynamic list templates, complete layout semantics, two-way form bindings, validation functions, choice pickers, tabs/modals/media, streaming transports, schema validation, and a reusable renderer library module.

## Protocol source

Implementation follows the official A2UI renderer guidance and v0.9 server-to-client message schema from the `a2ui-project/a2ui` repository.
