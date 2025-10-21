# GitHub Actions Release Setup Checklist

This checklist guides you through setting up automated release builds and signing for the TRMNL Android Buddy app.

## Prerequisites

- [ ] You have admin access to the GitHub repository
- [ ] You have `keytool` installed (comes with JDK)
- [ ] You have access to create and manage GitHub repository secrets

## Step 1: Create Production Release Keystore

### 1.1 Generate the Keystore

Run this command to create a new production keystore:

```bash
keytool -genkey -v \
  -keystore trmnl-android-buddy-release.keystore \
  -alias trmnl-android-buddy \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype PKCS12
```

**Important Notes:**
- You'll be prompted for a keystore password - **use a strong, unique password**
- You'll be prompted for a key password - **use the SAME password as the keystore password**
- Fill in the certificate information (name, organization, etc.)
- The keystore will be valid for ~27 years (10,000 days)

### 1.2 Secure the Keystore

- [ ] Save the keystore file in a secure location (NOT in the repository)
- [ ] Create a secure backup of the keystore file
- [ ] Document the passwords in a secure password manager
- [ ] **NEVER** commit the keystore file to version control

### 1.3 Record Your Keystore Information

Keep this information secure:

- **Keystore Password**: `[YOUR_PASSWORD_HERE]`
- **Key Alias**: `trmnl-android-buddy` (or whatever you used)
- **Keystore Location**: `[PATH_TO_YOUR_KEYSTORE]`

## Step 2: Convert Keystore to Base64

The GitHub Actions workflow needs the keystore as a base64-encoded string.

### 2.1 Encode the Keystore

**On macOS:**
```bash
base64 -i trmnl-android-buddy-release.keystore | pbcopy
```
The base64 string is now in your clipboard.

**On Linux:**
```bash
base64 -i trmnl-android-buddy-release.keystore > keystore-base64.txt
cat keystore-base64.txt
```
Copy the output (the entire base64 string).

**On Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("trmnl-android-buddy-release.keystore")) | Set-Clipboard
```

### 2.2 Verify the Encoding

Test that the encoding worked:

**On macOS/Linux:**
```bash
echo "[PASTE_YOUR_BASE64_HERE]" | base64 -d > test-keystore.keystore
keytool -list -keystore test-keystore.keystore
rm test-keystore.keystore
```

If it lists the keystore contents, the encoding is correct.

## Step 3: Configure GitHub Repository Secrets

### 3.1 Navigate to Repository Settings

1. Go to: https://github.com/hossain-khan/trmnl-android-buddy/settings/secrets/actions
2. Click "New repository secret"

### 3.2 Add Required Secrets

Add these three secrets (one at a time):

#### Secret 1: KEYSTORE_BASE64
- **Name**: `KEYSTORE_BASE64`
- **Value**: [Paste the entire base64 string from Step 2.1]
- Click "Add secret"

#### Secret 2: KEYSTORE_PASSWORD
- **Name**: `KEYSTORE_PASSWORD`
- **Value**: [Your keystore password]
- Click "Add secret"

#### Secret 3: KEY_ALIAS
- **Name**: `KEY_ALIAS`
- **Value**: `trmnl-android-buddy` (or whatever alias you used)
- Click "Add secret"

### 3.3 Verify Secrets Are Set

- [ ] Confirm all three secrets appear in the repository secrets list
- [ ] Verify the secret names match exactly (case-sensitive)

## Step 4: Test the Keystore Configuration

### 4.1 Run Keystore Diagnostics Workflow

1. Go to: https://github.com/hossain-khan/trmnl-android-buddy/actions/workflows/test-keystore.yml
2. Click "Run workflow"
3. Select the `main` branch
4. Click "Run workflow" button

**Expected Result:** All checks should pass with ✅

### 4.2 Review Diagnostic Results

- [ ] Check the workflow run completed successfully
- [ ] Download the "keystore-diagnostic-files" artifact
- [ ] Review the diagnostic output files
- [ ] Verify the alias matches your keystore

### 4.3 Test APK Signing Workflow

1. Go to: https://github.com/hossain-khan/trmnl-android-buddy/actions/workflows/test-keystore-apk-signing.yml
2. Click "Run workflow"
3. Select the `main` branch
4. Click "Run workflow" button

**Expected Result:** APK should be successfully built and signed

### 4.4 Verify APK Creation

- [ ] Check the workflow run completed successfully
- [ ] Verify "✅ Android release build succeeded with production keystore" appears in logs
- [ ] Verify "✅ Release APK generated successfully" appears in logs
- [ ] Download the "keystore-test-results" artifact (optional)

## Step 5: Test Automated Release Build

### 5.1 Trigger Release Workflow Manually

1. Go to: https://github.com/hossain-khan/trmnl-android-buddy/actions/workflows/android-release.yml
2. Click "Run workflow"
3. Select the `main` branch
4. Click "Run workflow" button

### 5.2 Verify Release Build

- [ ] Check the workflow run completed successfully
- [ ] Verify APK was created in the artifacts
- [ ] Download the APK artifact named "trmnl-android-buddy-app"
- [ ] Verify the APK file name includes the version number

### 5.3 Install and Test the APK (Optional)

- [ ] Transfer the APK to an Android device
- [ ] Install the APK (you may need to allow "Install from unknown sources")
- [ ] Verify the app launches and functions correctly
- [ ] Check that it's signed with your production keystore (Settings → Apps → TRMNL Android Buddy → Advanced)

## Step 6: Test GitHub Release Integration

### 6.1 Create a Test Release

1. Go to: https://github.com/hossain-khan/trmnl-android-buddy/releases/new
2. Create a new tag (e.g., `1.0.6-test`)
3. Add a release title and description
4. Click "Publish release"

### 6.2 Verify APK Attachment

- [ ] Wait for the "Android Release Build" workflow to complete
- [ ] Refresh the release page
- [ ] Verify the APK file is attached to the release
- [ ] Verify the APK file name matches the version

### 6.3 Clean Up Test Release (Optional)

- [ ] Delete the test release if desired
- [ ] Delete the test tag: `git push --delete origin 1.0.6-test`

## Ongoing Usage

### Automatic Builds

The `android-release.yml` workflow automatically runs on:

- **Every push to main branch**: Creates a snapshot build (artifact available for 30 days)
- **Manual trigger**: Run the workflow manually from the Actions tab
- **GitHub release**: Publishes APK and attaches it to the release

### Creating Production Releases

1. Update version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 7  // Increment
   versionName = "1.0.6"  // Update
   ```
2. Update `CHANGELOG.md` with release notes
3. Commit and push to main branch
4. Create a GitHub release with tag matching the version
5. The workflow automatically builds and attaches the signed APK

### Troubleshooting

If builds fail:

1. Run the `test-keystore.yml` workflow for diagnostics
2. Review the workflow logs for error messages
3. Verify all three secrets are set correctly
4. Check that the keystore alias matches the `KEY_ALIAS` secret
5. Ensure the keystore and key passwords are the same

## Security Reminders

- [ ] Never commit keystore files to version control
- [ ] Keep keystore passwords in a secure password manager
- [ ] Maintain secure backups of the keystore file
- [ ] Rotate the keystore only if compromised (requires user updates)
- [ ] Limit repository access to trusted collaborators
- [ ] Regularly audit who has access to repository secrets

## Completion

- [ ] All steps completed successfully
- [ ] Release workflows tested and working
- [ ] Keystore securely backed up
- [ ] Team members informed of the new release process

---

**Need Help?**
- Review workflow logs in the Actions tab
- Check `keystore/README.md` for additional documentation
- Run diagnostic workflows for troubleshooting
- Ensure all secrets are set correctly with exact names
