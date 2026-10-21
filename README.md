# Laya Showcase (Android)

A small Android app that showcases **[Laya](https://github.com/NandhaKishorM/laya)**,
the open-source (Apache-2.0) "System 1" typed-decision model from Convai Innovations —
the community's open alternative to TypeSafe AI's closed **Jev** model.

Instead of generating text, Laya looks at a piece of text (a "state") plus one or
more typed questions, and answers each one directly with a calibrated probability:

- **choice** — pick one label from a set, with per-option probabilities
- **score** — an ordinal level (e.g. urgency), returned as an expected value
- **noul** — a yes/no statement, returned as `P(true)` from 0.0–1.0

## This runs entirely on-device

Laya's English checkpoint (ModernBERT-large, 421M params) is exported to ONNX and
bundled directly inside the APK's assets (`app/src/main/assets/laya-model/`). On
first launch the app copies it out to app storage and opens an ONNX Runtime
session; every prediction after that is a local forward pass through
[laya-java](https://github.com/NandhaKishorM/laya/tree/main/laya-java) — a
vendored, pure-Java port of the reference tokenizer, sequence builder and
decision head (checked into [`laya-java/`](laya-java), since it isn't published
to Maven Central upstream). **Nothing is sent over the network** — there's no
server, no API key, and the app has no `INTERNET` permission.

That tradeoff is deliberate and size isn't a concern here: the bundled model
weights alone are ~1.7GB (fp32, unquantized — Laya's own benchmarks show INT8
weight quantization collapses decision accuracy, e.g. 31/96 vs 64/96 held-out
agreement on the English checkpoint, so this project ships full precision
instead). This APK is meant to be sideloaded, not published to Google Play.

## What the app does

- Three built-in preset scenarios (billing refund ticket, angry support message,
  product feedback) to try immediately.
- Lets you edit the input text and the typed questions sent to Laya.
- Runs the model locally and renders the answers: selected choice, score, or
  `P(true)` for noul questions, each with a confidence value and a probability
  bar per option (score questions show the real level text, read from the
  checkpoint's legend, not just an index).
- Shows on-device inference latency per request.

## Building the app

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34.0.0). minSdk is
26 (Android 8.0) — the vendored `laya-java` module uses `java.nio.file`, and core
library desugaring backports the rest of what it needs down to that floor.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Expect
it to be large (~1.7GB) and the first build to take a while — Gradle has to
package that much data into the APK.

### Re-exporting the model yourself

The exported `laya.onnx` + `laya.onnx.data` + tokenizer files under
`app/src/main/assets/laya-model/` were produced from the upstream repo:

```bash
pip install "laya[onnx]"
git clone https://github.com/NandhaKishorM/laya
cd laya
python scripts/export_onnx.py --model convaiinnovations/laya --output laya.onnx
```

(Fetch `rl_agent_config.json` and the `tokenizer/` directory straight from the
[Hugging Face repo](https://huggingface.co/convaiinnovations/laya) — they don't
need the export step.) Swap in `convaiinnovations/laya-multilingual` instead for
a smaller, 100+-language checkpoint at some English-task quality cost.

## Project layout

```
app/src/main/kotlin/com/layaplayground/showcase/
  MainActivity.kt         Compose UI
  MainViewModel.kt         UI state; opens the engine once and keeps it warm
  engine/LayaEngine.kt     Extracts the bundled checkpoint, runs predictions
  engine/LayaAnswer.kt     Typed answer model the UI renders
  data/Questions.kt        Typed question model + preset scenarios

laya-java/                 Vendored laya-java (Apache-2.0, from the laya repo)
```

## License

App code is MIT licensed — see [LICENSE](LICENSE). Laya itself, including the
vendored `laya-java` runtime and the bundled model weights, is licensed
Apache-2.0 by Convai Innovations (see [laya-java/LICENSE-laya-java-upstream](laya-java/LICENSE-laya-java-upstream)).
This project is unaffiliated with Convai Innovations or TypeSafe AI.
