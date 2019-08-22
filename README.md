## Releasing Signed Custom Apps

Before making a release, customise the zip file you wish to build into the custom app.

1. The project provides a template for the file with the necessary parameters. Copy this to `.signing` and
2. edit it to use the correct parameters.
3. Copy your keystore file to the project's root folder.
4. Run gradle with the additional parameter for the path to the ZIP file to incorporate into the app.

```bash
cp .signing.example .signing
vim .signing
cp path/to/keystore release.keystore
gradle -PzipPath=/path/to/zip assembleRelease
```

## Customising the ZIP file
Add a `config.json` to the root of the ZIP file. Details TBC.

