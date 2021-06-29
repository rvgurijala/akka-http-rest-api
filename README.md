# Akka Http Service

Routes
1. `POST /register` - Register a new user
2. `POST /login` - Login user
3. `POST /logout` - Logout user
4. `GET /books` - Get all books
5. `GET /books/:id` - Get book
6. `POST /books` - Create book (admin permission required)
7. `PUT /books/:id` - Update book (admin permission required)
8. `DELETE /books/:id` - Delete book (admin permission required)
9. `GET /orders` - Get orders (admin gets all orders, user gets just personal orders)
10. `POST /orders` - Create order (No need negative quantity check)
