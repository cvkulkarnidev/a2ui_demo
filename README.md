# A2UI Android Renderer

A native Android proof-of-concept renderer for the **A2UI v0.9 / v0.9.1** protocol family, implemented with Kotlin and Jetpack Compose.

## Current capabilities

- Runs a local **LLM -> A2UI -> renderer** pipeline demo.
- Parses A2UI JSONL, JSON arrays, and `{ "messages": [...] }` wrappers.
- Handles `createSurface`, `updateComponents`, `updateDataModel`, and `deleteSurface`.
- Maintains independent component and data-model state for each surface.
- Resolves literal values and JSON Pointer `{ "path": "/..." }` bindings, including scoped bindings for simple list templates.
- Renders the official basic catalog set used by the reference renderers: `Text`, `Image`, `Icon`, `Row`, `Column`, `List`, `Card`, `Tabs`, `Modal`, `Divider`, `Button`, `TextField`, `CheckBox`, `ChoicePicker`, `Slider`, and `DateTimeInput`.
- Keeps Android-specific extension components for richer local demos: `Chart`, `Chip`, `ProgressBar`, and `Metric`.
- Supports chart interconversion by long-pressing a rendered chart. A chart can cycle through compatible modes such as bar/histogram, line, pie/donut-style composition, and table.
- Performs local two-way data binding for form controls and sends the surface data model metadata when `sendDataModel` is enabled.
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

The app opens with a valid A2UI JSONL example. Edit the messages and press **Render**. Pressing the rendered button displays the generated action payload at the bottom of the screen.

The dropdown includes chart-heavy A2UI IR samples. Open **Chart lab: revenue** or **Chart lab: survey**, then long-press a chart to switch between the formats declared in that chart's `modes` property.

The **LLM to A2UI** panel demonstrates the intended assistant flow:

1. The LLM returns a normal plain-language answer.
2. `A2UIResponseConverter` converts that answer into A2UI v0.9/v0.9.1-compatible JSONL.
3. `A2UIProcessor` validates and applies the messages.
4. The catalog renderer materializes the surface with native Compose components.

The included `LocalDraftLlmClient` is a no-key local stub so the Android project builds anywhere. Replace it with a Gemini/OpenAI/local llama.cpp adapter when wiring a real model.

The renderer expects a component with ID `root` before displaying a surface.

## Scope

This is a working vertical slice, not yet a complete production renderer. It now follows the same main renderer responsibilities as the upstream A2UI renderers: message processing, surface state, component catalog resolution, data model updates, and local control bindings. Remaining production milestones are full schema validation, complete renderer capability metadata, validation/function execution, richer template scopes, streaming transport, visual parity tests, and a reusable renderer library module.

## Protocol source

Implementation follows the official A2UI renderer guidance, v0.9/v0.9.1 server-to-client message schema, and basic catalog from the `a2ui-project/a2ui` repository.
