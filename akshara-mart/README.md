# Akshara Mart - Home Appliances Store
Frontend: HTML + CSS + JavaScript | Backend: Spring Boot 3 (Java 17) | Database: MySQL

## Run panna steps
1. Install: JDK 17+, Maven, MySQL (MySQL server run aaganum).
2. `src/main/resources/application.properties` open pannunga.
   `spring.datasource.password=YOUR_MYSQL_PASSWORD` -> unga MySQL password podunga (username root illana athaiyum maathunga).
3. Database create panna thevai illa - `akshara_mart` automatic ah create aagum, tables-um, 50 products-um.
4. VS Code la: "Extension Pack for Java" + "Spring Boot Extension Pack" install pannunga.
   Terminal la project folder la:  `mvn spring-boot:run`
5. Browser la open: http://localhost:8080

## Demo logins
| Role   | Email                | Password  |
|--------|----------------------|-----------|
| Buyer  | buyer@akshara.com    | buyer123  |
| Seller | seller@akshara.com   | seller123 |
| Admin  | admin@akshara.com    | admin123  |

## Features
- Buyer: browse, search (no match na "No results found"), add to cart, checkout, "Order placed successfully!", My Orders history
- Seller: add / edit / delete products (photo upload or image URL), see orders, change order status
- Admin: view/delete users and products, see all orders

## Product images
Seed products use keyword-based photos from loremflickr.com (internet venum). Internet illa na placeholder image kaattum.
Unga own photos venum na Seller login -> product add pannum bodhu "Upload product photo" use pannunga.
