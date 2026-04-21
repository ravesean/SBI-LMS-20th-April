# SonarQube Cloud — Setup & Lab Guide
**SBI DevSecOps Intermediate Training**

---

## What is SonarQube Cloud?

SonarQube Cloud is a hosted version of SonarQube — no server installation needed. You sign up, connect your GitHub repository, and run scans from the command line. Results appear on your personal dashboard at sonarqube.io.

**Free tier covers:** 50,000 lines of code (LMS is well under this), 5 users, private repositories.

---

## Prerequisites

Before starting, confirm you have these on your machine:

- [ ] Git installed
- [ ] Java 17 installed (`java -version` to verify)
- [ ] Maven installed (`mvn -version` to verify)
- [ ] The LMS project folder on your machine (unzipped)

---

## Part 1 — One-Time Setup

### Step 1 — Create a GitHub Account

If you already have a GitHub account, skip to Step 2.

1. Go to **https://github.com**
2. Click **Sign up**
3. Complete registration with your email

---

### Step 2 — Push LMS Project to GitHub

**2a. Create a new repository on GitHub**

1. Log in to GitHub
2. Click the **+** icon (top right) → **New repository**
3. Name it: `sbi-lms`
4. Set visibility to: **Private**
5. Do NOT check "Add a README file"
6. Click **Create repository**

**2b. Push the LMS code from your machine**

Open a terminal, navigate to the LMS project folder, and run:

```bash
git init
git add .
git commit -m "initial commit"
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/sbi-lms.git
git branch -M main
git push -u origin main
```

> Replace `YOUR_GITHUB_USERNAME` with your actual GitHub username.

Verify: refresh your GitHub repository page — you should see all the LMS files.

---

### Step 3 — Sign Up for SonarQube Cloud

1. Go to **https://sonarqube.io**
2. Click **Sign up**
3. Select **GitHub** as your login method
4. Authorize SonarQube Cloud to access your GitHub account
5. You will land on the SonarQube Cloud dashboard

---

### Step 4 — Import the LMS Project into SonarQube Cloud

1. On the SonarQube Cloud dashboard, click **"Analyze a project"** or **"Import a project"**
2. Select **GitHub** as the source
3. Find and select your `sbi-lms` repository
4. Click **Set up**
5. When asked about the analysis method, select **"With GitHub Actions"** — this gives you the correct configuration values

> SonarQube Cloud will detect that LMS is a Java Maven project automatically.

---

### Step 5 — Note Your Configuration Values

After import, SonarQube Cloud shows you configuration values. Note these down — you need them in the next steps.

| Value | Where to find it | Your value |
|---|---|---|
| **Organization Key** | Shown on your dashboard / org settings | `________________` |
| **Project Key** | Shown on the project page | `________________` |

---

### Step 6 — Generate a SonarQube Token

1. Click your profile icon (top right) → **My Account**
2. Go to the **Security** tab
3. Under **Generate Tokens**, enter a name: `lms-training`
4. Click **Generate**
5. **Copy the token immediately** — it will not be shown again

```
Your token: ________________________________________________
```

---

### Step 7 — Update `pom.xml` in the LMS Project

Open `pom.xml` in VS Code. Find the `<properties>` section (around line 10–20). Update these two lines with your values from Step 5:

**Before:**
```xml
<sonar.host.url>http://SONAR_SERVER_IP:9000</sonar.host.url>
<sonar.projectKey>sbi-lms</sonar.projectKey>
<sonar.projectName>SBI Loan Management System</sonar.projectName>
```

**After:**
```xml
<sonar.projectKey>YOUR_PROJECT_KEY</sonar.projectKey>
<sonar.organization>YOUR_ORGANIZATION_KEY</sonar.organization>
<sonar.projectName>SBI Loan Management System</sonar.projectName>
```

> Remove the `sonar.host.url` line entirely — SonarQube Cloud does not need it.

Save the file.

---

### Step 8 — Commit the pom.xml Change

```bash
git add pom.xml
git commit -m "configure sonarqube cloud"
git push
```

---

### Step 9 — Verify Setup with a Test Scan

Run this command from the LMS project root directory:

```bash
mvn clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN
```

> Replace `YOUR_TOKEN` with the token you generated in Step 6.

The scan takes 2–3 minutes. You will see `BUILD SUCCESS` in the terminal when complete.

**Verify on the dashboard:**

1. Go to **https://sonarqube.io**
2. Open your `sbi-lms` project
3. You should see the scan results — including the intentional vulnerabilities in the LMS code

If you see results, setup is complete. Proceed to Part 2 for lab usage.

---

## Part 2 — Lab Usage

### Lab 1 — SAST Scan (Day 1, 12:30–1:30)

**Step 1 — Build the project**

```bash
mvn clean package -DskipTests
```

**Step 2 — Run the SonarQube scan**

```bash
mvn sonar:sonar -Dsonar.token=YOUR_TOKEN
```

**Step 3 — Open your dashboard**

Go to **https://sonarqube.io** → open `sbi-lms` project.

**Step 4 — Find the two Critical issues**

On the dashboard, click **Issues** → filter by **Severity: Critical**.

You should find:

| # | Issue | Location |
|---|---|---|
| 1 | Hardcoded JWT secret | `JwtUtils.java` line 27 |
| 2 | Missing `@PreAuthorize` | `ApplicationController.java` — `getById()` method |

**Step 5 — Fix Issue 1 (Hardcoded JWT secret)**

Open `src/main/java/com/sbi/lms/security/JwtUtils.java`.

Delete these two lines:
```java
private static final String JWT_SECRET = "SBIBankingSecretKey2024";
private static final long   JWT_EXPIRY  = 86400000;
```

Uncomment these lines:
```java
@Value("${jwt.secret}")
private String jwtSecret;

@Value("${jwt.expiration.ms:900000}")
private long jwtExpirationMs;
```

**Step 6 — Fix Issue 2 (Missing @PreAuthorize)**

Open `src/main/java/com/sbi/lms/controller/ApplicationController.java`.

Find the `getById()` method. Add the annotation above it:
```java
@PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
```

Also uncomment the `maskPiiIfOfficer` line inside the method.

**Step 7 — Re-scan and verify**

```bash
mvn clean package -DskipTests sonar:sonar -Dsonar.token=YOUR_TOKEN
```

Open your dashboard → **Quality Gate** should now show **PASSED** with 0 Critical issues.

---

### Capstone Lab — Full Pipeline (Day 2, 4:00–5:00)

**Step 1 — Introduce the SQL injection vulnerability**

Open `ApplicationController.java`. The `/search` endpoint is already present and vulnerable. Commit it:

```bash
git add .
git commit -m "feat: add branch search endpoint"
```

**Step 2 — Run SAST scan**

```bash
mvn clean package -DskipTests sonar:sonar -Dsonar.token=YOUR_TOKEN
```

Open dashboard → find the SQL injection **Blocker** on the `/search` endpoint. Click on it to see the full taint analysis — where the user input enters and where the unsafe query is built.

**Step 3 — Fix the vulnerability**

In `ApplicationController.java`, find the `searchByBranch()` method.

Delete the vulnerable block:
```java
List<LoanApplication> result = em.createQuery(
    "SELECT a FROM LoanApplication a WHERE a.branch.name = '" + branch + "'",
    LoanApplication.class).getResultList();
return ResponseEntity.ok(result);
```

Uncomment the safe block:
```java
List<LoanApplication> result = applicationRepository.findByBranchName(branch);
return ResponseEntity.ok(result.stream().map(mapper::toDto).collect(toList()));
```

**Step 4 — Re-scan and confirm clean**

```bash
mvn clean package -DskipTests sonar:sonar -Dsonar.token=YOUR_TOKEN
```

Dashboard → SQL injection Blocker should be gone. Quality Gate: **PASSED**.

---

## Quick Reference

### Scan Command (use throughout all labs)

```bash
mvn sonar:sonar -Dsonar.token=YOUR_TOKEN
```

### Full Build + Scan Command

```bash
mvn clean package -DskipTests sonar:sonar -Dsonar.token=YOUR_TOKEN
```

### Your Dashboard URL

```
https://sonarqube.io
```

### Severity Levels (SonarQube)

| Severity | Action Required |
|---|---|
| **Blocker** | Fix immediately — pipeline fails |
| **Critical** | Fix before merging — Quality Gate fails |
| **Major** | Fix before next release |
| **Minor** | Fix opportunistically |

---

## Troubleshooting

**"Not authorized" error during scan**
Your token may be incorrect or expired. Generate a new token (Step 6) and re-run.

**"Project not found" error**
Check that `sonar.projectKey` and `sonar.organization` in `pom.xml` exactly match what SonarQube Cloud shows on your dashboard. Values are case-sensitive.

**"BUILD FAILURE" before the scan starts**
Run `mvn clean package -DskipTests` first. Fix any compilation errors before scanning.

**Dashboard shows no new results after re-scan**
Wait 30 seconds and refresh. SonarQube Cloud takes a moment to process results after the Maven command completes.

**Quality Gate still failing after fixes**
Check that you saved the file, and that you re-ran `mvn clean package` (not just `sonar:sonar`) so the compiled code is updated before scanning.

---

*SBI DevSecOps Intermediate Training — Confidential, For Training Purposes Only*
