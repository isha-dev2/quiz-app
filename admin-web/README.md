# CS Quiz Web Admin Control Center

A modern, responsive web administration portal for the CS Quiz Android Application. Fully integrated in real-time with Firebase Firestore & Authentication (`quiz-ec6eb`).

---

## Features

- **Dashboard Analytics**: Live statistics for total questions, active questions, subject breakdown, and registered students.
- **Full Question Management (CRUD)**:
  - Add new questions with subject (`DSA`, `DBMS`, `OS`, `Networks`), difficulty, 4 choices, and correct answer selection.
  - Search and filter questions by text, subject, difficulty, or active status in real time.
  - In-place Edit and Delete with confirmation modal.
  - Quick 1-click active/inactive toggle directly from the table.
- **Bulk JSON Import / Export**:
  - Drag-and-drop or upload `questions_clean.json` to batch import into Firestore (with duplicate detection).
  - Export all questions to a downloadable JSON backup.
- **Student Leaderboard**: View student rankings, emails, and high scores.
- **Dark & Light Mode**: Modern glassmorphism UI with theme toggle saved to local storage.
- **Admin Authentication**: Role-based access verifying `role: "admin"` in Firestore, plus a 1-click **Demo / Evaluator Mode** for presentations.

---

## How to Run Locally

You can launch the web admin portal in any browser using a simple local server:

### Option 1: Python (Built into Mac)
Open your terminal and run:
```bash
cd admin-web
python3 -m http.server 3000
```
Then open your browser at:
👉 **[http://localhost:3000](http://localhost:3000)**

### Option 2: Node / npx
```bash
cd admin-web
npx serve .
```

### Option 3: Direct File Opening
You can also directly double-click `index.html` in Finder to open it in Chrome or Safari.
