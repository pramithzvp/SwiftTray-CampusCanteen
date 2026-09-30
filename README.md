# SwiftTray — Campus Canteen Ordering and Queue Management

EE4207 Web Application Development project. React + Vite frontend, Java Spring Boot REST API, and MySQL database.

## Implemented first version

- Student registration and login with hashed passwords and server-side sessions.
- Student, staff, and administrator permissions enforced by the API.
- Menu browsing, category filters, search, and availability.
- Tray quantities, server-calculated order totals, and unique collection tokens.
- Student order history and status tracking, refreshed every five seconds.
- Cancellation while an order is queued.
- Staff queue: Queued → Preparing → Ready → Collected.
- Administrator menu creation, editing, availability, and removal.
- Administrator creation of staff accounts.
- Responsive layouts, error states, keyboard dialog controls, and reduced-motion support.
- CSRF protection, session rotation at login, and transactional order creation.

Payment is made at collection. There is one canteen. Menu data starts empty; there are no shared demo accounts or automatically generated food records.

## Start on your Mac in VS Code

### 1. Open the folder

Extract `SwiftTray-Project.zip` and open the `swifttray` folder in VS Code. The root contains `package.json`, `frontend/`, and `backend/`.

Install Node.js supported by Vite (20.19+ or 22.12+), a JDK (Java 17 or newer; Java 21 is a good choice), and MySQL. Maven is supplied through `backend/mvnw`.

Check in the VS Code terminal:

```bash
node --version
npm --version
java -version
mysql --version
```

### 2. Create the database

Open MySQL Workbench or sign in using your own MySQL administrator account:

```bash
mysql -u root -p
```

Execute:

```sql
CREATE DATABASE IF NOT EXISTS swifttray CHARACTER SET utf8mb4;
CREATE USER 'swifttray'@'localhost' IDENTIFIED BY 'REPLACE_WITH_YOUR_OWN_DB_PASSWORD';
GRANT ALL PRIVILEGES ON swifttray.* TO 'swifttray'@'localhost';
```

Replace the example password with a unique password before executing the SQL. If the user already exists, use that account's credentials or explicitly change its password. The application creates its tables at startup.

### 3. Configure credentials

From the project root:

```bash
cp .env.example .env
```

Edit `.env` in VS Code:

- Set `DB_PASSWORD` to the password used for the MySQL `swifttray` account.
- Set `SWIFTTRAY_ADMIN_EMAIL` to your administrator email.
- Set `SWIFTTRAY_ADMIN_PASSWORD` to a unique password of at least 12 characters.
- Keep `DB_URL=jdbc:mysql://localhost:3306/swifttray` for local MySQL.
- Use shell quoting around values containing spaces or shell-special characters, for example `DB_PASSWORD='your-own-password'`.

Do not commit or share `.env`. Administrator credentials are used only to create the account when its email does not exist. After the first successful start, remove `SWIFTTRAY_ADMIN_PASSWORD` and `SWIFTTRAY_ADMIN_EMAIL` from `.env`; the saved administrator account continues to work. This bootstrap never promotes an existing student account.

### 4. Start the backend

In the first VS Code terminal, from the project root:

```bash
chmod +x backend/mvnw scripts/*.sh
./scripts/start-backend.sh
```

Wait for Spring Boot's `Started SwiftTrayApplication` message. Verify:

```bash
curl http://localhost:8080/api/health
```

Expected response: `{"status":"UP","application":"SwiftTray"}` (key order may differ).

### 5. Start the frontend

Open a second VS Code terminal in the project root:

```bash
npm ci
./scripts/start-frontend.sh
```

Open **http://localhost:4173**. Keep both terminals running. Vite forwards `/api` requests to Spring Boot on port 8080.

### 6. Run your first complete flow

1. Sign in using the administrator credentials you chose.
2. Open **Manage menu**, add an actual canteen item, set its price in LKR, and mark it available.
3. Open **Staff accounts** and create a staff account with a unique password.
4. Sign out. Create a student account using **Sign in → Create student account**.
5. Add the menu item to your tray and place an order. Note the collection token.
6. Use another browser/private window to sign in as staff. Start preparing the order, mark it ready, and confirm collection.
7. The student's **My orders** page updates automatically.

Each browser session holds one signed-in account; use separate browsers or a private window when showing student and staff simultaneously.

## Optional local development without MySQL

If MySQL is not installed yet, set your administrator bootstrap details in `.env`, then run:

```bash
./scripts/start-backend.sh local
```

This explicit profile uses a real, persistent H2 database in `backend/data/`, rather than MySQL. It does not simulate API responses. The normal command always uses MySQL. H2 and MySQL have separate data; switching does not migrate records.

## API routes

| Method | Endpoint | Permission |
|---|---|---|
| GET | `/api/health` | Public |
| GET | `/api/auth/csrf` | Public; returns token and header name |
| POST | `/api/auth/register` | Public; always creates a student |
| POST | `/api/auth/login` | Public |
| GET | `/api/auth/me` | Signed in |
| POST | `/api/auth/logout` | Signed in |
| GET | `/api/menu-items` | Public |
| POST | `/api/menu-items` | Administrator |
| PUT / DELETE | `/api/menu-items/{id}` | Administrator |
| POST | `/api/orders` | Student |
| GET | `/api/orders/mine` | Signed in; own orders only |
| POST | `/api/orders/{id}/cancel` | Student; own queued order only |
| GET | `/api/staff/orders` | Staff or administrator |
| PATCH | `/api/staff/orders/{id}/status` | Staff or administrator |
| POST | `/api/admin/staff` | Administrator |

For Thunder Client/Postman, first call `/api/auth/csrf`. Keep response cookies and send its returned token in the returned header name (`X-XSRF-TOKEN`) on every POST, PUT, PATCH, and DELETE request. Sign in with `/api/auth/login` and retain the session cookie. Passwords and roles are never taken from browser claims; public registration always creates a student.

Example administrator menu request:

```json
{"name":"Rice and curry","description":"Rice with vegetables and dhal","category":"Meals","price":350.00,"available":true}
```

Example student order request:

```json
{"items":[{"menuItemId":1,"quantity":2}]}
```

The backend ignores any browser-supplied total and uses database prices. Menu names and unit prices are saved on order lines so existing orders survive price changes and menu removal.

## Verification and build

```bash
npm run build
cd backend
./mvnw test
./mvnw -DskipTests package
```

Build verification passed: frontend production build, backend compilation, and both HTTP integration tests (0 failures, 0 errors).

The integration suite uses a temporary H2 database and an actual HTTP server. It checks student/admin/staff access, CSRF, registration, session login/logout, order totals, price snapshots, availability, cancellation, status transitions, order privacy, and concurrent token generation. It creates test-only accounts and data, never records in your normal database.

A live MySQL instance and interactive browser QA were not available in the build environment. Verify the MySQL connection and the browser demonstration locally before submission. Optional WebMCP tools are feature-detected in supporting browsers; supported-browser validation was unavailable.

## Hosting

The private hosted Site contains the **frontend only**. The Spring Boot server and MySQL database are separate and are not hosted by that link. Login, the menu, and ordering require the backend; the frontend reports service unavailability rather than showing simulated data.

For a full deployment, host Spring Boot with MySQL and serve the frontend and `/api` under the same HTTPS origin using a reverse proxy. Set `COOKIE_SECURE=true` in that environment. The repository's `dist/` is generated by `npm run build`; the Java archive is generated in `backend/target/`. Do not serve the development server as a production server. Add production database migrations, login rate limiting, backups, and a canteen operating policy before public use.

## Troubleshooting

- **Java not found:** install a JDK and make sure the VS Code terminal uses it. On macOS, `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` selects an installed Java 21 JDK.
- **Port 8080 already in use:** stop the other backend intentionally, or change the `PORT` environment value and update the proxy target in `vite.config.js` to match.
- **Access denied for MySQL:** check `DB_USERNAME`, `DB_PASSWORD`, and the MySQL user's permissions.
- **Cannot load menu:** confirm the backend is running and `/api/health` responds.
- **No administrator login:** check first-run bootstrap credentials. If the email already belongs to a student, use a different bootstrap email.
- **First Maven run takes time:** it downloads dependencies. Later runs use Maven's cache.

## Source layout

```text
swifttray/
  frontend/src/          React screens, API client, and styles
  frontend/public/       Favicon and licensed meal photo
  backend/               Spring Boot API, persistence, security, tests, Maven wrapper
  database/schema.sql    Table reference for your ER diagram
  scripts/               Mac/Linux startup helpers
  .env.example           Credential template
  README.md              Setup, API routes, and demonstration guide
```

Meal photograph: Nilantha Sanjeewa, https://unsplash.com/photos/cooked-rice-on-brown-wooden-bowl-EXHIHvJu4VU. Used under the Unsplash License: https://unsplash.com/license.
