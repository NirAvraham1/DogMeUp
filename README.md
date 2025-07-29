🐾 DogMeUp
DogMeUp is a mobile app that connects dog owners with trusted dog sitters. Whether you're going on vacation or just need help for a few hours, DogMeUp helps you find a sitter nearby — quickly, easily, and securely.

📱 Features
🔐 User registration and login (Email, Google)
🧑‍💼 Role-based interface: dog owners (clients) and dog sitters (service providers)
📅 Sitters can publish their availability (date + time)
📍 Location-aware sitter listings
🐶 Clients can search for sitters by availability and location
✍️ Sitters can write bios and upload profile photos
🌟 Post-service review and rating system
⚙️ Profile and settings management
🔓 Firebase Authentication & Firestore Database integration

🛠 Tech Stack
Kotlin — Native Android development
Firebase Authentication — User login & identity
Firebase Firestore — Realtime database for users, availability, and reviews
Firebase Storage — Store profile photos
Android Studio — Development environment

🚀 Getting Started
Prerequisites
Android Studio (latest version recommended)
Android device or emulator
A Firebase project with:
  Authentication (Email/Password + Google)
  Firestore Database
  Firebase Storage

The project includes a valid google-services.json file, but does not include any API keys. Make sure to add your own Firebase project and enable required services.

🧭 Project Structure
DogMeUp/
├── activities/               # Core screens (Login, Register, Home, FindSitter, etc.)
├── auth/                    # Authentication logic
├── models/                  # Data models (User, Availability, Review)
├── firebase/                # Firestore / Storage helper classes
├── utils/                   # Shared utilities (validation, constants)
├── res/                     # Layouts, drawables, values
├── AndroidManifest.xml
├── google-services.json     # Firebase config (without API key)

👥 User Roles
Dog Owner (Client)
Sign up and log in
Browse available sitters
Filter by date
View sitter profiles
Submit ratings and reviews

Dog Sitter
Create a profile with bio and photo
Add and manage availability slots
Receive reviews

📌 Notes
The app defaults to role-based navigation. If a user checks "I am a sitter" during sign-up, they are redirected to sitter setup screens.
The app assumes network access and location permissions.
Location-based features may fall back to a default location if permission is denied.

👨‍💻 Contributing
This is a course project built by [Your Name]. Contributions or feedback are welcome if you're reviewing or collaborating.

📄 License
This project is for educational purposes. All rights reserved © 2025 by DogMeUp team.

