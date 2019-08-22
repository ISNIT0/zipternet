## Releasing Signed Custom Apps
```bash
cp .signing.example .signing
cp path/to/keystore release.keystore
gradle -PzipPath=/path/to/zip assembleRelease
```