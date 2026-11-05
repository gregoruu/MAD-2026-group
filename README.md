# **App name: Movara**

# **Team members: Marten Tiisler, Kristo Kontse, Gregor Uustalu, Sander Zirel, Anna Liisa Nurm**

# **App idea**
Movie recommendation app that allows the user to search for movies based on different criteria, view information about movies, like movies that they have enjoyed, save movies that they want to watch in the future and receive recommendations for new movies that they might enjoy based on their liked movies.

# **Problem statement**
Users often spend an excessive amount of time scrolling through multiple streaming platforms without finding a movie that matches their mood and preferences or availability, leading to frustration.

# **Product purpose**
Serve as a fast and centralized recommendation platform that remembers and analyzes viewing history to pick movies that users genuinely enjoy without excessive cross-platform searching.

# **Business goals**
The primary goal of this project is to design and deploy a fully functional Minimum Viable Product (MVP) that integrates movie search, user preferences, and personal watchlists into a single platform. By delivering personalized recommendations based on user likings and watch history, the application aims to improve the overall user experience by minimizing decision fatigue and reducing the time spent on choosing movies. Additionally, the project focuses on implementing a smart recommendation algorithm to ensure accurate and relevant movie suggestions. 

## Repository structure

| Path | Purpose |
| --- | --- |
| `frontend/app/` | Android application, including production code, tests and Room schemas |
| `docs/` | Architecture, API documentation, design specifications and technical decisions |
| `gradle/` | Gradle Wrapper and dependency version catalog |
| `.run/` | Shared Android Studio run configuration |
| Root Gradle files and `gradlew` / `gradlew.bat` | Configuration and build entry points for the single Gradle project |

Movara has no server component of its own. The Android app is designed to access
TMDB directly for movie data and store the user's movie lists locally with Room.
There is no `backend/` directory because there is no server code to maintain.
See the [architecture documentation](docs/architecture.md#backend-decision) for
the rationale and the [Android setup guide](frontend/README.md) to run the project.
Open the repository root in Android Studio; `frontend/` is not a separate Gradle project.

## **Contribution rules**
View rules here: [CONTRIBUTION_RULES.md](./CONTRIBUTION_RULES.md)
