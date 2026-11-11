# CSE202 Banking System

Java 17 + JavaFX + H2 (JDBC) banking system for the CSE202 OOAD assignment.

Customers register, log in, open Savings / Investment / Cheque accounts, deposit, withdraw and view statements.
An administrator views all accounts and pays monthly interest (Investment 5%, Savings 0.05%, Cheque none).

## Run it in GitHub Codespaces

1. Create a new GitHub repository and upload (or push) this whole folder, including `.devcontainer/`.
2. On GitHub: **Code -> Codespaces -> Create codespace on main**. The first start installs Java, Maven and a desktop.
3. In the Codespace terminal:
   ```bash
   mvn test          # runs the 34 unit + integration tests
   mvn javafx:run    # starts the GUI
   ```
4. Open the **Ports** tab, open port **6080** in the browser and click **Connect** (password `vscode`).
   The JavaFX window appears on that desktop.

Locally (Windows/macOS/Linux with JDK 17+ and Maven): the same two commands.

## Sample logins (10 customers are seeded on first run)

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `Admin@123` |
| Customer with all three account types | `nseretse` | `Password@123` |
| Customer, not employed (savings only) | `lkgosi` | `Password@123` |
| Other customers | `kmolefe`, `tmokoena`, `bdikgang`, `mtau`, `ophiri`, `tramotswe`, `nmoremi`, `kmasire` | `Password@123` |

The database file is `data/bankdb.mv.db`. Delete the `data/` folder to start again with fresh sample data.

## Project structure

```
src/main/java/com/bac/banking/
  App.java, Launcher.java        start-up and wiring of all layers
  model/        entity classes   User, Customer, Administrator, Employer, Account (abstract),
                                 SavingsAccount, InvestmentAccount, ChequeAccount,
                                 Depositable, Withdrawable, InterestBearing (interfaces),
                                 Transaction, InterestPayment, Bank, enums
  controller/   control classes  LoginController, CustomerController, AccountController,
                                 AdminController, Session, InputValidator, dto/ records
  view/         boundary classes LoginView, RegisterView, CustomerDashboardView, OpenAccountDialog,
                                 TransactionDialog, EmploymentDialog, StatementView, AdminDashboardView
  dao/          JDBC layer       DAO<T,K>, DatabaseManager, CustomerDAO, AccountDAO,
                                 TransactionDAO, AdministratorDAO, DataSeeder
  exception/                     BankingException and subclasses
  util/                          PasswordUtil (salted SHA-256), MoneyFormat
src/main/resources/schema.sql    database schema
src/test/java/...                model, DAO and integration tests
docs/diagrams/*.puml             PlantUML sources of every UML diagram (+ PNGs)
docs/build_report.js             regenerates the Word report
```

## Suggested commits (one per assignment week)

1. Core model: `model/`, `exception/`, `util/`, `pom.xml`, model tests
2. GUI: `view/`, `styles.css`
3. Controllers: `controller/`
4. Database: `schema.sql`, `dao/`, DAO tests
5. Integration: `App.java`, integration tests
