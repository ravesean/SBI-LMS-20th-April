# SonarQube Cloud — Setup & Lab Guide
**SBI DevSecOps Intermediate Training**

---

## What is SonarQube Cloud?

SonarQube Cloud is a hosted version of SonarQube — no server installation needed. You sign up, connect your GitHub repository, and push your code. The CI/CD pipeline runs automatically and results appear on your personal dashboard at sonarqube.io.

**Free tier covers:** 50,000 lines of code (LMS is well under this), 5 users, private repositories.

---

## How It Works

```
You push code to GitHub
        ↓
GitHub Actions pipeline triggers automatically
        ↓
Pipeline runs: Secrets → SAST → Build → Container Scan → IaC Scan → DAST
        ↓
SonarQube Cloud dashboard shows your scan results
```

**You never need to run scan commands manually. Every push triggers the full pipeline.**

---

## Prerequisites

Before starting, confirm you have these on your machine:

- [ ] Git installed (`git --version` to verify)
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

### Step 2 — Create a GitHub Repository and Push LMS Code

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
5. When asked about the analysis method, select **"With GitHub Actions"**

SonarQube Cloud will now show you your configuration values — keep this page open for the next step.

---

### Step 5 — Note Your Configuration Values

From the SonarQube Cloud setup page, note these two values:

| Value | Where to find it | Your value |
|---|---|---|
| **Organization Key** | Your GitHub username as shown in SonarQube Cloud | `________________` |
| **Project Key** | Shown on the project setup page | `________________` |

> **Example:** If your GitHub username is `john-doe`, your Organization Key is `john-doe` and your Project Key will be something like `john-doe_sbi-lms`.

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

Open `pom.xml` in VS Code. Find the `<properties>` section (around line 10–20).

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

### Step 8 — Add GitHub Secrets

The pipeline needs two secrets stored securely in GitHub — never in code files.

1. Go to your `sbi-lms` repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add these two secrets:

| Secret Name | Value |
|---|---|
| `SONAR_TOKEN` | The token you generated in Step 6 |
| `JWT_SECRET` | Any long random string e.g. `training-jwt-secret-2026` |

---

### Step 9 — Add the Pipeline File

The pipeline file `devsecops.yml` is provided by your trainer.

Place it in your project at exactly this path:
```
sbi-lms/
└── .github/
    └── workflows/
        └── devsecops.yml
```

> On Windows, folders starting with `.` may need to be created manually. In VS Code terminal:
> ```bash
> mkdir -p .github/workflows
> ```
> Then copy the `devsecops.yml` file into that folder.

---

### Step 10 — Commit Everything and Trigger the Pipeline

```bash
git add pom.xml .github/workflows/devsecops.yml
git commit -m "configure sonarqube cloud and pipeline"
git push
```

**Watch the pipeline run:**

1. Go to your `sbi-lms` repository on GitHub
2. Click the **Actions** tab
3. You will see the pipeline running with 6 stages

**Watch the results appear:**

1. Go to **https://sonarqube.io**
2. Open your `sbi-lms` project
3. After the SAST stage completes (~2–3 minutes), you will see scan results including the intentional vulnerabilities in the LMS code

If you see results on the SonarQube Cloud dashboard, setup is complete.

---

## Part 2 — Lab Usage

> **Important:** You do not run scan commands manually. For every lab, the workflow is:
> 1. Make code changes in VS Code
> 2. Commit and push
> 3. Watch the pipeline on GitHub Actions tab
> 4. See results on SonarQube Cloud dashboard

---

### Lab 1 — SAST Scan + Fix (Day 1, 12:30–1:30)

#### Phase A — See the vulnerabilities

Your initial push from setup already triggered a scan. Open your SonarQube Cloud dashboard and click **Issues** → filter by **Severity: Critical**.

You should find two issues:

| # | Issue | Location |
|---|---|---|
| 1 | Hardcoded JWT secret | `JwtUtils.java` line 27 |
| 2 | Missing `@PreAuthorize` | `ApplicationController.java` — `getById()` method |

#### Phase B — Fix the issues

**Fix 1 — Hardcoded JWT secret**

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

**Fix 2 — Missing @PreAuthorize**

Open `src/main/java/com/sbi/lms/controller/ApplicationController.java`.

Find the `getById()` method. Add this annotation directly above it:
```java
@PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
```

Also uncomment the `maskPiiIfOfficer` line inside the method.

#### Phase C — Push and verify

```bash
git add .
git commit -m "fix: remove hardcoded secret and add PreAuthorize"
git push
```

1. Go to GitHub → **Actions** tab → watch the pipeline run
2. Go to SonarQube Cloud dashboard → **Quality Gate** should now show **PASSED** with 0 Critical issues

---

### Capstone Lab — Full Pipeline (Day 2, 4:00–5:00)

#### Phase A — Introduce the vulnerability

The `/search` endpoint in `ApplicationController.java` is already written with a SQL injection vulnerability. Commit and push it as-is:

```bash
git add .
git commit -m "feat: add branch search endpoint"
git push
```

#### Phase B — Watch the pipeline catch it

1. Go to GitHub → **Actions** tab → watch the pipeline
2. The pipeline will **fail** at the SAST stage
3. Go to SonarQube Cloud dashboard → find the SQL injection **Blocker** on the `/search` endpoint
4. Click on the issue — SonarQube shows the full taint flow: where user input enters and where the unsafe query is built

#### Phase C — Fix the vulnerability

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

#### Phase D — Push and confirm pipeline goes green

```bash
git add .
git commit -m "fix: replace string-concat JPQL with safe repository query"
git push
```

1. Go to GitHub → **Actions** tab → all 6 stages should go green ✓
2. Go to SonarQube Cloud dashboard → SQL injection Blocker is gone → Quality Gate: **PASSED**

**Full pipeline clean: SAST ✓  Container ✓  IaC ✓  DAST ✓**

---

## Quick Reference

### The Only Workflow You Need

```bash
# 1. Make your code changes in VS Code
# 2. Then:
git add .
git commit -m "your message"
git push
# 3. Watch: GitHub → Actions tab
# 4. Results: https://sonarqube.io
```

### Your Key URLs

| What | URL |
|---|---|
| Pipeline runs | `https://github.com/YOUR_USERNAME/sbi-lms/actions` |
| Scan results | `https://sonarqube.io` |

### Severity Levels (SonarQube)

| Severity | Pipeline behaviour |
|---|---|
| **Blocker** | Pipeline fails — code cannot be merged |
| **Critical** | Quality Gate fails — pipeline fails |
| **Major** | Pipeline passes — flagged for review |
| **Minor** | Pipeline passes — informational |

---

## Troubleshooting

**Pipeline does not appear in the Actions tab after push**
Check that the pipeline file is at exactly `.github/workflows/devsecops.yml` in your repository. Open the file on GitHub to confirm it uploaded correctly.

**SAST stage fails with "Not authorized"**
Your `SONAR_TOKEN` secret is incorrect or expired. Generate a new token in SonarQube Cloud → My Account → Security, then update the GitHub secret under Settings → Secrets → Actions.

**SAST stage fails with "Project not found"**
The `sonar.projectKey` or `sonar.organization` in `pom.xml` does not match what SonarQube Cloud shows. Values are case-sensitive. Check your SonarQube Cloud dashboard and correct the values, then commit and push again.

**Pipeline passes but dashboard shows no results**
Wait 30 seconds and refresh the SonarQube Cloud dashboard. Results take a moment to appear after the pipeline stage completes.

**Quality Gate still failing after fixes**
Make sure you saved the file before running `git add`. Check the SonarQube Cloud dashboard issue list — there may be an additional issue you have not fixed yet.

**Build stage fails with compilation error**
There is a Java error in the code. Check the pipeline logs under GitHub → Actions → click the failed run → click the Build stage. Fix the compilation error, commit, and push.

---

*SBI DevSecOps Intermediate Training — Confidential, For Training Purposes Only*
