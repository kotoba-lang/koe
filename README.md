# koe-clj (声)

Reusable **voice-session kernel** in portable Clojure — every namespace is `.cljc`,
designed for **Clojure-on-WASM hosts** (SCI, ClojureScript, GraalVM, kotoba-clj) as
well as the JVM. The shared library behind voice agents: it defines the **ports**
(telephony / STT / dialog / TTS / booking) and the **dialog loop** that binds them,
with every concrete capability **injected by the host** — no telephony SDK, no model,
no credentials live here.

Built alongside [langgraph-clj](https://github.com/com-junkawasaki/langgraph-clj) /
[langchain-clj](https://github.com/com-junkawasaki/langchain-clj). Sibling of
[godaddy-dns-clj](https://github.com/com-junkawasaki/godaddy-dns-clj) and
[browser-use-clj](https://github.com/com-junkawasaki/browser-use-clj).

## Why a shared library (org placement)

Per the three-way rule, the **reusable** kernel lives in **com-junkawasaki**, while
**public-benefit actor instances** that consume it live in **etzhayyim**, and any
**business/private deployment** lives in **gftdcojp**. The first consumer is the
`denwaban` (電話番) voice-receptionist actor (etzhayyim, ADR-2606271930), which injects
`twilio-compat` (telephony), `whisper-compat` (STT), `elevenlabs-compat` (TTS) and
`yotei` (booking) into these ports.

## Ports (`koe.ports`)

```
ITelephony  answer / say / hangup        — twilio-compat | vonage | kotoba-net/webrtc
ISTT        transcribe / stream          — whisper-compat
IDialog     step (utterance → reply+act) — kotoba-llm
ITTS        synth                        — elevenlabs-compat
IBooking    propose / confirm            — yotei  (confirm is member-signed; never server-side)
```

The dialog loop (`koe.session/converse`) is a pure fold over these ports: STT in →
dialog step → optional booking action → TTS out, with **barge-in** (a new partial
transcript interrupts in-flight TTS). Booking is always **delegated** — the kernel
never confirms a booking itself.

The closed booking-delegation shape gate also has a native, capability-free
`.kotoba` implementation for restricted JavaScript and Wasm. Dialog state,
utterances, TTS/audio, slot proposal, cryptographic signature verification,
confirmation, and all host ports remain CLJC/host responsibilities.

```
clojure -X:test
```
