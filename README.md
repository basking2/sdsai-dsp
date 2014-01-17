sdsai-dsp
=========

Digital Signal Processing algorithms and data structures.

Building
========
```
gradle jar
```

Running Tests
=============

```
gradle check
gradle check -Dtest.single=BpskDetectorTest
```

TODO
====

These are desired features, but are not on any roadmap yet.

- Binary input and output streams. These would send 8 bit bytes over Bpsk.