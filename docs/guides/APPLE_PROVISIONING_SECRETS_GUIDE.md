# Apple Developer Provisioning & CI/CD Credentials Guide

This guide provides the complete, bulletproof operational procedure to prepare Apple Developer credentials, certificates, provisioning profiles, and App Store Connect API keys for automated TestFlight releases via GitHub Actions (`.github/workflows/ios_testflight.yml`).

---

## 1. Prerequisites & Topology

| Item | Requirement / Value | Notes |
| :--- | :--- | :--- |
| **Apple Account** | `ibrahim alkateb` (`pm.ibrahimalkateb965@autovemtech.com`) | Signed in Xcode & Apple Developer Portal |
| **Developer Program** | Paid Apple Developer Program ($99/year) | Required for App Store / TestFlight signing |
| **Apple Team ID** | `3UMLLDSZ4S` | Matching `DEVELOPMENT_TEAM` in `iosApp/project.yml` |
| **App Bundle ID** | `com.aistudio.quranblind.a11y` | Explicit App ID matching `iosApp/project.yml` |
| **Profile Name** | `BlindQuran AppStore` | App Store distribution profile |
| **CI Runner** | GitHub Actions `macos-15` | Ephemeral keychain isolated per build |
| **Local Tools** | `openssl`, `base64`, `gh` CLI | Can run on Linux VPS, macOS, or WSL |

---

## 2. Step 1: Apple Developer Portal Setup (App ID)

1. Sign in to the [Apple Developer Portal - Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources).
2. Select **Identifiers** from the left navigation, then click the blue `+` button.
3. Select **App IDs** and click **Continue**, then choose **App** and click **Continue**.
4. Configure App ID details:
   - **Description**: `BlindQuran`
   - **Bundle ID**: Select **Explicit** and enter:
     ```
     com.aistudio.quranblind.a11y
     ```
     > [!IMPORTANT]
     > Do **not** use a wildcard (`*`) Bundle ID. Wildcards cannot bind explicit App Store provisioning profiles or entitlements.
5. **Capabilities**:
   - In iOS, continuous recitation with the screen locked relies on `UIBackgroundModes: ["audio"]`. This is declared in `iosApp/project.yml` (and rendered into `Info.plist`).
   - On the Apple Developer Portal, audio background mode does not require a special portal entitlement toggle (unlike Push Notifications or Apple Pay). Keep the default capability set (Game Center and In-App Purchase enabled by default).
6. Click **Continue**, review the details, and click **Register**.

---

## 3. Step 2: CSR Generation & Apple Distribution Certificate (`.p12`)

You can generate the Certificate Signing Request (CSR) and convert the certificate from any machine with `openssl` (Linux VPS container, macOS, or WSL).

### 3.1 Generate the Private Key and CSR

Create a dedicated, private working directory:

```bash
mkdir -p ~/apple-signing && cd ~/apple-signing && chmod 700 .

# Generate 2048-bit RSA private key
openssl genrsa -out ios_distribution.key 2048

# Generate the Certificate Signing Request (CSR)
# Replace CN and emailAddress with your Apple Developer account legal name and email
openssl req -new \
  -key ios_distribution.key \
  -out ios_distribution.certSigningRequest \
  -subj "/emailAddress=your_apple_id_email@domain.com/CN=Your Name/C=SA"
```

### 3.2 Request Certificate in Apple Developer Portal

1. Go to **Certificates** in the portal, click `+`.
2. Under **Software**, select **Apple Distribution** (or "iOS Distribution (App Store and Ad Hoc)").
3. Click **Continue**.
4. Upload `ios_distribution.certSigningRequest`.
5. Click **Generate**.
6. Download the issued certificate file `distribution.cer` into `~/apple-signing/`.

### 3.3 Convert Certificate to PKCS#12 (`.p12`) with `-legacy`

```bash
cd ~/apple-signing

# Convert Apple's DER certificate to PEM
openssl x509 -inform DER -in distribution.cer -out ios_distribution.pem

# Define your P12 export password (record this for GitHub Secrets)
export P12_PASSWORD="YourStrongPasswordHere"

# Export to .p12 using the MANDATORY -legacy flag
openssl pkcs12 -export -legacy \
  -inkey ios_distribution.key \
  -in ios_distribution.pem \
  -out ios_distribution.p12 \
  -name "Apple Distribution: BlindQuran" \
  -passout pass:"$P12_PASSWORD"
```

> [!CAUTION]
> **Why the `-legacy` flag is mandatory:**
> OpenSSL 3.x defaults to PBKDF2 with modern cipher suites (AES-256-CBC) for PKCS#12 bundles. Apple's macOS `security import` tool (executed inside the CI runner) does not recognize OpenSSL 3's modern KDF and fails with:
> `MAC verification failed during PKCS12 import (wrong password?)`
> The `-legacy` flag forces OpenSSL 3 to use the standard legacy cipher algorithms (3DES / SHA-1) that macOS Security framework supports.

---

## 4. Step 3: App Store Provisioning Profile Creation

1. In the Apple Developer Portal, select **Profiles**, then click `+`.
2. Under **Distribution**, select **App Store** (or "App Store Connect") and click **Continue**.
3. **App ID**: Select `com.aistudio.quranblind.a11y` (`BlindQuran`) and click **Continue**.
4. **Certificate**: Select the Apple Distribution certificate created in Step 2, then click **Continue**.
5. **Profile Name**: Enter `BlindQuran AppStore`.
6. Click **Generate**.
7. Download `BlindQuran_AppStore.mobileprovision` into `~/apple-signing/`.

---

## 5. Step 4: App Store Connect API Key Creation

App Store Connect API keys allow GitHub Actions to validate and upload `.ipa` packages without SMS/2FA prompts.

1. Navigate to [App Store Connect - Users and Access - Integrations - API Keys](https://appstoreconnect.apple.com/access/integrations/api).
   *(If prompted, click "Request Access to API").*
2. Click the `+` button to create a new API Key.
3. Configure:
   - **Name**: `GitHubActionsTestFlight`
   - **Access / Role**: Select **App Manager**.
     *(App Manager provides sufficient privileges for TestFlight uploads, build processing, and metadata management without exposing billing or account ownership).*
4. Click **Generate**.
5. Note and save the following values:
   - **Key ID**: 10-character alphanumeric string (e.g. `ABCD1234EF`), also shown in the downloaded file name.
   - **Issuer ID**: UUID shown at the top of the Keys page (e.g. `aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee`).
   - **Team ID**: 10-character Team ID found on [Apple Developer Account](https://developer.apple.com/account) under Membership Details (e.g. `A1B2C3D4E5`).
6. Click **Download API Key** next to the key:
   - File name: `AuthKey_<KEY_ID>.p8`.
   - > [!WARNING]
     > Apple permits downloading the `.p8` private key file **only once**. Save it into your password manager vault immediately.

---

## 6. Step 5: Base64 Encoding & GitHub Secrets Setup

### 6.1 Base64 Encode Binary Files

GitHub Actions secrets store plain text. The `.p12` certificate and `.mobileprovision` file must be encoded as single-line Base64 strings:

**On Linux (Ubuntu VPS / Container):**
```bash
cd ~/apple-signing
base64 -w0 ios_distribution.p12 > p12.b64
base64 -w0 BlindQuran_AppStore.mobileprovision > pp_appstore.b64

# Sanity check: non-empty outputs (thousands of characters)
wc -c p12.b64 pp_appstore.b64
```

**On macOS:**
```bash
cd ~/apple-signing
base64 -i ios_distribution.p12 | tr -d '\n' > p12.b64
base64 -i BlindQuran_AppStore.mobileprovision | tr -d '\n' > pp_appstore.b64

# Sanity check
wc -c p12.b64 pp_appstore.b64
```

### 6.2 The 8 Required GitHub Secrets

| Secret Name | Value Description | Format | Source |
| :--- | :--- | :--- | :--- |
| `BUILD_CERTIFICATE_BASE64` | Base64-encoded Apple Distribution `.p12` | Single-line base64 (`p12.b64`) | Step 2 & 5.1 |
| `P12_PASSWORD` | Password protecting the `.p12` file | Plain text string | Step 2.3 |
| `KEYCHAIN_PASSWORD` | Random password for ephemeral runner keychain | Plain text string (`openssl rand -base64 24`) | Generated locally |
| `PROVISIONING_PROFILE_APPSTORE_BASE64` | Base64-encoded `.mobileprovision` | Single-line base64 (`pp_appstore.b64`) | Step 3 & 5.1 |
| `APPLE_TEAM_ID` | Apple Developer Team ID (10 characters) | Plain text (e.g., `A1B2C3D4E5`) | Apple Account page |
| `APPSTORE_KEY_ID` | App Store Connect API Key ID (10 characters) | Plain text (e.g., `ABCD1234EF`) | Step 4 |
| `APPSTORE_ISSUER_ID` | App Store Connect API Issuer ID | UUID (e.g., `aaaaaaaa-bbbb-...`) | Step 4 |
| `APPSTORE_PRIVATE_KEY` | Raw contents of `AuthKey_<KEY_ID>.p8` | Multi-line text including `-----BEGIN...` | Step 4 |

### 6.3 Set Secrets via GitHub CLI (`gh`)

Run the following commands from `~/apple-signing`:

```bash
# Authenticate gh CLI if not already authenticated
gh auth login --hostname github.com --git-protocol https --web

REPO="your-org-or-username/blind-app" # Or omit --repo if inside the git repo

# Values (replace placeholders)
P12_PASS="YourChosenP12Password"
KEYCHAIN_PASS="$(openssl rand -base64 24)"
TEAM_ID="3UMLLDSZ4S"
KEY_ID="ABCD1234EF"
ISSUER_ID="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"

# Set all 8 secrets
gh secret set BUILD_CERTIFICATE_BASE64 --repo "$REPO" < p12.b64
gh secret set P12_PASSWORD             --repo "$REPO" --body "$P12_PASS"
gh secret set KEYCHAIN_PASSWORD        --repo "$REPO" --body "$KEYCHAIN_PASS"
gh secret set PROVISIONING_PROFILE_APPSTORE_BASE64 --repo "$REPO" < pp_appstore.b64
gh secret set APPLE_TEAM_ID            --repo "$REPO" --body "$TEAM_ID"
gh secret set APPSTORE_KEY_ID          --repo "$REPO" --body "$KEY_ID"
gh secret set APPSTORE_ISSUER_ID       --repo "$REPO" --body "$ISSUER_ID"
gh secret set APPSTORE_PRIVATE_KEY     --repo "$REPO" < "AuthKey_${KEY_ID}.p8"

# Verify all 8 secrets are present
gh secret list --repo "$REPO"
```

### 6.4 Set Secrets via GitHub Web UI

1. Open repository on GitHub.
2. Navigate to **Settings** -> **Secrets and variables** -> **Actions**.
3. Under **Repository secrets**, click **New repository secret**.
4. Add all 8 secrets from the table above.
   - For `BUILD_CERTIFICATE_BASE64` and `PROVISIONING_PROFILE_APPSTORE_BASE64`, paste the full text of `p12.b64` and `pp_appstore.b64`.
   - For `APPSTORE_PRIVATE_KEY`, open `AuthKey_<KEY_ID>.p8` in a text editor and copy the full text including:
     ```
     -----BEGIN PRIVATE KEY-----
     ...
     -----END PRIVATE KEY-----
     ```

---

## 7. Security Hygiene & Cleanup

After loading the secrets into GitHub:

1. **Secure Storage**: Backup `ios_distribution.p12` (and its password) and `AuthKey_<KEY_ID>.p8` in an encrypted password vault (e.g. 1Password, Bitwarden, KeePass).
2. **Local Shredding**: Destroy the signing artifacts in `~/apple-signing` to avoid leaving private keys exposed on the development host:
   ```bash
   cd ~
   shred -uvz -n 3 ~/apple-signing/* 2>/dev/null || rm -P ~/apple-signing/* 2>/dev/null || rm -f ~/apple-signing/*
   rm -rf ~/apple-signing
   ```
3. **Repository Check**: Confirm no `.p12`, `.p8`, `.key`, or `.mobileprovision` files are staged or committed in git.

---

## 8. Annual Expiry Reminder

- **Apple Distribution Certificate**: Expires after **1 year**.
- **Provisioning Profiles**: Expire after **1 year** (and are invalidated if the underlying certificate is revoked/expires).
- **App Store Connect API Key**: Does **not** expire unless revoked manually.

> [!TIP]
> Schedule a calendar reminder for **Month 11** to re-issue the Apple Distribution certificate and recreate the provisioning profile before CI builds fail.
