# Local Development Setup Guide

This project requires specific versions of Java (21), Maven, and Node.js. 
To ensure a consistent environment without requiring administrative privileges or system-wide installations, we have provided scripts to set up a local toolchain.

## 1. Initial Setup (One-time)

Run the setup script to download and extract the required tools into the `tools/` directory.

```powershell
# You may need to bypass execution policy if scripts are disabled
powershell -ExecutionPolicy Bypass -File scripts\setup_tools.ps1
```

This will download:
-   **Java 21 (OpenJDK)**
-   **Apache Maven 3.9.6**
-   **Node.js v20.11.1 (LTS)**

## 2. Activate Environment (Every Session)

Whenever you open a new terminal to work on this project, run the activation script to configure your `PATH` and `JAVA_HOME` for the current session.

```powershell
# Source the script to apply changes to current session
. .\activate.ps1
```

**Troubleshooting:**
If you see an error like "cannot be loaded because running scripts is disabled", you can temporarily allow it for the current process:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
. .\activate.ps1
```

## 4. Application Startup (One-Click)

We provide a single script to start the entire application stack (Database + Backend + Frontend).

### Start All Services
Run the following command to start everything:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start_all.ps1
```

This script will:
1.  **Activate** the local environment.
2.  **Start PostgreSQL** in the background (if not already running).
3.  **Start the Backend** (Spring Boot) in a **new PowerShell window**.
4.  *(Future)* **Start the Frontend** in another new window.

### Stop All Services
To stop the application:
1.  Close the **Backend** PowerShell window.
2.  Run the stop script to shut down the database:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\stop_all.ps1
```

## 5. Manual Database Setup (Alternative)

If you prefer to manage the database separately:

### Start the Database
Run the helper script to initialize (first time only) and start the database server:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start_db.ps1
```

This script will:
1.  Initialize the data directory in `tools/pgdata`.
2.  Start the PostgreSQL server on port **5432**.
3.  Create the default database `finventory`.

*Note: The database runs as a background process.*

## 6. Verify Setup

After activation, you can verify the versions:

```powershell
java -version
mvn -version
node -v
postgres --version
```
