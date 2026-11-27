# Warehouse Inventory and Stock Movement System

## Project layout

- `frontend/` — Angular 22 application.
- `warehouse-inventory/` — Spring Boot 4.2 REST API, configured for Java 25.
- MySQL database — `warehouse_db`.

The Angular development environment calls `http://localhost:8080/api`. The Spring Boot
controllers allow the development origin `http://localhost:4200`; use that exact hostname
and port in the browser rather than `127.0.0.1`.

## Prerequisites

- Windows PowerShell.
- JDK 25 on `PATH` (`java --version` should report 25).
- MySQL Server running on `localhost:3306`.
- Node.js and npm supported by Angular 22.

## Create the database

Start the MySQL service, then create the database using MySQL Workbench or the MySQL CLI:

```sql
CREATE DATABASE IF NOT EXISTS warehouse_db;
```

The backend uses Spring datasource settings. In the PowerShell window used to start the
backend, provide local connection values without adding passwords to source files:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/warehouse_db"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "<your local MySQL password>"
```

Replace the username/password for your local MySQL account. Keep real credentials out of
Git and do not commit `.env` files.

## Run the Spring Boot API

From the repository root, open a PowerShell window and run:

```powershell
Set-Location .\warehouse-inventory
java --version
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`. Keep this window open.

## Run the Angular application

Open a second PowerShell window at the repository root:

```powershell
Set-Location .\frontend
npm ci
npm.cmd start
```

Open `http://localhost:4200`. The frontend reads and writes real backend data; inventory
balances and dashboard totals are calculated by the backend.

## Verify persisted data

Use the application forms to create or update items, warehouses, receipts, issues, and
transfers. Refresh the browser and confirm the records remain. Master-record deactivation
is soft deletion; receipt, issue, and transfer operations follow backend stock rules.

In MySQL Workbench, select `warehouse_db` and inspect the persisted entity tables:

```sql
USE warehouse_db;
SELECT * FROM items ORDER BY id DESC;
SELECT * FROM warehouses ORDER BY id DESC;
SELECT * FROM stock_receipts ORDER BY id DESC;
SELECT * FROM stock_issues ORDER BY id DESC;
SELECT * FROM stock_transfers ORDER BY id DESC;
```

Available balances are calculated rather than stored as a separate movement total. Verify
them through `GET http://localhost:8080/api/inventory/balances` (optionally with
`?warehouseId=<id>`). The dashboard summary is available at
`GET http://localhost:8080/api/dashboard/summary`.

## Build and tests

From `frontend/`:

```powershell
npm.cmd test -- --watch=false
npm.cmd run build
```

## Deploy the frontend to Vercel

Import the repository into Vercel and set the project Root Directory to `frontend`.
The Vercel configuration writes the static build from `dist/frontend/browser` and
rewrites `/` and client-side routes to Angular's `index.csr.html`. Static assets and
`/api/...` paths are not routed to the Angular fallback.

The production API URL in `src/environments/environment.ts` is intentionally empty
because no public backend URL has been configured. Set it to the deployed Spring Boot
API base URL before deploying if the frontend should load live data. The development
environment continues to use `http://localhost:8080/api`. The backend must also allow
the deployed Vercel origin through CORS; deploy the Spring Boot API and its database
separately from this static frontend.
