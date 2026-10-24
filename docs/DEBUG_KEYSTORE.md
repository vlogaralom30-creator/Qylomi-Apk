# Debug Keystore Generation

To ensure consistent app signatures across debug builds and local/CI environments, you can generate a stable debug keystore and its base64-encoded representation.

## Instructions

1. Generate the `debug.keystore` file using `keytool`:
   ```bash
   keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey \
     -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Android Debug,O=Android,C=US"
   ```

2. Encode the keystore to base64:
   - Linux / WSL:
     ```bash
     base64 -w 0 debug.keystore > debug.keystore.base64
     ```
   - macOS:
     ```bash
     base64 -i debug.keystore -o debug.keystore.base64
     ```

3. Commit only `debug.keystore.base64` to the repository root. Keep `debug.keystore` ignored in `.gitignore`.
