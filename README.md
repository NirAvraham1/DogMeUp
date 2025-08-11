
# 🐾 DogMeUp

**DogMeUp** is a mobile application that connects dog owners with available dog sitters in their area.  
The app provides a smooth experience for both clients and sitters — from registration and profile setup to booking services, managing availability, and leaving reviews.

---

## 📱 Features

### Client Side (Dog Owner)
- **Sign Up & Login** – Email, Google, or Phone authentication.
- **Client Home Screen** – Personalized welcome message with navigation buttons.
- **Search Sitters** – Filter by date, city, and maximum price.
- **View Sitter Availability** – Display sitter details (name, location, hours).
- **Request a Service** – Send booking requests directly to sitters.
- **Manage Active Orders** – Track ongoing bookings in *My Orders*.
- **Service History** – View past completed bookings.
- **Leave Review & Rating** – Submit feedback after completed services.
- **Edit Profile** – Update personal details (name, email, role).
- **Logout** – Secure sign-out.

---

### Sitter Side (Service Provider)
- **Sign Up & Login** – Option to register as a sitter during sign-up.
- **Sitter Profile Setup** – First-time flow to add bio and upload profile picture.
- **Sitter Home Screen** – Displays existing availability and navigation.
- **Manage Availability** – Add, edit, or delete available slots (date & time).
- **View Incoming Bookings** – See requests from clients.
- **Service History** – List of completed bookings.
- **Receive Reviews & Ratings** – Feedback from clients.
- **Edit Profile** – Update personal details, bio, and role.
- **Logout** – Secure sign-out.

---

## 🛠️ Tech Stack

- **Android Studio** (Kotlin)
- **Firebase Authentication** (Email, Google, Phone)
- **Firebase Firestore** – Data storage for users, availability, bookings, and reviews
- **Firebase Storage** – Profile photo uploads
- **Glide** – Image loading
- **Material Design Components** – UI styling

---

## 📂 Project Structure (Main Modules)
DogMeUp/
├── activities/ # All Activity screens for Client & Sitter flows
├── adapters/ # RecyclerView adapters for lists
├── models/ # Data models for Users, Availability, Bookings, Reviews
├── utils/ # Helper classes and shared logic
└── res/ # Layout XML files, drawables, and string resources


## 👤 Author

Developed by **Nir Avraham**

## 📜 License

This project is provided for academic and personal portfolio purposes.  
No commercial use without permission.
