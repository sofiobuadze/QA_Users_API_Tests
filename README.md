# Users API Tests

`GET /users` სერვისის (ფილტრაცია `?age=`, `?gender=`) ავტომატური ტესტები.
სერვისს ამოკებს **WireMock** (Docker), სტაბები ტესტების გაშვებამდე დინამიურად იტვირთება.

**Stack:** Java 11+, Maven, TestNG, RestAssured, WireMock 2.35.0 (Docker), Lombok, Jackson, SQLite (JDBC).

## გაშვება

Windows (PowerShell) და Linux/macOS — საჭიროა Docker, JDK 11+, Maven.

1. WireMock-ის გაშვება:

   ```bash
   ./start-wiremock.sh
   ```

   ან პირდაპირ (PowerShell-ში `${PWD}` იგივე მუშაობს):

   ```bash
   docker run --rm -d --name wiremock -p 8080:8080 -v ${PWD}/mocks:/home/wiremock wiremock/wiremock:2.35.0
   ```

2. ტესტები:

   ```bash
   mvn clean test
   ```

   სხვა მისამართი/ბაზა: `mvn test -Dwiremock.url=http://localhost:8080 -Ddb.path=test-results.db`

3. შეჩერება: `./stop-wiremock.sh` (ან `docker stop wiremock`).

## სტრუქტურა

| ფაილი | დანიშნულება |
|---|---|
| `model/User.java` | Lombok მოდელი (`@Data`, `@Builder`) |
| `mock/WireMockStubs.java` | სტაბების დინამიური რეგისტრაცია `/__admin/mappings`-ით (`@BeforeSuite`-იდან) |
| `client/UsersClient.java` | RestAssured კლიენტი |
| `tests/UsersApiTest.java` | 6 ტესტი, თითოეული `@DataProvider`-ით |
| `listener/ResultListener.java` | შედეგების ჩაწერა SQLite-ში |
| `db/ResultRepository.java` | JDBC: თუ `test_name` არსებობს — `UPDATE`, სხვა შემთხვევაში `INSERT` |

## სტაბები

| მოთხოვნა | პასუხი |
|---|---|
| `GET /users` | 200, [Alice, Bob] |
| `GET /users?age=30` / `?age=25` | 200, [Alice] / [Bob] |
| `GET /users?gender=female` / `?gender=male` | 200, [Alice] / [Bob] |
| `GET /users?age=-1` | 400 |
| `GET /users?gender=unknown` | 200, `[]` (ტესტი იღებს 200+ცარიელ სიას ან 422-ს) |
| `GET /users` + header `X-Simulate-Error: true` | 500 |

> 500-ის სტაბი header-ით არჩევა აუცილებელია: იგივე `GET /users` უკვე აბრუნებს 200-ს (პოზიტიური სტაბი),
> ორი სტაბი ერთსა და იმავე მოთხოვნაზე ვერ იარსებებს.

## SQLite

ფაილი `test-results.db` (ჩნდება პირველ გაშვებაზე), ცხრილი `test_results(id, test_name, status, execution_time)`.
DataProvider-იანი ტესტი ბევრჯერ სრულდება, მაგრამ ბაზაში ერთი ჩანაწერია ტესტ-მეთოდზე:
`FAILED` თუ ერთი გაშვებაც ჩავარდა, სხვა შემთხვევაში `PASSED`.

```bash
sqlite3 test-results.db "SELECT * FROM test_results;"
```
