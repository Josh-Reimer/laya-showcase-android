# Laya Showcase (Android)

A small Android app that showcases **[Laya](https://github.com/NandhaKishorM/laya)**,
the open-source (Apache-2.0) "System 1" typed-decision model from Convai Innovations —
the community's open alternative to TypeSafe AI's closed **Jev** model.

Instead of generating text, Laya looks at a piece of text (a "state") plus one or
more typed questions, and answers each one directly with a calibrated probability:

- **choice** — pick one label from a set, with per-option probabilities
- **score** — an ordinal level (e.g. urgency), returned as an expected value
- **noul** — a yes/no statement, returned as `P(true)` from 0.0–1.0

This app is a thin, friendly client for that model. It does **not** run Laya
on-device — a 421M-parameter encoder isn't something you want to bundle into an
APK — instead it talks to a self-hosted `laya-serve` instance over its
Jev-compatible `POST /v1/systemone` REST endpoint, so you can point it at a
server running on your laptop, a LAN box, or a cloud VM.

## What the app does

- Ships with three ready-made preset scenarios (billing refund ticket, angry
  support message, product feedback) so there's something to try immediately.
- Lets you edit the input text and the typed questions sent to Laya.
- Calls `/v1/systemone` and renders the answers: selected choice, score, or
  `P(true)` for noul questions, each with a confidence value and a probability
  bar per option.
- Shows which checkpoint Laya's router selected (e.g. `english` vs
  `multilingual`) and the round-trip latency measured on-device.
- Lets you change the server base URL at runtime (defaults to
  `http://10.0.2.2:8000`, the standard alias for your host machine from the
  Android emulator).

## Running the Laya backend

The app is only useful once something is answering `/v1/systemone`. On any
machine with Python 3.10+:

```bash
pip install "laya[serve]"
LAYA_DEVICE=cpu LAYA_PRELOAD=1 laya-serve
```

This starts the server on `:8000`. Point the app at `http://10.0.2.2:8000`
from the emulator, or `http://<your-lan-ip>:8000` from a real device on the
same network.

See [Laya's GitHub repo](https://github.com/NandhaKishorM/laya) for the full
reference implementation, checkpoints, and Python API.

## Building the app

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34.0.0).

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```
app/src/main/kotlin/com/layaplayground/showcase/
  MainActivity.kt      Compose UI
  MainViewModel.kt      UI state + calls into the Laya client
  net/LayaClient.kt     OkHttp client for the /v1/systemone wire protocol
  data/Questions.kt     Typed question model + preset scenarios
```

## License

App code is MIT licensed — see [LICENSE](LICENSE). Laya itself is licensed
Apache-2.0 by Convai Innovations; this project is unaffiliated with Convai
Innovations or TypeSafe AI.
