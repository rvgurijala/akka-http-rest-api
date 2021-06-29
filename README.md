# Akka Http Service

Hello!

We need to implement functionality of a bookshop. We will have three instances of a bookshop.
The main is in-house bookshop, which stores `Book.Internal`, should be connected to H2 database and two other which are just functions behaving as third party api calls and returning `Book.External`.
These functions can't be changed.

**Please make sure that the books coming from third party apis are in the response of `GET /books` call.**

Some tests and data types are pre-defined.

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

Hints
- Consider that third party apis as well as our database can have millions books.
- All CRUD operations should be performed only in H2 database, it means we can't run create/update/delete operations on mocked books in `StorageService`.
- Be sure that all pre-defined tests are passed
- Please don't change build.sbt

Bonus
- `WEBSOCKET /ws` - Everytime the order is placed a notification should be delivered to connected users. Admin receives all sent notifications, user receives only personal notifications.

Good luck!
