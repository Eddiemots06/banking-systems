// Builds the CSE202 Part A (and Part B testing) report as a Word document.
// Run: node docs/build_report.js   (from the project root)
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, ShadingType, ImageRun, PageBreak, TableOfContents, LevelFormat, PageOrientation,
  Header, Footer, PageNumber, BorderStyle, SectionType,
} = require("docx");

const DIAG = path.join(__dirname, "diagrams");
const OUT = process.argv[2] || path.join(__dirname, "CSE202_Banking_System_Report.docx");

const FONT = "Calibri";
const TEAL = "0F6E6E";
const PORTRAIT_W = 9638; // A4 minus 2 cm margins, in DXA
const LANDSCAPE_W = 14570;

// ---------- helpers ----------
const p = (text, opts = {}) =>
  new Paragraph({
    spacing: { after: 120, line: 276 },
    alignment: opts.align,
    children: runs(text, opts),
  });

function runs(text, opts = {}) {
  // **bold** support inside a string
  const parts = String(text).split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
  return parts.map((part) =>
    part.startsWith("**")
      ? new TextRun({ text: part.slice(2, -2), bold: true, italics: opts.italics, size: opts.size })
      : new TextRun({ text: part, bold: opts.bold, italics: opts.italics, size: opts.size, color: opts.color })
  );
}

const h1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)] });
const h2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const h3 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(t)] });
const bullet = (t) => new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 }, children: runs(t) });
const pageBreak = () => new Paragraph({ children: [new PageBreak()] });
const caption = (t) =>
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 60, after: 240 },
    children: [new TextRun({ text: t, italics: true, size: 20, color: "555555" })] });

function image(file, maxW, maxH) {
  const buf = fs.readFileSync(path.join(DIAG, file));
  const w0 = buf.readUInt32BE(16), h0 = buf.readUInt32BE(20);
  let w = maxW, h = Math.round((h0 * maxW) / w0);
  if (h > maxH) { h = maxH; w = Math.round((w0 * maxH) / h0); }
  return new Paragraph({ alignment: AlignmentType.CENTER, children: [
    new ImageRun({ type: "png", data: buf, transformation: { width: w, height: h },
      altText: { title: file, description: file, name: file } }) ] });
}

const border = { style: BorderStyle.SINGLE, size: 4, color: "B7CACA" };
const borders = { top: border, bottom: border, left: border, right: border };

function table(headers, rows, widths, total = PORTRAIT_W) {
  const sum = widths.reduce((a, b) => a + b, 0);
  const cols = widths.map((w) => Math.round((w / sum) * total));
  cols[cols.length - 1] += total - cols.reduce((a, b) => a + b, 0);
  const cell = (text, i, head) =>
    new TableCell({
      borders, width: { size: cols[i], type: WidthType.DXA },
      shading: head ? { fill: "D7EEEE", type: ShadingType.CLEAR, color: "auto" } : undefined,
      margins: { top: 60, bottom: 60, left: 100, right: 100 },
      children: String(text).split("\n").map((line) =>
        new Paragraph({ spacing: { after: 40 }, children: runs(line, { bold: head, size: 19 }) })),
    });
  return new Table({
    width: { size: total, type: WidthType.DXA }, columnWidths: cols,
    rows: [
      new TableRow({ tableHeader: true, children: headers.map((h, i) => cell(h, i, true)) }),
      ...rows.map((r) => new TableRow({ children: r.map((c, i) => cell(c, i, false)) })),
    ],
  });
}
const gap = () => new Paragraph({ spacing: { after: 120 }, children: [] });

// ---------- content ----------
const title = [
  new Paragraph({ spacing: { before: 1800, after: 200 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "School of Computing and Information Systems", size: 26, color: "555555" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: "BSc Computer Systems Engineering", size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 },
    children: [new TextRun({ text: "CSE202 - Object Oriented Analysis & Design with Java", bold: true, size: 28 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: "Banking System", bold: true, size: 56, color: TEAL })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 1200 },
    children: [new TextRun({ text: "Requirements, UML Models and System Documentation", size: 30 })] }),
  table(["Item", "Details"], [
    ["Module code", "CSE202"],
    ["Title of paper", "Banking System - OOAD Assignment (Part A and Part B documentation)"],
    ["Student name", "[Your full name]"],
    ["Student registration number", "[Your student number]"],
    ["Programme / Year", "BSc Computer Systems Engineering, Year 2, Semester 1"],
    ["Lecturer / Tutor", "[Lecturer name]"],
    ["Submission date", "20 November 2026"],
  ], [35, 65]),
  pageBreak(),
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("Contents")] }),
  new TableOfContents("Contents", { hyperlink: true, headingStyleRange: "1-2" }),
  p("(If the contents list is empty, right-click it in Word and choose Update Field.)", { italics: true, size: 18, color: "777777" }),
  pageBreak(),
];

const intro = [
  h1("1. Introduction"),
  p("This report documents the analysis and design of a banking system for BAC Bank. The system lets customers register, log in, open one or more accounts (Savings, Investment and Cheque), deposit and withdraw funds within each account's rules, and view their transaction history. A bank administrator can view every account and pay monthly interest: 5% per month on Investment Accounts and 0.05% per month on Savings Accounts."),
  p("Part A covers requirements elicitation (Section 2), structural UML models (Section 3) and behavioural UML models (Section 4). Section 5 documents the implementation architecture and database for Part B, and Section 6 is the integration test report. The interview record is in Appendix A."),
  p("The design is implemented in Java 17 with JavaFX for the user interface and an H2 database accessed through JDBC. The code is organised into four layers: boundary (JavaFX views), control (controllers), entity (domain model) and data access (DAOs)."),
];

const reqs = [
  h1("2. Requirements Elicitation"),
  h2("2.1 Elicitation approach"),
  p("Requirements were gathered through a structured mock interview with the client (played by the module lecturer/tutor), using the assignment scenario as the starting brief. The questions covered the people who use the system, the account types and their rules, security expectations and how interest is paid. Answers were recorded (Appendix A), turned into numbered requirements, and each requirement was traced to a use case so it could be checked against the design and the tests."),
  h2("2.2 Functional requirements"),
  p("Functional requirements describe what the system must do. Priority uses MoSCoW (M = Must, S = Should, C = Could)."),
  table(["ID", "Requirement", "Priority", "Use case"], [
    ["FR-01", "The system shall let a new customer register with first name, surname, National ID (Omang), physical address, phone, email, username and password.", "M", "Register as Customer"],
    ["FR-02", "The system shall let a customer optionally record employment details (company name, company address, job title) at registration or later.", "M", "Register / Update Employment Details"],
    ["FR-03", "The system shall authenticate customers and administrators with a username and password before any banking function is available.", "M", "Log In"],
    ["FR-04", "The system shall block further login attempts after three consecutive failed attempts.", "S", "Lock Out After 3 Failed Attempts"],
    ["FR-05", "The system shall let a logged-in customer open a Savings, Investment or Cheque Account at a chosen branch, and allow one customer to hold several accounts, including all three types.", "M", "Open Account"],
    ["FR-06", "The system shall only open an Investment Account with an initial deposit of at least BWP 500.00.", "M", "Open Investment Account"],
    ["FR-07", "The system shall only open a Cheque Account for a customer whose employer details (company and company address) are on record.", "M", "Open Cheque Account"],
    ["FR-08", "The system shall let a customer deposit funds into any account they hold.", "M", "Deposit Funds"],
    ["FR-09", "The system shall allow withdrawals from Investment and Cheque Accounts, and refuse any withdrawal from a Savings Account.", "M", "Withdraw Funds"],
    ["FR-10", "The system shall refuse a withdrawal that is larger than the available balance and leave the balance unchanged.", "M", "Reject: Insufficient Funds"],
    ["FR-11", "The system shall record every deposit, withdrawal, opening deposit and interest payment as a transaction with date/time, amount, balance after and reference.", "M", "Record Transaction"],
    ["FR-12", "The system shall show a customer a list of their accounts with balances, and a statement (transaction history) for each account.", "M", "View My Accounts / View Transaction History"],
    ["FR-13", "The system shall let an administrator pay monthly interest: 5% per month on Investment Accounts and 0.05% per month on Savings Accounts. Cheque Accounts earn no interest.", "M", "Pay Monthly Interest"],
    ["FR-14", "The system shall let an administrator view all customers' accounts and balances.", "S", "View All Accounts"],
    ["FR-15", "The system shall only let a customer view or transact on accounts they own.", "M", "All customer use cases"],
  ], [9, 59, 9, 23]),
  gap(),
  h2("2.3 Non-functional requirements"),
  p("Non-functional requirements describe how well the system must work. Each has a measurable criterion and is linked to how the design meets it."),
  table(["ID", "Category", "Requirement and measure", "How the design meets it"], [
    ["NFR-01", "Security", "Passwords must never be stored in plain text.", "Salted SHA-256 hashing with 10,000 iterations (PasswordUtil); only the hash and salt are stored."],
    ["NFR-02", "Security", "Login errors must not reveal whether the username or the password was wrong; lock out after 3 failures.", "LoginController returns one generic message and counts failed attempts."],
    ["NFR-03", "Security", "Database access must be safe from SQL injection.", "All JDBC statements use PreparedStatement parameters."],
    ["NFR-04", "Integrity / reliability", "A balance change and its transaction record must be saved together or not at all.", "AccountDAO.applyTransaction uses one JDBC transaction with commit/rollback; CHECK constraints stop negative balances."],
    ["NFR-05", "Accuracy", "Money must be exact to the thebe (2 decimal places) with no floating-point error.", "BigDecimal with scale 2 and HALF_EVEN rounding; DECIMAL(15,2) columns."],
    ["NFR-06", "Performance", "Common operations (log in, deposit, statement) complete in under 2 seconds on a standard laptop/Codespace.", "Indexed foreign keys; small, direct queries; embedded H2 database."],
    ["NFR-07", "Usability", "A first-time user can open an account and make a deposit without training; errors are shown in plain language next to the form.", "Single window, clear buttons, account rules shown on screen, validation messages in dialogs."],
    ["NFR-08", "Maintainability", "No business logic in the GUI and no GUI or SQL code in the domain model.", "Layered design (view - controller - model - DAO); views only receive read-only DTOs."],
    ["NFR-09", "Portability", "The system must run in GitHub Codespaces and on Windows/macOS/Linux with Java 17+.", "Maven build, embedded H2, devcontainer configuration with a browser-based desktop."],
    ["NFR-10", "Testability", "Business rules and the full user flow must be covered by automated tests.", "34 JUnit tests including 7 end-to-end integration tests against an in-memory database."],
  ], [10, 15, 37, 38]),
];

const usecase = [
  pageBreak(),
  h1("3. Structural UML Modelling"),
  h2("3.1 System use case diagram"),
  image("usecase.png", 520, 760),
  caption("Figure 1 - Use case diagram for the Banking System"),
  h3("Actors"),
  table(["Actor", "Description"], [
    ["User (abstract)", "Anyone who logs in. Generalises Customer and Administrator, so both inherit Log In and Log Out."],
    ["Customer", "A person who banks with BAC Bank: registers, opens accounts, deposits, withdraws, views statements and keeps employment details up to date."],
    ["Administrator", "Bank staff who view all accounts and run the monthly interest payment."],
  ], [22, 78]),
  gap(),
  h3("Relationships used"),
  bullet("**Generalisation (actors):** Customer and Administrator are kinds of User."),
  bullet("**Generalisation (use cases):** Open Savings, Open Investment and Open Cheque Account are specialised forms of Open Account."),
  bullet("**<<include>>** (always happens): Deposit and Withdraw always include Validate Amount and Record Transaction; Open Investment Account always includes Make Opening Deposit (min BWP 500); Open Cheque Account always includes Verify Employment; Pay Monthly Interest always includes Calculate Interest and Record Transaction."),
  bullet("**<<extend>>** (happens only under a condition): Reject: Insufficient Funds extends Withdraw Funds when amount > balance; Update Employment Details extends Open Cheque Account when the customer is not employed; Lock Out extends Log In after 3 wrong passwords."),
  h3("Key use case descriptions"),
  table(["Use case", "Actor", "Pre-condition", "Main flow", "Alternative flows"], [
    ["Log In", "Customer, Administrator", "User is registered", "1. Enter username and password. 2. System checks the salted hash. 3. System opens the customer or administrator dashboard.", "Wrong details: generic error, attempts left shown. 3 failures: locked out."],
    ["Open Account", "Customer", "Logged in", "1. Choose type and branch. 2. Enter opening deposit. 3. System checks rules, creates the account and records any opening deposit.", "Investment under BWP 500: refused. Cheque but not employed: refused, prompt to update employment."],
    ["Deposit Funds", "Customer", "Logged in, owns account", "1. Select account. 2. Enter amount and reference. 3. System adds amount, records transaction, shows new balance.", "Amount not a positive number: error, nothing saved."],
    ["Withdraw Funds", "Customer", "Logged in, owns Investment or Cheque account", "1. Select account. 2. Enter amount. 3. System subtracts amount and records transaction.", "Savings account: button disabled / refused. Amount > balance: Insufficient Funds, balance unchanged."],
    ["Pay Monthly Interest", "Administrator", "Logged in as administrator", "1. Click Pay Monthly Interest and confirm. 2. For each Savings/Investment account: calculate, credit, record. 3. Show summary.", "Zero balance: skipped. Database error on one account: that payment fails, others continue."],
  ], [15, 13, 15, 32, 25]),
];

const classSection = [
  h2("3.2 Class diagram"),
  p("Figure 2 shows the domain model (package com.bac.banking.model). Private attributes (-) are only changed through public or protected methods (+ / #), which apply the business rules. Abstract classes and methods are shown in italics; static members are underlined."),
  image("class.png", 940, 580),
  caption("Figure 2 - Class diagram of the domain model"),
];

const classDesc = [
  h3("Description of each class"),
  table(["Class / interface", "Responsibility"], [
    ["User (abstract)", "Holds login credentials and the shared authenticate() and changePassword() behaviour. Declares abstract getRole() and getDisplayName()."],
    ["Customer", "A bank customer. Holds personal details, optional Employer, and the list of accounts. Provides getTotalBalance(), isEmployed() and findAccount()."],
    ["Administrator", "Bank staff user. Overrides getRole() and getDisplayName()."],
    ["Employer", "Company name, company address and job title. Its presence is what allows a Cheque Account to be opened."],
    ["Account (abstract)", "Common state (number, owner, branch, balance, date opened) and deposit behaviour for all accounts. An account must have a Customer owner, passed in the constructor (composition)."],
    ["SavingsAccount", "Deposits only, 0.05% interest per month. Does not implement Withdrawable, so a withdrawal cannot even be written in code."],
    ["InvestmentAccount", "Deposits, withdrawals, 5% interest per month; the opening constructor refuses deposits below BWP 500.00."],
    ["ChequeAccount", "Salary account. Deposits and withdrawals of any amount, no interest; the opening constructor refuses customers without an employer."],
    ["Depositable / Withdrawable / InterestBearing", "Interfaces describing capabilities. Each account type implements only the capabilities it has."],
    ["Transaction", "Immutable record of one movement of money: type, amount, balance after, time and reference."],
    ["InterestPayment", "Tracks one month's interest for one InterestBearing account through its states (see the state diagram)."],
    ["Bank", "Prepares the monthly interest run polymorphically over a list of Accounts."],
    ["AccountType, TransactionType", "Enumerations of the fixed account and transaction kinds."],
  ], [28, 72]),
  gap(),
  h3("How the design applies the OOP principles"),
  table(["Principle", "Where it is used"], [
    ["Abstraction", "Abstract classes User and Account cannot be instantiated; they define what every user/account has while hiding the details. Abstract methods getAccountType(), getAccountRules(), getRole(), getDisplayName()."],
    ["Inheritance", "SavingsAccount, InvestmentAccount and ChequeAccount extend Account; Customer and Administrator extend User; all custom exceptions extend BankingException."],
    ["Interfaces", "Depositable, Withdrawable and InterestBearing. InvestmentAccount implements two interfaces; SavingsAccount does not implement Withdrawable. Generic DAO<T, K> interface in the data layer."],
    ["Polymorphism", "Bank.prepareMonthlyInterest(List<Account>) treats every account as an Account and pays interest only to those that are InterestBearing, each using its own rate. AccountDAO builds the right subclass from one table."],
    ["Overriding", "getMonthlyInterestRate(), creditInterest(), getAccountType(), getAccountRules(), withdraw(), getRole(), getDisplayName(), equals(), hashCode() and toString()."],
    ["Overloading", "deposit(amount) and deposit(amount, reference); withdraw(amount) and withdraw(amount, reference); two constructors on every account class (open new vs. load from database) and on Transaction."],
    ["Encapsulation", "All fields private; balance can only change through deposit/withdraw/creditInterest, which validate amounts; getTransactions() and getAccounts() return read-only lists."],
    ["Composition / association", "Customer 1 *-- 0..* Account (an account cannot exist without its customer); Account 1 *-- 0..* Transaction; Customer 1 o-- 0..1 Employer."],
  ], [20, 80]),
];

const seqSection = [
  h1("4. Behavioural UML Modelling"),
  h2("4.1 Sequence diagram 1 - Log In"),
  image("seq_login.png", 900, 500),
  caption("Figure 3 - Sequence diagram for Log In"),
];
const seqSection2 = [
  h2("4.2 Sequence diagram 2 - Deposit Funds"),
  image("seq_deposit.png", 900, 560),
  caption("Figure 4 - Sequence diagram for Deposit Funds"),
];

const seqText = [
  h3("Explanation of the sequence diagrams"),
  p("**Log In (Figure 3).** The LoginView only collects the username and password and passes them to the LoginController. The controller checks that both fields are filled and that the user is not locked out, then asks the AdministratorDAO and CustomerDAO to load the user. The Customer object checks the password against its salted hash (authenticate()). On success the controller starts the Session and returns the role, and the view asks the ViewNavigator to show the right dashboard. On failure the attempt counter goes up and the same generic message is shown, so the screen never reveals which part was wrong."),
  p("**Deposit Funds (Figure 4).** The customer selects an account and the TransactionDialog sends the amount as text to the AccountController. The controller validates the amount, reloads the customer and accounts from the database (so the balance is current) and checks that the account belongs to the customer. The Account object itself applies the deposit and creates a Transaction. The AccountDAO then saves the new balance and the transaction inside one database transaction: if both statements succeed it commits, otherwise it rolls back and the user is told no money was moved."),
  h2("4.3 State diagram - InterestPayment"),
  p("The Pay Monthly Interest use case is modelled through the InterestPayment class, which has a real lifecycle in the code: each method only works from one state and throws an exception if called in the wrong state."),
  image("state_interest.png", 520, 520),
  caption("Figure 5 - State diagram of an InterestPayment"),
  table(["State", "Meaning", "Event / guard to leave"], [
    ["Pending", "Created for an InterestBearing account; nothing calculated yet.", "calculate() [amount > 0] to Calculated; calculate() [amount = 0] to Skipped; fail()"],
    ["Calculated", "Interest = balance x monthly rate (0.0005 or 0.05), rounded to 2 dp.", "credit() to Credited; fail()"],
    ["Credited", "Balance increased in memory and an INTEREST Transaction created.", "markRecorded() after the database commit to Recorded; fail() if the save is rolled back"],
    ["Recorded", "Balance and transaction are stored. Final state.", "-"],
    ["Skipped", "Nothing due (zero balance). Final state.", "-"],
    ["Failed", "Saving failed; reason stored, database unchanged. Final state.", "-"],
  ], [16, 46, 38]),
];

const partB = [
  pageBreak(),
  h1("5. Implementation Overview (Part B)"),
  h2("5.1 Layered architecture"),
  p("The application follows the Boundary-Control-Entity pattern with a data access layer. Views (JavaFX) only talk to controllers and only receive read-only DTO records (AccountSummary, TransactionRow), so they cannot change balances or contain business rules. Controllers validate input, enforce ownership and orchestrate the domain model and DAOs. The domain model has no JavaFX or JDBC code."),
  image("architecture.png", 640, 420),
  caption("Figure 6 - Layers and dependencies"),
  table(["Layer / package", "Classes"], [
    ["Boundary - com.bac.banking.view", "LoginView, RegisterView, CustomerDashboardView, OpenAccountDialog, TransactionDialog, EmploymentDialog, StatementView, AdminDashboardView, ViewNavigator"],
    ["Control - com.bac.banking.controller", "LoginController, CustomerController, AccountController, AdminController, Session, InputValidator, DTO records"],
    ["Entity - com.bac.banking.model", "User, Customer, Administrator, Employer, Account, SavingsAccount, InvestmentAccount, ChequeAccount, Depositable, Withdrawable, InterestBearing, Transaction, InterestPayment, Bank, enums"],
    ["Data access - com.bac.banking.dao", "DAO<T,K>, DatabaseManager, CustomerDAO, AccountDAO, TransactionDAO, AdministratorDAO, DataSeeder"],
  ], [30, 70]),
  gap(),
  h2("5.2 Database schema"),
  p("The schema (src/main/resources/schema.sql) maps the class diagram to five H2 tables. The Account hierarchy uses single-table inheritance: one accounts table with an account_type column, which AccountDAO turns back into the right subclass when reading."),
  image("erd.png", 480, 460),
  caption("Figure 7 - Entity relationship diagram"),
  table(["Class diagram", "Table", "Notes"], [
    ["Customer (+ User fields)", "customers", "username and national_id are UNIQUE; password stored as hash + salt"],
    ["Employer", "employers", "0..1 per customer (UNIQUE customer_id), deleted with the customer"],
    ["Account, SavingsAccount, InvestmentAccount, ChequeAccount", "accounts", "account_type discriminator; CHECK balance >= 0; FK to customers (an account cannot exist without a customer)"],
    ["Transaction", "transactions", "CHECK amount > 0; FK to accounts; never updated"],
    ["Administrator", "administrators", "Bank staff logins"],
  ], [32, 18, 50]),
  gap(),
  p("On first start the DataSeeder inserts 10 sample customers, 16 accounts with their transactions and one administrator, satisfying the requirement for 10 sample records. All CRUD operations are implemented in the DAO classes with PreparedStatements."),
];

const testReport = [
  h1("6. Integration Test Report"),
  h2("6.1 Approach"),
  p("Integration tests (BankingSystemIntegrationTest) wire the complete application exactly as App.java does, but against a private in-memory H2 database created for each test. Each test calls the controllers with the same raw text the JavaFX views send from their form fields, so it exercises controller, domain model, DAO and database together. Results are then checked by reading the database through brand-new DAO objects, which proves the data was really stored."),
  p("Tests are run with: mvn test. Alongside the integration tests there are 27 unit and DAO tests (AccountModelTest, InterestTest, CustomerTest, DaoCrudTest)."),
  h2("6.2 Test cases and results"),
  table(["ID", "Scenario (user flow)", "Expected result", "Actual", "Result"], [
    ["IT-01", "Register a new employed customer, log in, open Savings, Investment (BWP 1,500) and Cheque accounts, deposit \"1,000.00\" and 9,500, withdraw 250.50, view statement.", "3 accounts; database balances 1,000.00 / 1,249.50 / 9,500.00; statement shows Withdrawal then Opening deposit; total BWP 11,749.50.", "As expected", "Pass"],
    ["IT-02", "Unemployed customer with a savings account tries to withdraw, open a Cheque account, open Investment with 499, deposit \"abc\" and \"-20\"; then adds employment and opens Cheque.", "Every invalid action refused with a message; balance in database unchanged; Cheque opens once employed.", "As expected", "Pass"],
    ["IT-03", "Withdraw BWP 500.01 from an Investment account holding 500.00.", "InsufficientFundsException; database balance still 500.00.", "As expected", "Pass"],
    ["IT-04", "Wrong password, unknown user, empty fields, customer trying admin function, logged-out access, three failures then correct password.", "All refused; customer cannot pay interest; lock-out message after 3 failures.", "As expected", "Pass"],
    ["IT-05", "Customer A tries to deposit into / view the statement of Customer B's account.", "Refused: account does not belong to you.", "As expected", "Pass"],
    ["IT-06", "Administrator logs in and pays monthly interest over the 16 seeded accounts.", "10 payments (5 savings, 5 investment), 0 failures; 1,200.00 savings -> 1,200.60; 15,000.00 investment -> 15,750.00; cheque unchanged; INTEREST appears on statement.", "As expected", "Pass"],
    ["IT-07", "Registration with bad Omang, weak password, taken username, employed but no company.", "Each refused with a specific validation message.", "As expected", "Pass"],
  ], [8, 32, 34, 13, 9]),
  gap(),
  h2("6.3 Summary"),
  p("All 34 automated tests passed (27 unit/DAO tests and 7 integration tests). The tests confirm that the four layers work together: input from the views is validated by the controllers, business rules are enforced by the domain classes, and every change is stored atomically by the JDBC layer. The GUI screens themselves were also tested manually by logging in as a seeded customer and as the administrator and repeating flows IT-01 and IT-06 through the JavaFX windows."),
];

const appendix = [
  pageBreak(),
  h1("Appendix A - Interview Record"),
  p("Note to self before submitting: hold this interview with your lecturer/tutor, then replace the answers below with what they actually said and fill in the date. The answers currently reflect the assignment brief.", { italics: true, color: "B3261E" }),
  table(["Item", "Details"], [
    ["Interviewer", "[Your name]"],
    ["Interviewee (client)", "[Lecturer / tutor name], acting as BAC Bank representative"],
    ["Date / place / duration", "[Date], [Venue or online], [e.g. 30 minutes]"],
    ["Purpose", "Identify the core functions and quality attributes of the banking system"],
  ], [30, 70]),
  gap(),
  table(["No.", "Question", "Client's answer (summary)", "Requirement"], [
    ["1", "Who will use the system?", "Customers (individuals or companies) and bank staff who manage interest.", "FR-03, FR-14"],
    ["2", "What does a customer need to do first?", "Register their details and log in securely.", "FR-01, FR-03"],
    ["3", "What account types do you offer?", "Savings, Investment and Cheque. A customer can have one, several, or all three.", "FR-05"],
    ["4", "What are the rules for a Savings Account?", "Deposits only, no withdrawals; pays interest monthly based on the balance (0.05% per month).", "FR-09, FR-13"],
    ["5", "And the Investment Account?", "Higher interest (5% per month); deposits and withdrawals allowed; minimum opening deposit BWP 500.00.", "FR-06, FR-09, FR-13"],
    ["6", "And the Cheque Account?", "Used for salaries; deposits and withdrawals of any amount; only for working customers, who must give company name and address.", "FR-02, FR-07"],
    ["7", "Can an account exist without a customer?", "No, every account must belong to a customer.", "Class design"],
    ["8", "What should happen if a customer withdraws more than they have?", "Refuse it; no overdraft.", "FR-10"],
    ["9", "Do customers need to see past transactions?", "Yes, a statement per account.", "FR-11, FR-12"],
    ["10", "Who pays interest and how often?", "The bank, once a month, to the correct accounts.", "FR-13"],
    ["11", "What security do you expect?", "Passwords protected, customers only see their own accounts, block repeated wrong logins.", "NFR-01 to NFR-03, FR-04, FR-15"],
    ["12", "What about usability and performance?", "Simple screens, clear errors, fast responses.", "NFR-06, NFR-07"],
  ], [6, 30, 44, 20]),
  gap(),
  h1("References"),
  p("Fowler, M. (2004) UML Distilled: A Brief Guide to the Standard Object Modeling Language. 3rd edn. Boston: Addison-Wesley."),
  p("Larman, C. (2005) Applying UML and Patterns: An Introduction to Object-Oriented Analysis and Design and Iterative Development. 3rd edn. Upper Saddle River: Prentice Hall."),
  p("Sommerville, I. (2016) Software Engineering. 10th edn. Harlow: Pearson."),
  p("Bloch, J. (2018) Effective Java. 3rd edn. Boston: Addison-Wesley."),
];

// ---------- document ----------
const header = new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
  children: [new TextRun({ text: "CSE202 - Banking System", size: 16, color: "888888" })] })] });
const footer = new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
  children: [new TextRun({ children: ["Page ", PageNumber.CURRENT], size: 16, color: "888888" })] })] });
const margin = { top: 1134, bottom: 1134, left: 1134, right: 1134 };
const portrait = { page: { size: { width: 11906, height: 16838 }, margin } };
const landscape = { page: { size: { width: 11906, height: 16838, orientation: PageOrientation.LANDSCAPE }, margin } };
const sec = (props, children, first) => ({
  properties: { ...props, ...(first ? {} : { type: SectionType.NEXT_PAGE }) },
  headers: { default: header }, footers: { default: footer }, children,
});

const doc = new Document({
  creator: "CSE202 Student",
  features: { updateFields: true },
  title: "CSE202 Banking System Report",
  styles: {
    default: { document: { run: { font: FONT, size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, color: TEAL, font: FONT }, paragraph: { spacing: { before: 240, after: 160 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, color: "12403F", font: FONT }, paragraph: { spacing: { before: 200, after: 120 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 23, bold: true, color: "333333", font: FONT }, paragraph: { spacing: { before: 160, after: 80 }, outlineLevel: 2 } },
    ],
  },
  numbering: { config: [{ reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•",
    alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
  sections: [
    sec(portrait, [...title, ...intro, ...reqs, ...usecase], true),
    sec(landscape, classSection),
    sec(portrait, classDesc),
    sec(landscape, seqSection),
    sec(landscape, seqSection2),
    sec(portrait, [...seqText, ...partB, ...testReport, ...appendix]),
  ],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(OUT, buf);
  console.log("Wrote " + OUT);
});
