sdsai-dsp
=========

Digital Signal Processing algorithms and data structures.

Building
========
```
gradle jar
```

Or to install for use with Maven,

```
gradle install
````

This will install the jar in the package `org.sdsai` as `sdsai-dsp.jar`.

Running Tests
=============

```
gradle check
gradle check -Dtest.single=BpskDetectorTest
```

TODO
====

These are desired features, but are not on any roadmap yet.

- Add filtering
  - Low pass
  - High pass
- Add amplitude normalization
- Add decimation?
- Binary input and output streams. These would send 8 bit bytes over Bpsk.
- Write simple signal generators as OutputStreams.
