# Dispensary

Dispensary is a Spring Boot pharmacy retail app for a single store. Staff can receive medicine batches, see which stock needs attention, ring up a sale and reprint the customer's receipt. The browser interface and REST API run in the same application.

## Run locally

Install a JDK 17 or newer. From the repository root, run:

```sh
cd medicine
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. Open [the local app](http://localhost:8080) and sign in as `pharmacist`. On first use, Spring prints a generated password in the terminal. Set `SPRING_SECURITY_USER_PASSWORD` to keep a password across restarts. Set `STAFF_USER` to change the username.

The app starts with an empty inventory and saves data in `medicine/data/` when launched from the module directory. Stop the app before copying this directory for an H2 backup. The default bind address is loopback.

## Retail workflows

- **Inventory** — Add one record per medicine batch, including expiry, unit price, manufacturer, supplier, target stock and prescription requirement. Search the catalog, edit batch details, or use **Adjust** for deliveries and corrections. Stock adjustments cannot take the balance below zero.
- **Reordering** — Use **Inventory → Reorder list** for a printable supplier-sorted list. Batches below 10 units qualify. The order quantity brings each batch to its own target, which defaults to 30.
- **Point of sale** — Choose from sellable batches ordered by earliest expiry. Add multiple items, record the payment method, and enter a prescription reference when required. Completing a sale records the receipt and deducts stock in one transaction. Payment methods are recorded for bookkeeping.
- **Customers** — Enter a phone number during checkout to link the sale to a customer. Search by name or phone and reopen receipts from purchase history. Walk-in sales can omit both fields.
- **Overview and sales reports** — Review daily revenue, a seven-day sales chart, low-stock batches, batches expiring within 90 days, and recent receipts. Select a reporting period to see sales history, top-selling batches and a downloadable PDF. Revenue uses prices recorded at sale time.

Prices display in USD. Checkout records a prescription reference supplied by staff. The application uses a single staff account and does not calculate taxes or process card payments.

## Database and optional email

For MySQL, create a database and supply `DB_URL` (for example `jdbc:mysql://localhost:3306/pharmacy`), `DB_USER`, and `DB_PASSWORD`. The application uses Hibernate schema updates. Back up an existing installation and test the update on a copy, particularly the conversion of monetary fields to decimal. Older receipts without a saved medicine name or batch number display the linked inventory details.

To enable daily stock and expiry emails and a weekly sales PDF, set `ALERTS_ENABLED=true`, `ALERT_RECIPIENT`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER` and `SMTP_PASSWORD`. Scheduled jobs use the server's time zone. Configure the server and tills for the store's time zone. For a network installation set `APP_BIND_ADDRESS` and serve the application over HTTPS.

## Build and test

```sh
cd medicine
./mvnw clean package
java -jar target/medicine-0.0.1-SNAPSHOT.jar
```

Integration tests use an isolated in-memory H2 database. They exercise staff authentication, checkout validation, transaction rollback, receipt retries, customer history and competing sales against the same stock. The build packages all browser assets in the executable JAR.

## API access

The existing medicine, sale and report routes are available under `/api` after staff authentication. For a write operation, fetch `GET /api/session` first and send its CSRF token in the named header using the same session cookie. The browser interface handles this automatically. API clients can include a unique `checkoutKey` per sale and reuse that key when retrying the same checkout.
